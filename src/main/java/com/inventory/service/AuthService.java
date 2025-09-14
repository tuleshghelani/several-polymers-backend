package com.inventory.service;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.inventory.dto.LoginRequest;
import com.inventory.dto.RegisterRequest;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.UserRepository;
import com.inventory.security.JwtTokenProvider;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public UserMaster register(RegisterRequest request) {
        // Use phoneNumber if provided, otherwise fallback to email
        String identifier = StringUtils.hasText(request.getPhoneNumber()) ? 
            request.getPhoneNumber() : request.getEmail();
        
        if (!StringUtils.hasText(identifier)) {
            throw new ValidationException("Phone number or email is required", HttpStatus.BAD_REQUEST);
        }
        
        // Check if user already exists by phoneNumber or email
        if (StringUtils.hasText(request.getPhoneNumber()) && 
            userRepository.existsByPhoneNumber(request.getPhoneNumber())) {
            throw new ValidationException("User with this phone number already exists", HttpStatus.BAD_REQUEST);
        }
        
        if (StringUtils.hasText(request.getEmail()) && 
            userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("User with this email already exists", HttpStatus.BAD_REQUEST);
        }

        UserMaster userMaster = new UserMaster();
        userMaster.setPhoneNumber(request.getPhoneNumber());
        userMaster.setEmail(request.getEmail());
        userMaster.setPassword(passwordEncoder.encode(request.getPassword()));
        userMaster.setFirstName(request.getFirstName());
        userMaster.setLastName(request.getLastName());
        userMaster.setStatus(request.getStatus() != null ? request.getStatus() : "A");
        userMaster.setRoles(request.getRoles() != null ? request.getRoles() : new java.util.ArrayList<>());

        return userRepository.save(userMaster);
    }

    public Map<String, Object> login(LoginRequest request) throws ValidationException {
        try {
            // Use phoneNumber if provided, otherwise fallback to email
            String identifier = StringUtils.hasText(request.getPhoneNumber()) ? 
                request.getPhoneNumber() : request.getEmail();
            
            if (!StringUtils.hasText(identifier)) {
                throw new ValidationException("Phone number or email is required", HttpStatus.BAD_REQUEST);
            }
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(identifier, request.getPassword())
            );

            String token = tokenProvider.generateToken(authentication);
            String refreshToken = UUID.randomUUID().toString();

            // Find user by phoneNumber first, then by email
            UserMaster user = null;
            if (StringUtils.hasText(request.getPhoneNumber())) {
                user = userRepository.findByPhoneNumber(request.getPhoneNumber()).orElse(null);
            }
            if (user == null && StringUtils.hasText(request.getEmail())) {
                user = userRepository.findByEmail(request.getEmail()).orElse(null);
            }
            
            if (user == null) {
                throw new ValidationException("Invalid credentials", HttpStatus.UNAUTHORIZED);
            }

            if(!Objects.equals("A", user.getStatus())) {
                throw new ValidationException("User not active", HttpStatus.UNAUTHORIZED);
            }
            
            user.setJwtToken(token);
            user.setRefreshToken(refreshToken);
            user.setRefreshTokenExpiry(OffsetDateTime.now().plusDays(7)); // 7 days expiry
            user.setUpdatedAt(OffsetDateTime.now());
            userRepository.save(user);

            Map<String, Object> tokens = new HashMap<>();
            tokens.put("accessToken", token);
            tokens.put("refreshToken", refreshToken);
            tokens.put("firstName", user.getFirstName());
            tokens.put("lastName", user.getLastName());
            tokens.put("phoneNumber", user.getPhoneNumber());
            tokens.put("email", user.getEmail());
            tokens.put("id", user.getId().toString());
            tokens.put("role", user.getRoles());
            
            return tokens;
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new ValidationException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }
    }
}
