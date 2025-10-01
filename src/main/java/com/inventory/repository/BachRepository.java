package com.inventory.repository;

import com.inventory.entity.Batch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface BachRepository extends JpaRepository<Batch, Long> {
    @Query(value = "SELECT COUNT(b.id) FROM batch b WHERE b.client_id = :clientId AND b.date = :date", nativeQuery = true)
    long countByClientAndDate(@Param("clientId") Long clientId, @Param("date") LocalDate date);
}


