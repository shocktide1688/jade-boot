package com.jade.admin.repository;

import com.jade.admin.entity.SysRole;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class SysRoleRepository implements PanacheRepository<SysRole> {

    public List<SysRole> listByUserId(Long userId) {
        return getEntityManager().createQuery(
                "SELECT r FROM SysRole r WHERE r.id IN " +
                "(SELECT ur.roleId FROM SysUserRole ur WHERE ur.userId = :uid) AND r.deleted = false",
                SysRole.class)
                .setParameter("uid", userId)
                .getResultList();
    }

    public Optional<SysRole> findByCode(String roleCode) {
        return find("roleCode = ?1 and deleted = false", roleCode).firstResultOptional();
    }
}
