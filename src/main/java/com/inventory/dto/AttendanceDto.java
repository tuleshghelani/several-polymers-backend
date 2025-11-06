package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

/**
 * Unified DTO for Attendance operations
 * Handles both request (filtering, pagination) and response (attendance data)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AttendanceDto {
    
    // ==================== Attendance Fields (Response) ====================
    private Long id;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime startDateTime;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime endDateTime;
    
    private BigDecimal regularHours;
    private BigDecimal overtimeHours;
    private BigDecimal regularPay;
    private BigDecimal overtimePay;
    private BigDecimal totalPay;
    
    private String remarks;
    private String shift;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX")
    private OffsetDateTime createdAt;
    
    // Employee information (uses existing EmployeeDto)
    private EmployeeDto employee;
    
    // Created by user information
    private String createdByName;
    private Long createdById;
    
    // Client information
    private Long clientId;
    
    // ==================== Request Fields (Filtering & Pagination) ====================
    
    // Pagination parameters
    @JsonIgnore
    @Builder.Default
    private Integer page = 0;
    @JsonIgnore
    @Builder.Default
    private Integer size = 10;
    
    // Sorting parameters
    @JsonIgnore
    @Builder.Default
    private String sortBy = "id";
    @JsonIgnore
    @Builder.Default
    private String sortDir = "desc";
    
    // Filter parameters
    @JsonIgnore
    private Long employeeId;
    
    @JsonIgnore
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate startDate;
    
    @JsonIgnore
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate endDate;
    
    @JsonIgnore
    private String employeeName; // For searching by employee name
    @JsonIgnore
    private String employeeMobile; // For searching by employee mobile
}
