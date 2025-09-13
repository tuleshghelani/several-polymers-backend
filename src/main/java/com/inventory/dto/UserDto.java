package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String password;
    private String status;
    private List<String> roles;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
    private Long clientId;
    private String email;
    
    // For search/filter purposes
    private String searchTerm;
    private Integer page;
    private Integer size;
}