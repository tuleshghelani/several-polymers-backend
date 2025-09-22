package com.inventory.repository;

import com.inventory.entity.Production;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductionRepository extends JpaRepository<Production, Long> {
    @Modifying
    @Query(value = "DELETE FROM production WHERE bach_id = :bachId", nativeQuery = true)
    void deleteByBachId(@Param("bachId") Long bachId);
}


