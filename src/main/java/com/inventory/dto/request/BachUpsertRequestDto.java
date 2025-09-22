package com.inventory.dto.request;

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
public class BachUpsertRequestDto {
    private Long id; // edit time only, bach id
    private LocalDate date;
    private String shift; // varchar(2)
    private BigDecimal resignBagUse;
    private BigDecimal resignBagOpeningStock;
    private BigDecimal cpwBagUse;
    private BigDecimal cpwBagOpeningStock;
    private Long machineId;

    private List<MixerItem> mixer;
    private List<ProductionItem> production;

    @Data
    public static class MixerItem {
        private Long bachId; // optional when creating
        private Long productId;
        private BigDecimal quantity;
    }

    @Data
    public static class ProductionItem {
        private Long bachId; // optional when creating
        private Long productId;
        private BigDecimal quantity;
        private Integer numberOfRoll;
    }
}


