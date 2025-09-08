package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PurchaseSearchDto {
    private String invoiceNumber;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long productId;
    private Long clientId;
    private Integer currentPage = 0;
    private Integer perPageRecord = 10;
    private String sortBy = "purchaseDate";
    private String sortDir = "desc";
} 