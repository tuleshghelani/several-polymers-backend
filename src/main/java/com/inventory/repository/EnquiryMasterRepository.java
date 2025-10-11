package com.inventory.repository;

import com.inventory.entity.EnquiryMaster;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EnquiryMasterRepository extends JpaRepository<EnquiryMaster, Long> {
    Optional<EnquiryMaster> findByNameAndClient_Id(String name, Long clientId);
    Optional<EnquiryMaster> findByNameAndIdNotInAndClient_Id(String name, List<Long> ids, Long clientId);
}
