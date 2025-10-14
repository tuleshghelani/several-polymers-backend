package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.config.CustomDateDeserializer;
import com.inventory.config.CustomLocalDateDeserializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentHistoryDto {
    private Long id;
    private BigDecimal amount;
    private Long customerId;
    private String customerName;
    private String type; // 'C' for Cash, 'B' for Bank
    private String remarks;
    private Boolean isReceived = true;
    
    @JsonDeserialize(using = CustomLocalDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy", timezone = "IST")
    private LocalDate date;
    
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime createdAt;
    
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime updatedAt;
    
    private Long createdById;
    private String createdByName;
    private Long updatedById;
    private String updatedByName;
    
    // Search and pagination parameters
    private String search;
    private Integer currentPage = 0;
    private Integer perPageRecord = 10;
    private Long clientId;
    
    @JsonDeserialize(using = CustomLocalDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy", timezone = "IST")
    private LocalDate startDate;
    
    @JsonDeserialize(using = CustomLocalDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy", timezone = "IST")
    private LocalDate endDate;
}