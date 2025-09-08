package com.inventory.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PowderCoatingProcessPdfDto {
    private Long customerId;
    private List<Long> processIds;
    private Long clientId;
} 