package com.jade.demo.repository;

import com.jade.demo.entity.SysPatient;
import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class SysPatientRepository implements PanacheRepository<SysPatient> {
}
