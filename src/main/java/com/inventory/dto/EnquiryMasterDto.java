package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EnquiryMasterDto {
    private Long id;
    private String name;
    private String mobile;
    private String mail;
    private String subject;
    private String address;
    private String description;
    private String status;
    private String type;
    private String company;
    private String city;
    private String state;
    private Long clientId;

    private String search;
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "id";
    private String sortDir = "desc";
}
