package com.inventory.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RegisterRequest {
    private Long id;
    private String email;
    private String phoneNumber;
    private String password;
    private String confirmPassword; // For password update
    private String firstName;
    private String lastName;
    private String status;
    private java.util.List<String> roles;
    private Long clientId;
    
    // Search and pagination parameters
    private String searchTerm;
    private Integer page = 0;
    private Integer size = 10;
}