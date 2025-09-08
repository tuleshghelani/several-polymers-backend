package com.inventory.repository;

import com.inventory.entity.PowderCoatingProcess;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PowderCoatingProcessRepository extends JpaRepository<PowderCoatingProcess, Long> {
    List<PowderCoatingProcess> findByCustomerId(Long customerId);
    List<PowderCoatingProcess> findByProductId(Long productId);
    @Query("SELECT p FROM PowderCoatingProcess p WHERE p.id IN :ids")
    List<PowderCoatingProcess> findAllById(@org.springframework.data.repository.query.Param("ids") List<Long> ids);
} 