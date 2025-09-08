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
import java.util.Date;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SaleDto {
    private Long id;
    private String name;
    private String search;
    private Long customerId;
    private Long categoryId;
    private String description;
    private BigDecimal purchaseAmount;
    private BigDecimal saleAmount;
    private BigDecimal minimumStock;
    private String status;
    private BigDecimal weight;
    private BigDecimal remainingQuantity;
    private Long clientId;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy", timezone = "IST")
    private OffsetDateTime purchaseDate;
    private String invoiceNumber;
    private BigDecimal otherExpenses;
    private Integer currentPage;
    private Integer perPageRecord;
    private String coilNumber;
    private BigDecimal discount;
    private BigDecimal discountAmount;
    private BigDecimal discountPrice;
    private Date startDate;
    private Date endDate;
    private Boolean isBlack;
}