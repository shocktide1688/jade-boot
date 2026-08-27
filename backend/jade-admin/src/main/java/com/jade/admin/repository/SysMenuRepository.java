package com.jade.admin.repository;

import com.jade.admin.entity.SysMenu;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;

@ApplicationScoped
public class SysMenuRepository implements PanacheRepository<SysMenu> {

    /** 根据用户 ID 查出所有菜单（含按钮），按 sortOrder 排序 */
    public List<SysMenu> listByUserId(Long userId) {
        return getEntityManager().createQuery(
                "SELECT DISTINCT m FROM SysMenu m WHERE m.id IN " +
                "(SELECT rm.menuId FROM SysRoleMenu rm WHERE rm.roleId IN " +
                "  (SELECT ur.roleId FROM SysUserRole ur WHERE ur.userId = :uid)) " +
                "AND m.deleted = false AND m.status = 1 " +
                "ORDER BY m.parentId, m.sortOrder",
                SysMenu.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    /** 角色绑定的所有 menu_id */
    public List<Long> listMenuIdsByRoleId(Long roleId) {
        return getEntityManager().createQuery(
                "SELECT rm.menuId FROM SysRoleMenu rm WHERE rm.roleId = :rid",
                Long.class)
                .setParameter("rid", roleId)
                .getResultList();
    }
}
