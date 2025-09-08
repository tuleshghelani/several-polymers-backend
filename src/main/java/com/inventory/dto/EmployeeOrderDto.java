package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.config.CustomDateDeserializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import java.time.OffsetDateTime;
import java.util.List;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EmployeeOrderDto {
    private Long id;
    private Long productId;
    private String productName;
    private List<Long> employeeIds;
    private List<String> employeeNames;
    private Integer quantity;
    private String remarks;
    private String status;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long clientId;
    // Search parameters
    private String search;
    private Integer currentPage = 0;
    private Integer perPageRecord = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
    
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime startDate;
    
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime endDate;

} 