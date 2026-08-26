package com.jade.demo.repository;

import com.jade.demo.entity.SysTenant;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SysTenantRepository implements PanacheRepository<SysTenant> {
}
