package com.inventory.service;

import com.inventory.dao.UserDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.RegisterRequest;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final UserDao userDao;
    private final PasswordEncoder passwordEncoder;
    private final UtilityService utilityService;
    
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[+]?[0-9]{10,15}$");
    
    @Transactional
    public ApiResponse<?> createUser(RegisterRequest userDto) {
        try {
            validateUserDto(userDto, true);
            
            // Check if phone number already exists
            if (StringUtils.hasText(userDto.getPhoneNumber()) && 
                userRepository.existsByPhoneNumber(userDto.getPhoneNumber())) {
                throw new ValidationException("User with this phone number already exists", HttpStatus.BAD_REQUEST);
            }
            
            // Check if email already exists
            if (StringUtils.hasText(userDto.getEmail()) && 
                userRepository.existsByEmail(userDto.getEmail())) {
                throw new ValidationException("User with this email already exists", HttpStatus.BAD_REQUEST);
            }
            
            UserMaster user = new UserMaster();
            mapDtoToEntity(userDto, user);
            user.setPassword(passwordEncoder.encode(userDto.getPassword()));
            user.setCreatedAt(OffsetDateTime.now());
            user.setUpdatedAt(OffsetDateTime.now());
            
            // Set client from current logged in user
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            user.setClient(currentUser.getClient());
            
            user = userRepository.save(user);
            return ApiResponse.success("User created successfully", mapEntityToDto(user));
            
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Transactional
    public ApiResponse<?> updateUser(Long id, RegisterRequest userDto) {
        try {
            validateUserDto(userDto, false);
            
            UserMaster user = userRepository.findById(id)
                .orElseThrow(() -> new ValidationException("User not found", HttpStatus.NOT_FOUND));
            
            // Check if phone number already exists for other users
            if (StringUtils.hasText(userDto.getPhoneNumber())) {
                Optional<UserMaster> existingUser = userRepository.findByPhoneNumber(userDto.getPhoneNumber());
                if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                    throw new ValidationException("User with this phone number already exists", HttpStatus.BAD_REQUEST);
                }
            }
            
            // Check if email already exists for other users
            if (StringUtils.hasText(userDto.getEmail())) {
                Optional<UserMaster> existingUser = userRepository.findByEmail(userDto.getEmail());
                if (existingUser.isPresent() && !existingUser.get().getId().equals(id)) {
                    throw new ValidationException("User with this email already exists", HttpStatus.BAD_REQUEST);
                }
            }
            
            // Update only allowed fields (not password, not createdAt)
            user.setFirstName(userDto.getFirstName());
            user.setLastName(userDto.getLastName());
            user.setPhoneNumber(userDto.getPhoneNumber());
            user.setEmail(userDto.getEmail());
            user.setStatus(userDto.getStatus());
            user.setRoles(userDto.getRoles() != null ? userDto.getRoles() : new ArrayList<>());
            user.setUpdatedAt(OffsetDateTime.now());
            
            user = userRepository.save(user);
            return ApiResponse.success("User updated successfully", mapEntityToDto(user));
            
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Transactional
    public ApiResponse<?> updatePassword(Long userId, String newPassword, String confirmPassword) {
        try {
            if (!StringUtils.hasText(newPassword)) {
                throw new ValidationException("New password is required", HttpStatus.BAD_REQUEST);
            }
            
            UserMaster user = userRepository.findById(userId)
                .orElseThrow(() -> new ValidationException("User not found", HttpStatus.NOT_FOUND));
            
            user.setPassword(passwordEncoder.encode(newPassword));
            user.setUpdatedAt(OffsetDateTime.now());
            
            // Clear JWT token to force re-login
            user.setJwtToken(null);
            user.setRefreshToken(null);
            user.setRefreshTokenExpiry(null);
            
            userRepository.save(user);
            return ApiResponse.success("Password updated successfully", null);
            
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update password: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    public ApiResponse<Map<String, Object>> searchUsers(RegisterRequest searchDto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            searchDto.setClientId(currentUser.getClient().getId());
            Map<String, Object> result = userDao.searchUsers(searchDto);
            return ApiResponse.success("Users retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search users: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    public ApiResponse<?> getUserDetail(RegisterRequest userDto) {
        try {
            if (userDto.getId() == null) {
                throw new ValidationException("User ID is required", HttpStatus.BAD_REQUEST);
            }
            
            UserMaster user = userRepository.findById(userDto.getId())
                .orElseThrow(() -> new ValidationException("User not found", HttpStatus.NOT_FOUND));
            
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if (user.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to view this user", HttpStatus.FORBIDDEN);
            }

            Map<String, Object> result = userDao.getUserDetail(userDto.getId());
            return ApiResponse.success("User detail retrieved successfully", result);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to get user detail: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    public ApiResponse<?> getAllUsers() {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            List<Map<String, Object>> users = userDao.getAllUsers(currentUser.getClient().getId());
            return ApiResponse.success("Users retrieved successfully", users);
        } catch (Exception e) {
            throw new ValidationException("Failed to get users: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    @Transactional
    public ApiResponse<?> deleteUser(Long id) {
        try {
            UserMaster user = userRepository.findById(id)
                .orElseThrow(() -> new ValidationException("User not found", HttpStatus.NOT_FOUND));
            
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if (user.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to delete this user", HttpStatus.FORBIDDEN);
            }
            
            // Soft delete by changing status
            user.setStatus("D");
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);
            
            return ApiResponse.success("User deleted successfully", null);
            
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete user: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
    
    private void validateUserDto(RegisterRequest userDto, boolean isCreate) {
        if (!StringUtils.hasText(userDto.getFirstName())) {
            throw new ValidationException("First name is required", HttpStatus.BAD_REQUEST);
        }
        
        if (StringUtils.hasText(userDto.getPhoneNumber()) && 
            !PHONE_PATTERN.matcher(userDto.getPhoneNumber()).matches()) {
            throw new ValidationException("Invalid phone number format", HttpStatus.BAD_REQUEST);
        }
        
        if (isCreate && !StringUtils.hasText(userDto.getPassword())) {
            throw new ValidationException("Password is required", HttpStatus.BAD_REQUEST);
        }
        
        if (StringUtils.hasText(userDto.getStatus()) && 
            !userDto.getStatus().matches("^[AID]$")) {
            throw new ValidationException("Status must be A (Active), I (Inactive), or D (Deleted)", HttpStatus.BAD_REQUEST);
        }
    }
    
    private void mapDtoToEntity(RegisterRequest dto, UserMaster entity) {
        entity.setFirstName(dto.getFirstName());
        entity.setLastName(dto.getLastName());
        entity.setPhoneNumber(dto.getPhoneNumber());
        entity.setEmail(dto.getEmail());
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : "A");
        entity.setRoles(dto.getRoles() != null ? dto.getRoles() : new ArrayList<>());
    }
    
    private RegisterRequest mapEntityToDto(UserMaster entity) {
        RegisterRequest dto = new RegisterRequest();
        dto.setId(entity.getId());
        dto.setFirstName(entity.getFirstName());
        dto.setLastName(entity.getLastName());
        dto.setPhoneNumber(entity.getPhoneNumber());
        dto.setEmail(entity.getEmail());
        dto.setStatus(entity.getStatus());
        dto.setRoles(entity.getRoles());
        dto.setClientId(entity.getClient() != null ? entity.getClient().getId() : null);
        // Don't include password in response
        return dto;
    }
} 