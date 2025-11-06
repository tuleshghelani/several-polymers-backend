package com.inventory.repository;

import com.inventory.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    @Query("SELECT a FROM Attendance a WHERE a.client.id = :clientId AND a.startDateTime BETWEEN :startDate AND :endDate")
    List<Attendance> findByClientIdAndDateRange(
        @Param("clientId") Long clientId,
        @Param("startDate") OffsetDateTime startDate,
        @Param("endDate") OffsetDateTime endDate
    );
    
    /**
     * Find all attendance records by client with pagination
     */
    @Query("SELECT a FROM Attendance a WHERE a.client.id = :clientId")
    Page<Attendance> findAllByClientId(@Param("clientId") Long clientId, Pageable pageable);
} 