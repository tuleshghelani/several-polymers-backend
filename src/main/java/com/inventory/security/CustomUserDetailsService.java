package com.inventory.security;

import com.inventory.entity.UserMaster;
import com.inventory.repository.UserRepository;
import com.inventory.exception.ValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // Try phoneNumber first, then email
        UserMaster userMaster = null;        
        userMaster = userRepository.findByPhoneNumber(identifier).orElse(null);
        
        // If not found by phoneNumber, try email
        if (userMaster == null) {
            userMaster = userRepository.findByEmail(identifier).orElse(null);
        }
        
        if (userMaster == null) {
            throw new ValidationException("User not found", HttpStatus.UNAUTHORIZED);
        }

        return UserPrincipal.create(userMaster);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long id) {
        UserMaster userMaster = userRepository.findById(id)
                .orElseThrow(() -> new ValidationException("User not found", HttpStatus.UNAUTHORIZED));

        return UserPrincipal.create(userMaster);
    }
}