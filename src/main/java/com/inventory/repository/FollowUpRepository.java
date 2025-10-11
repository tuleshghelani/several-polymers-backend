package com.inventory.repository;

import com.inventory.entity.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FollowUpRepository extends JpaRepository<FollowUp, Long> {
    List<FollowUp> findByEnquiry_IdAndClient_Id(Long enquiryId, Long clientId);
    List<FollowUp> findByClient_Id(Long clientId);
    Optional<FollowUp> findByIdAndClient_Id(Long id, Long clientId);
}
