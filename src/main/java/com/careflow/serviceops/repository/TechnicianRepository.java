package com.careflow.serviceops.repository;

import com.careflow.serviceops.domain.Technician;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TechnicianRepository extends JpaRepository<Technician, UUID> {
    List<Technician> findByActiveTrueOrderByFullNameAsc();
}

