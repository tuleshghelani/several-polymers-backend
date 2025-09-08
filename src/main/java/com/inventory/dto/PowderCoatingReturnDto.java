package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PowderCoatingReturnDto {
    private Long id;
    private Long processId;
    private Integer returnQuantity;
    private OffsetDateTime createdAt;
    private String processDetails;
    private String customerName;
    private String productName;
    private Long clientId;

    private String search;
    private Integer currentPage = 0;
    private Integer perPageRecord = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
} 