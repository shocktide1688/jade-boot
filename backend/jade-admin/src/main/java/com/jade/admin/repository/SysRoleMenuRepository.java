package com.jade.admin.repository;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;

import java.util.List;

@ApplicationScoped
public class SysRoleMenuRepository {

    @Inject
    EntityManager em;

    public List<Long> listMenuIdsByRoleId(Long roleId) {
        return em.createQuery("SELECT rm.menuId FROM SysRoleMenu rm WHERE rm.roleId = :rid", Long.class)
                .setParameter("rid", roleId)
                .getResultList();
    }

    public void deleteByRoleId(Long roleId) {
        em.createNativeQuery("DELETE FROM sys_role_menu WHERE role_id = ?1")
                .setParameter(1, roleId)
                .executeUpdate();
    }

    public void insert(Long roleId, Long menuId) {
        em.createNativeQuery("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?1, ?2) ON CONFLICT DO NOTHING")
                .setParameter(1, roleId)
                .setParameter(2, menuId)
                .executeUpdate();
    }
}
