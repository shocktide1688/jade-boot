package com.jade.demo.controller;

import com.jade.common.api.R;
import com.jade.demo.entity.SysProject;
import com.jade.demo.entity.SysTenant;
import com.jade.demo.repository.SysProjectRepository;
import com.jade.demo.repository.SysTenantRepository;
import com.jade.security.context.TenantContext;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

/**
 * 多租户接口（修复了安全审计中的越权问题）
 *
 * 权限规则：
 *   - 租户管理（/tenants）   : 仅 admin 角色
 *   - 项目管理（/projects）   : 任何已登录用户，但只能操作自己所属租户的数据
 *   - 创建项目时自动绑定当前用户所属租户（不允许通过 X-Tenant-Id 切换）
 */
@Path("/api/v1")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "多租户")
@Authenticated   // 修复 1：移除类级 @PermitAll，必须登录
public class TenantController {

    @Inject
    SysTenantRepository tenantRepository;

    @Inject
    SysProjectRepository projectRepository;

    @Inject
    SecurityIdentity identity;

    // ============== 租户管理（仅管理员）==============

    @POST
    @Path("/tenants")
    @RolesAllowed("admin")   // 修复 2：必须 admin 角色
    @Transactional
    @Operation(summary = "创建租户（仅管理员）")
    public R<SysTenant> createTenant(SysTenant tenant) {
        tenantRepository.persist(tenant);
        return R.ok(tenant);
    }

    @GET
    @Path("/tenants")
    @RolesAllowed("admin")   // 修复 2：仅管理员可查看所有租户
    @Operation(summary = "查询所有租户（仅管理员）")
    public R<List<SysTenant>> listTenants() {
        return R.ok(tenantRepository.listAll());
    }

    // ============== 项目（任何登录用户，只能看自己租户的）==============

    @POST
    @Path("/projects")
    @Transactional
    @Operation(summary = "创建项目（自动绑定当前用户所属租户）")
    public R<SysProject> createProject(SysProject project) {
        // 修复 3：使用 JWT 中的 tenantId，不允许通过 header 切换
        Long tenantId = TenantContext.require();
        project.tenantId = tenantId;
        projectRepository.persist(project);
        return R.ok(project);
    }

    @GET
    @Path("/projects")
    @Operation(summary = "查询当前租户的项目（隔离）")
    public R<List<SysProject>> listProjects() {
        Long tenantId = TenantContext.require();
        return R.ok(projectRepository.list("tenantId = ?1 and deleted = false", tenantId));
    }
}
