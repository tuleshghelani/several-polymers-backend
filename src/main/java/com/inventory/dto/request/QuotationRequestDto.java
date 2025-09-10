package com.inventory.dto.request;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class QuotationRequestDto {
    private Long quotationId;
    private Long customerId;
    private String customerName;
    private String quoteNumber;
    private String remarks;
    private String termsConditions;
    private String contactNumber;
    private String address;
    private BigDecimal quotationDiscountPercentage;
    private Long transportMasterId;
    private String caseNumber;
    private BigDecimal packagingAndForwadingCharges;


    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate quoteDate;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate validUntil;

    private List<QuotationItemRequestDto> items;
}
