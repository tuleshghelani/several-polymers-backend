package com.inventory.repository;

import com.inventory.entity.Mixer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MixerRepository extends JpaRepository<Mixer, Long> {
    @Modifying
    @Query(value = "DELETE FROM mixer WHERE bach_id = :bachId", nativeQuery = true)
    void deleteByBachId(@Param("bachId") Long bachId);
}


