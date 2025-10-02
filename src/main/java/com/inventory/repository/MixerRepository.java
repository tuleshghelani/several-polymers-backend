package com.inventory.repository;

import com.inventory.entity.Mixer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MixerRepository extends JpaRepository<Mixer, Long> {
    @Modifying
    @Query(value = "DELETE FROM mixer WHERE batch_id = :batchId", nativeQuery = true)
    void deleteByBatchId(@Param("batchId") Long batchId);
    
    List<Mixer> findByBatchId(Long batchId);
}


