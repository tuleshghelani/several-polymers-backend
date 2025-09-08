package com.inventory.repository;

import com.inventory.entity.Sale;
import com.inventory.entity.TransportBag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransportBagRepository extends JpaRepository<TransportBag, Long> {
    List<TransportBag> findByTransportId(Long transportId);
    void deleteByTransportId(Long transportId);
} 