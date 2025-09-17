package com.inventory.dto.request;

import java.math.BigDecimal;
import java.util.List;

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
public class QuotationItemRequestDto {
    // Common identifiers and status (also used by quotation status update)
    private Long id;
    private String status;
    private Long productId;
    private Long brandId;
    private String productType;
    private String calculationType;
//    private BigDecimal weight;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal taxPercentage = BigDecimal.valueOf(18); // Default 18%
    private BigDecimal finalPrice;
    private BigDecimal quotationDiscountPercentage = BigDecimal.ZERO;
    private BigDecimal quotationDiscountAmount = BigDecimal.ZERO;
    private BigDecimal quotationDiscountPrice = BigDecimal.ZERO;
    private Integer numberOfRoll = 0;
    private Integer createdRoll;
    private BigDecimal weightPerRoll = BigDecimal.ZERO;
    private String remarks;
//    private BigDecimal loadingCharge;
    private Boolean isProduction;
    private String quotationItemStatus;
    private List<String> quotationItemStatuses;

    // Search & pagination
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
}
