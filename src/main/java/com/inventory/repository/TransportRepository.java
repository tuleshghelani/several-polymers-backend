package com.inventory.repository;

import com.inventory.entity.Sale;
import com.inventory.entity.Transport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransportRepository extends JpaRepository<Transport, Long> {
    List<Transport> findByCustomerId(Long customerId);
} 