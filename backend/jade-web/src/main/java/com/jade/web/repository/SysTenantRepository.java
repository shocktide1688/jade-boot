package com.jade.web.repository;

import com.jade.web.entity.SysTenant;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SysTenantRepository implements PanacheRepository<SysTenant> {
}
