package com.jade.admin.service;

import com.jade.admin.entity.SysConfig;
import com.jade.admin.repository.SysConfigRepository;
import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.value.ValueCommands;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

/**
 * 登录失败计数（Redis 优先，按用户名维度）
 *
 * Redis 正常时为分布式计数（多实例共享）；Redis 不可用时自动降级为进程内计数
 * （双写保连续，切换期间计数不丢），超次强制验证码功能不中断——
 * 单机语义：重启归零、多实例各自计数，恢复后自动回到 Redis。
 *
 * 计数窗口 = sys.account.lockTime 分钟：每次失败刷新 TTL，窗口内无新失败则归零；
 * 连续失败达到 sys.account.passwordRetryCount 后要求验证码，直到登录成功重置。
 * 配置来自 sys_config（V5 预置键，改库即生效）。
 */
@ApplicationScoped
public class LoginAttemptService {

    private static final Logger LOG = Logger.getLogger(LoginAttemptService.class);

    private static final String KEY_PREFIX = "login:fail:";
    private static final String CFG_CAPTCHA_ENABLED = "sys.account.captchaEnabled";
    private static final String CFG_RETRY_COUNT = "sys.account.passwordRetryCount";
    private static final String CFG_LOCK_TIME = "sys.account.lockTime";
    private static final int DEFAULT_RETRY_COUNT = 5;
    private static final int DEFAULT_LOCK_TIME_MINUTES = 10;
    /** Redis 故障后熔断时长，到期重试 */
    private static final long REDIS_RETRY_MS = 30_000;

    @Inject
    RedisDataSource redis;

    @Inject
    SysConfigRepository configRepository;

    private final InMemoryTtlStore memory = new InMemoryTtlStore();
    /** 0 = Redis 可用；否则为下次允许重试 Redis 的时间戳 */
    private volatile long redisRetryAt;

    /**
     * 是否要求验证码：captchaEnabled=true 始终要求；否则失败次数达到阈值后要求
     */
    public boolean captchaRequired(String username) {
        if (Boolean.parseBoolean(getConfig(CFG_CAPTCHA_ENABLED, "false"))) {
            return true;
        }
        int count = failCount(username);
        return count > 0 && count >= retryCount();
    }

    public int failCount(String username) {
        String key = KEY_PREFIX + normalize(username);
        if (redisAvailable()) {
            try {
                String v = cmd().get(key);
                return v == null ? 0 : Integer.parseInt(v);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
        String v = memory.get(key);
        return v == null ? 0 : Integer.parseInt(v);
    }

    /** 记录一次失败：INCR 并刷新计数窗口（进程内始终双写，Redis 故障时计数不中断） */
    public void recordFailure(String username) {
        String key = KEY_PREFIX + normalize(username);
        long ttl = lockTimeMinutes() * 60L;
        memory.increment(key, ttl);
        if (redisAvailable()) {
            try {
                cmd().incr(key);
                redis.key().expire(key, ttl);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
    }

    /** 登录成功后清除计数 */
    public void reset(String username) {
        String key = KEY_PREFIX + normalize(username);
        memory.del(key);
        if (redisAvailable()) {
            try {
                redis.key().del(key);
            } catch (Exception e) {
                markRedisDown(e);
            }
        }
    }

    private String getConfig(String key, String defaultValue) {
        SysConfig c = configRepository.find("configKey = ?1 and deleted = false", key).firstResult();
        return c == null || c.configValue == null || c.configValue.isBlank() ? defaultValue : c.configValue;
    }

    private int retryCount() {
        try {
            return Math.max(1, Integer.parseInt(getConfig(CFG_RETRY_COUNT, String.valueOf(DEFAULT_RETRY_COUNT))));
        } catch (NumberFormatException e) {
            return DEFAULT_RETRY_COUNT;
        }
    }

    private int lockTimeMinutes() {
        try {
            return Math.max(1, Integer.parseInt(getConfig(CFG_LOCK_TIME, String.valueOf(DEFAULT_LOCK_TIME_MINUTES))));
        } catch (NumberFormatException e) {
            return DEFAULT_LOCK_TIME_MINUTES;
        }
    }

    private String normalize(String username) {
        return username == null ? "" : username.trim().toLowerCase();
    }

    private ValueCommands<String, String> cmd() {
        return redis.value(String.class, String.class);
    }

    private boolean redisAvailable() {
        return System.currentTimeMillis() >= redisRetryAt;
    }

    private void markRedisDown(Exception e) {
        redisRetryAt = System.currentTimeMillis() + REDIS_RETRY_MS;
        LOG.warnf(e, "[LoginAttempt] Redis 不可用，%ds 内降级为进程内计数", REDIS_RETRY_MS / 1000);
    }
}
