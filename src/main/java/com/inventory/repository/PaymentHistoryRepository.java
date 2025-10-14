package com.inventory.repository;

import com.inventory.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Long> {
    List<PaymentHistory> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<PaymentHistory> findByClientIdOrderByCreatedAtDesc(Long clientId);
    Optional<PaymentHistory> findByIdAndClientId(Long id, Long clientId);
}