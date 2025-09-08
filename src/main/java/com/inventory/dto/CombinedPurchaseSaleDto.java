package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.config.CustomDateDeserializer;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class CombinedPurchaseSaleDto {
    private Long productId;
    private BigDecimal purchaseUnitPrice;

    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime purchaseDate;
    private String purchaseInvoiceNumber;
    private BigDecimal purchaseOtherExpenses;
    private Integer quantity;
    private BigDecimal saleUnitPrice;

    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime saleDate;
    private String saleInvoiceNumber;
    private BigDecimal saleOtherExpenses;

    // Purchase discount fields
    private BigDecimal purchaseDiscount;
    private BigDecimal purchaseDiscountAmount;
    private BigDecimal purchaseDiscountPrice;
    
    // Sale discount fields
    private BigDecimal saleDiscount;
    private BigDecimal saleDiscountAmount;
    private BigDecimal saleDiscountPrice;
    private Long clientId;
} 