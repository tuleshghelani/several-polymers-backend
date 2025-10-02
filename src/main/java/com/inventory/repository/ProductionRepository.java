package com.inventory.repository;

import com.inventory.entity.Production;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductionRepository extends JpaRepository<Production, Long> {
    @Modifying
    @Query(value = "DELETE FROM production WHERE batch_id = :batchId", nativeQuery = true)
    void deleteByBatchId(@Param("batchId") Long batchId);
    
    List<Production> findByBatchId(Long batchId);
}


