package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductDto {
    private Long id;
    private String name;
    private String search;
    private Long categoryId;
    private String description;
    private BigDecimal purchaseAmount;
    private BigDecimal saleAmount;
    private BigDecimal minimumStock;
    private String status;
    private String measurement;
    private BigDecimal weight;
    private BigDecimal remainingQuantity;
    private BigDecimal blockedQuantity;
    private BigDecimal totalRemainingQuantity;
    private Long clientId;
    private BigDecimal taxPercentage;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
}