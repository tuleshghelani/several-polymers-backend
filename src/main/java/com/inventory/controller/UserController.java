package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.RegisterRequest;
import com.inventory.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class UserController {
    
    private final UserService userService;
    
    @PostMapping
    public ResponseEntity<ApiResponse<?>> createUser(@RequestBody RegisterRequest userDto) {
        return ResponseEntity.ok(userService.createUser(userDto));
    }
    
    @PutMapping
    public ResponseEntity<ApiResponse<?>> updateUser(@RequestBody RegisterRequest userDto) {
        if (userDto.getId() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User ID is required for update"));
        }
        return ResponseEntity.ok(userService.updateUser(userDto.getId(), userDto));
    }
    
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<?>> searchUsers(@RequestBody RegisterRequest searchDto) {
        return ResponseEntity.ok(userService.searchUsers(searchDto));
    }
    
    @PostMapping("/getById")
    public ResponseEntity<ApiResponse<?>> getUserById(@RequestBody RegisterRequest userDto) {
        return ResponseEntity.ok(userService.getUserDetail(userDto));
    }
    
    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<?>> deleteUser(@RequestBody RegisterRequest userDto) {
        if (userDto.getId() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User ID is required for deletion"));
        }
        return ResponseEntity.ok(userService.deleteUser(userDto.getId()));
    }
    
    @PostMapping("/update-password")
    public ResponseEntity<ApiResponse<?>> updatePassword(@RequestBody RegisterRequest passwordDto) {
        if (passwordDto.getId() == null) {
            return ResponseEntity.badRequest().body(ApiResponse.error("User ID is required for password update"));
        }
        return ResponseEntity.ok(userService.updatePassword(
            passwordDto.getId(), 
            passwordDto.getPassword(), 
            passwordDto.getConfirmPassword()
        ));
    }
    
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<?>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }
} 