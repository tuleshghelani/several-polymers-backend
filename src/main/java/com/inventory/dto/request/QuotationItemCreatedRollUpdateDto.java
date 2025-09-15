package com.inventory.dto.request;

import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@NoArgsConstructor
public class QuotationItemCreatedRollUpdateDto {
    private Long id;
    private Integer createdRoll;
}


