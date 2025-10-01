package com.inventory.repository;

import com.inventory.entity.MachineMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MachineMasterRepository extends JpaRepository<MachineMaster, Long> {
    Optional<MachineMaster> findByNameAndClient_Id(String name, Long clientId);
    List<MachineMaster> findByClient_IdOrderByNameAsc(Long clientId);
}


