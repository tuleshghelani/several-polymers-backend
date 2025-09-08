package com.inventory.dto.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BatchDeleteRequestDto {
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate startDate;
    
    @JsonFormat(pattern = "dd-MM-yyyy")
    private LocalDate endDate;
    
    private List<String> type;
} 