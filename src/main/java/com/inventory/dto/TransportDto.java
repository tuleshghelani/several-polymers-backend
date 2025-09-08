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
import java.util.List;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TransportDto {
    private Long id;
    private Long customerId;
    private List<BagDto> bags;
    private String customerName;
    private OffsetDateTime createdAt;
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
    private Integer totalBags;
    private BigDecimal weight;
    
    @Data
    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BagDto {
        private Long id;
        private BigDecimal weight;
        private Integer numberOfBags;
        private BigDecimal totalBagWeight;
        private List<BagItemDto> items;
    }
    
    @Data
    @Getter
    @Setter
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class BagItemDto {
        private Long productId;
        private Integer quantity;
        private String remarks;
        private Long clientId;
        // Purchase related fields
        private BigDecimal purchaseUnitPrice;
        private BigDecimal purchaseDiscount;
        private BigDecimal purchaseDiscountAmount;
        private BigDecimal purchaseDiscountPrice;
        
        // Sale related fields
        private BigDecimal saleUnitPrice;
        private BigDecimal saleDiscount;
        private BigDecimal saleDiscountAmount;
        private BigDecimal saleDiscountPrice;
    }
} 