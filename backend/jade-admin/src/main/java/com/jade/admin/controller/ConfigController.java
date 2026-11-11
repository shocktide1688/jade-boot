package com.jade.admin.controller;

import com.jade.admin.aspect.Log;
import com.jade.admin.entity.SysConfig;
import com.jade.admin.repository.SysConfigRepository;
import com.jade.common.api.R;
import com.jade.common.exception.BizException;
import com.jade.common.constant.ResultCode;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Path("/api/v1/configs")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "参数配置")
@RolesAllowed("admin")
public class ConfigController {

    @Inject
    SysConfigRepository configRepository;

    /** 公开可读的应用级白名单键 (登录页未登录也要取欢迎语/标题), 防止误存敏感值被匿名拉走 */
    private static final List<String> PUBLIC_APP_KEYS = List.of("app.title", "login.welcome");

    @GET
    @Path("/public")
    @PermitAll
    @Operation(summary = "公开应用配置 (白名单键, 供登录页/布局读取标题与欢迎语)")
    public R<Map<String, String>> publicApp() {
        return R.ok(readPublicApp());
    }

    @PUT
    @Path("/app")
    @Transactional
    @Log(title = "参数配置", businessType = 2)
    @Operation(summary = "保存应用配置 (应用标题/登录欢迎语, 白名单外键忽略)")
    public R<Map<String, String>> saveApp(Map<String, String> body) {
        if (body != null) {
            for (String key : PUBLIC_APP_KEYS) {
                String value = body.get(key);
                if (value != null) {
                    upsertApp(key, value);
                }
            }
        }
        return R.ok(readPublicApp());
    }

    private Map<String, String> readPublicApp() {
        Map<String, String> out = new LinkedHashMap<>();
        for (String key : PUBLIC_APP_KEYS) {
            SysConfig c = configRepository.find("configKey = ?1 and deleted = false", key).firstResult();
            if (c != null) {
                out.put(key, c.configValue == null ? "" : c.configValue);
            }
        }
        return out;
    }

    private void upsertApp(String key, String value) {
        SysConfig c = configRepository.find("configKey = ?1 and deleted = false", key).firstResult();
        if (c == null) {
            c = new SysConfig();
            c.configKey = key;
            c.configName = "app.title".equals(key) ? "应用标题" : "登录欢迎语";
            c.configType = "system";
            c.isBuiltin = 1;
            c.createdAt = LocalDateTime.now();
        }
        c.configValue = value;
        c.updatedAt = LocalDateTime.now();
        configRepository.persist(c);
    }

    @GET
    @Path("/all")
    @Operation(summary = "所有参数配置")
    public R<List<SysConfig>> all() {
        return R.ok(configRepository.list("deleted = false"));
    }

    @GET
    @Path("/key/{key}")
    @Operation(summary = "按 key 查配置")
    public R<String> getByKey(@PathParam("key") String key) {
        SysConfig c = configRepository.find("configKey = ?1 and deleted = false", key).firstResult();
        if (c == null) return R.fail(404, "配置不存在");
        return R.ok(c.configValue);
    }

    @POST
    @Transactional
    @Log(title = "参数配置", businessType = 1)
    @Operation(summary = "新建配置")
    public R<SysConfig> create(SysConfig config) {
        config.id = null;
        if (config.configType == null) config.configType = "business";
        if (config.isBuiltin == null) config.isBuiltin = 0;
        configRepository.persist(config);
        return R.ok(config);
    }

    @PUT
    @Path("/{id}")
    @Transactional
    @Log(title = "参数配置", businessType = 2)
    @Operation(summary = "更新配置")
    public R<SysConfig> update(@PathParam("id") Long id, SysConfig req) {
        SysConfig c = configRepository.findById(id);
        if (c == null) return R.fail(404, "配置不存在");
        if (c.isBuiltin != null && c.isBuiltin == 1) {
            throw new BizException(ResultCode.BAD_REQUEST, "系统内置配置不能修改");
        }
        if (req.configName != null) c.configName = req.configName;
        if (req.configValue != null) c.configValue = req.configValue;
        if (req.remark != null) c.remark = req.remark;
        configRepository.persist(c);
        return R.ok(c);
    }

    @DELETE
    @Path("/{id}")
    @Transactional
    @Log(title = "参数配置", businessType = 3)
    @Operation(summary = "删除配置")
    public R<Void> delete(@PathParam("id") Long id) {
        SysConfig c = configRepository.findById(id);
        if (c == null) return R.fail(404, "配置不存在");
        if (c.isBuiltin != null && c.isBuiltin == 1) {
            throw new BizException(ResultCode.BAD_REQUEST, "系统内置配置不能删除");
        }
        c.deleted = true;
        configRepository.persist(c);
        return R.ok();
    }
}
