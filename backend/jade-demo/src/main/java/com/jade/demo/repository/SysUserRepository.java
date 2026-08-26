package com.jade.demo.repository;

import com.jade.demo.entity.SysUser;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class SysUserRepository implements PanacheRepository<SysUser> {

    public Optional<SysUser> findByUsername(String username) {
        return find("username = ?1 and deleted = false", username).firstResultOptional();
    }
}
