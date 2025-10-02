package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BachDto {
    private Long id;
    private LocalDate date;
    private String shift;
    private String name;
    private String operator;
    private BigDecimal resignBagUse;
    private BigDecimal resignBagOpeningStock;
    private BigDecimal cpwBagUse;
    private BigDecimal cpwBagOpeningStock;
    private Long machineId;
    private String machineName;
    private Long clientId;
    private String clientName;
    private String createdBy;
    private String createdAt;
    private String updatedAt;

    private List<MixerDetailDto> mixerItems;
    private List<ProductionDetailDto> productionItems;

    private String search;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "id";
    private String sortDir = "desc";

    @Data
    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MixerDetailDto {
        private Long id;
        private Long productId;
        private String productName;
        private String productDescription;
        private String productMeasurement;
        private BigDecimal productWeight;
        private BigDecimal productPurchaseAmount;
        private BigDecimal productSaleAmount;
        private BigDecimal productRemainingQuantity;
        private BigDecimal productTaxPercentage;
        private String productStatus;
        private BigDecimal quantity;
        private String categoryName;
    }

    @Data
    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ProductionDetailDto {
        private Long id;
        private Long productId;
        private String productName;
        private String productDescription;
        private String productMeasurement;
        private BigDecimal productWeight;
        private BigDecimal productPurchaseAmount;
        private BigDecimal productSaleAmount;
        private BigDecimal productRemainingQuantity;
        private BigDecimal productTaxPercentage;
        private String productStatus;
        private BigDecimal quantity;
        private Integer numberOfRoll;
        private String categoryName;
    }
}


