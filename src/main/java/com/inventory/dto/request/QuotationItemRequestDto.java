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
    private Long productId;
    private String productType;
    private String calculationType;
//    private BigDecimal weight;
    private BigDecimal quantity;
    private BigDecimal unitPrice;
    private BigDecimal taxPercentage = BigDecimal.valueOf(18); // Default 18%
    private BigDecimal discountPercentage = BigDecimal.ZERO;
    private BigDecimal finalPrice;
    private BigDecimal quotationDiscountPercentage = BigDecimal.ZERO;
    private BigDecimal quotationDiscountAmount = BigDecimal.ZERO;
    private BigDecimal quotationDiscountPrice = BigDecimal.ZERO;
//    private BigDecimal loadingCharge;
}
