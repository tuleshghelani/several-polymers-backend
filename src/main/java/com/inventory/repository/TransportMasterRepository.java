package com.inventory.repository;

import com.inventory.entity.TransportMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TransportMasterRepository extends JpaRepository<TransportMaster, Long> {
    Optional<TransportMaster> findByNameAndClient_Id(String name, Long clientId);
    Optional<TransportMaster> findByNameAndIdNotInAndClient_Id(String name, List<Long> ids, Long clientId);
}


