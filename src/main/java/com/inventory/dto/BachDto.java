package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BachDto {
    private Long id;
    private LocalDate date;
    private String shift;
    private String name;
    private BigDecimal resignBagUse;
    private BigDecimal resignBagOpeningStock;
    private BigDecimal cpwBagUse;
    private BigDecimal cpwBagOpeningStock;
    private Long machineId;
    private Long clientId;

    private String search;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
}


