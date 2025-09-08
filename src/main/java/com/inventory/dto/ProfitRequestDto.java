package com.inventory.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.inventory.config.CustomDateDeserializer;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProfitRequestDto {
    private Long clientId;
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    private OffsetDateTime startDate;
    
    @JsonDeserialize(using = CustomDateDeserializer.class)
    @JsonFormat(pattern = "dd-MM-yyyy", timezone = "Asia/Kolkata")
    private OffsetDateTime endDate;
} 