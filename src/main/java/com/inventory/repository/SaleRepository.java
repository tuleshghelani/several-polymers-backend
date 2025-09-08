package com.inventory.repository;

import com.inventory.entity.Purchase;
import com.inventory.entity.Sale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SaleRepository extends JpaRepository<Sale, Long> {
    @Query("SELECT s FROM Sale s WHERE s.client.id = :clientId")
    List<Purchase> findByClientId(@Param("clientId") Long clientId);
    
    @Query("SELECT s FROM Sale s WHERE s.id = :id AND s.client.id = :clientId")
    Optional<Purchase> findByIdAndClientId(@Param("id") Long id, @Param("clientId") Long clientId);

    @Query("SELECT s FROM Sale s WHERE s.client.id = :clientId AND s.saleDate BETWEEN :startDate AND :endDate")
    List<Sale> findByClientIdAndDateRange(
        @Param("clientId") Long clientId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
}