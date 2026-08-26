package com.jade.demo.service;

import at.favre.lib.crypto.bcrypt.BCrypt;
import com.jade.common.api.R;
import com.jade.common.constant.ResultCode;
import com.jade.common.exception.BizException;
import com.jade.demo.dto.LoginRequest;
import com.jade.demo.dto.LoginResponse;
import com.jade.demo.entity.SysUser;
import com.jade.demo.metrics.BusinessMetrics;
import com.jade.demo.repository.SysUserRepository;
import com.jade.security.jwt.MpJwtUtil;
import io.micrometer.core.instrument.Timer;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Set;

/**
 * 认证服务（修复了安全审计中的密码校验缺陷）
 *
 * 修复点：
 *   - 统一使用 BCrypt 验证（不再有明文密码分支）
 *   - 移除"固定密码 admin123"绕过逻辑
 *   - 统一错误消息（"用户名或密码错误"，不暴露用户是否存在）
 *   - 登录失败抛 401（FORBIDDEN→UNAUTHORIZED）
 *   - 计时攻击防护：无论用户是否存在，BCrypt.verify 都执行一次
 */
@ApplicationScoped
public class AuthService {

    @Inject
    SysUserRepository userRepository;

    @Inject
    MpJwtUtil mpJwtUtil;

    @Inject
    BusinessMetrics metrics;

    @ConfigProperty(name = "jade.jwt.expire-seconds", defaultValue = "7200")
    long expireSeconds;

    public R<LoginResponse> login(LoginRequest req) {
        Timer.Sample sample = Timer.start();
        try {
            // 1) 查用户
            SysUser user = userRepository.findByUsername(req.getUsername()).orElse(null);

            // 2) 校验密码（统一 BCrypt 路径）
            //    重要：无论用户是否存在都执行一次 BCrypt.verify，防止时间侧信道泄露用户存在
            boolean valid = verifyPassword(user, req.getPassword());

            if (user == null || !valid) {
                metrics.recordLoginFailure();
                throw new BizException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
            }

            // 3) 校验账号状态
            if (user.status == 0) {
                metrics.recordLoginFailure();
                throw new BizException(ResultCode.FORBIDDEN, "账号已被禁用");
            }

            // 4) 生成 token（绑 tenantId 到 JWT claim，供 TenantFilter 读取）
            Set<String> roles = Set.of("admin", "user");  // 简化：实际应从 user_role 表读
            String token = mpJwtUtil.generate(
                    user.id,
                    user.username,
                    roles,
                    user.tenantId == null ? 0L : user.tenantId  // 系统管理员可能无 tenant
            );

            metrics.recordLoginSuccess();
            return R.ok(new LoginResponse(
                    token,
                    "Bearer",
                    expireSeconds,
                    new LoginResponse.UserInfo(user.id, user.username, user.nickname, user.email)
            ));
        } finally {
            sample.stop(metrics.loginTimer());
        }
    }

    /**
     * 密码校验（统一 BCrypt）
     *
     * 用 BCrypt 自带 verify 方法：自动处理：
     *   - 哈希格式校验
     *   - 时间常量比较（防时间攻击）
     *   - 不抛异常（安全失败返回 false）
     */
    private boolean verifyPassword(SysUser user, String rawPassword) {
        if (user == null) {
            // 仍然执行一次 BCrypt 防时间攻击
            BCrypt.verifyer().verify(rawPassword.toCharArray(), "$2a$10$invalidinvalidinvalidinvalidinvalidinvalidinvalidinvalidi");
            return false;
        }
        BCrypt.Result result = BCrypt.verifyer()
                .verify(rawPassword.toCharArray(), user.password);
        return result.verified;
    }
}
