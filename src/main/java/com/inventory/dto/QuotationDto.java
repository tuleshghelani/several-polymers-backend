package com.inventory.dto;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.inventory.dto.request.QuotationItemRequestDto;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotationDto {
    private Long id;
    private Long customerId;
    private String customerName;
    private String quoteNumber;
    private BigDecimal totalAmount;
    private String status;
    private List<String> quotationStatus;
    private Long transportMasterId;
    private String caseNumber;
    private BigDecimal packagingAndForwadingCharges;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate quoteDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validUntil;

    private String remarks;
    private String termsConditions;
    private BigDecimal quotationDiscountPercentage;
    private BigDecimal quotationDiscountAmount;
    private List<Long> quotationItemIds;
    private List<QuotationItemRequestDto> items;

    // Search parameters
    private String search;
    private Integer currentPage = 0;
    private Integer perPageRecord = 10;
    //    private Integer currentPage;
//    private Integer perPageRecord;
    private String sortBy = "id";
    private String sortDir = "desc";
    private Long clientId;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate startDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate endDate;

    private String contactNumber;
    private String address;
}