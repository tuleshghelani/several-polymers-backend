package com.inventory.repository;

import com.inventory.entity.EmployeeOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EmployeeOrderRepository extends JpaRepository<EmployeeOrder, Long> {
} 