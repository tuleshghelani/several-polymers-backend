package com.inventory.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
public class QuotationItemNumberOfRollUpdateDto {
    private Long id;
    private Integer numberOfRoll;
}


