package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.config.CustomDateDeserializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PowderCoatingProcessDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private Long productId;
    private String productName;
    private Integer quantity;
    private Integer remainingQuantity;
    private Integer totalBags;
    private String remarks;
    private BigDecimal unitPrice;
    private BigDecimal totalAmount;
    private String status;
    private Integer returnQuantity;
    private Long clientId;

    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime returnDate;

    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime createdAt;
    
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