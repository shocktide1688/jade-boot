package com.jade.admin.controller;

import com.jade.common.api.R;
import com.jade.admin.dto.LoginRequest;
import com.jade.admin.dto.LoginResponse;
import com.jade.admin.service.AuthService;
import com.jade.redis.annotation.RateLimit;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "认证管理")
public class AuthController {

    @Inject
    AuthService authService;

    @POST
    @Path("/login")
    @PermitAll
    @RateLimit(key = "auth:login", limit = 5, window = 60, message = "登录尝试过于频繁，请 1 分钟后再试")
    @Operation(summary = "登录", description = "用户名密码登录，返回 JWT（每 IP 每分钟最多 5 次）")
    public R<LoginResponse> login(@Valid LoginRequest req) {
        return authService.login(req);
    }

    @GET
    @Path("/me")
    @Authenticated
    @Operation(summary = "当前登录用户信息")
    public R<String> me() {
        return R.ok("ok");
    }

    @POST
    @Path("/logout")
    @Authenticated
    @Operation(summary = "登出（前端清 token 即可，后端可选写黑名单）")
    public R<Void> logout() {
        return R.ok();
    }
}
