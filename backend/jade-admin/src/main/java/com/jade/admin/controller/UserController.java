package com.jade.admin.controller;

import com.jade.common.api.PageResult;
import com.jade.common.api.R;
import com.jade.security.entity.SysUser;
import com.jade.security.repository.SysUserRepository;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/users")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "用户管理")
public class UserController {

    @Inject
    SysUserRepository userRepository;

    @GET
    @PermitAll
    @Operation(summary = "分页查询用户")
    public R<PageResult<SysUser>> page(
            @QueryParam("page") @DefaultValue("1") int page,
            @QueryParam("size") @DefaultValue("10") int size,
            @QueryParam("keyword") String keyword) {

        boolean hasKeyword = keyword != null && !keyword.isBlank();
        String query = hasKeyword
                ? "deleted = false and (username like ?1 or nickname like ?1)"
                : "deleted = false";
        Object params = hasKeyword ? "%" + keyword + "%" : null;

        PanacheQuery<SysUser> panacheQuery = userRepository.find(
                query,
                Sort.by("createdAt").descending(),
                hasKeyword ? new Object[]{params} : new Object[]{}
        );

        long total = panacheQuery.count();
        List<SysUser> records = panacheQuery.page(Page.of(page - 1, size)).list();

        return R.ok(PageResult.of(records, total, page, size));
    }

    @GET
    @Path("/{id}")
    @PermitAll
    @Operation(summary = "根据 ID 查询用户")
    public R<SysUser> getById(@PathParam("id") Long id) {
        SysUser user = userRepository.findById(id);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }
        return R.ok(user);
    }
}
