package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.config.CustomDateDeserializer;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DailyProfitDto {
    private Long id;
    private String search;
    private Long saleId;
    private Long productId;
    private Long clientId;
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime startDate;
    
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy HH:mm:ss", timezone = "IST")
    private OffsetDateTime endDate;
    
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
} 