package com.inventory.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.inventory.dao.PriceDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.LastPriceRequestDto;
import com.inventory.exception.ValidationException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PriceService {
    private final PriceDao priceDao;
    
    public ApiResponse<?> getLastPrices(LastPriceRequestDto request) {
        try {
            validateRequest(request);
            Map<String, Object> result = priceDao.getLastPrices(request.getProductId(), request.getCustomerId());
            return ApiResponse.success("Last prices retrieved successfully", result);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to get last prices: " + e.getMessage());
        }
    }
    
    private void validateRequest(LastPriceRequestDto request) {
        if (request == null) {
            throw new ValidationException("Request cannot be null");
        }
        if (request.getProductId() == null) {
            throw new ValidationException("Product ID is required");
        }
        if (request.getCustomerId() == null) {
            throw new ValidationException("Please select Customer first");
        }
    }
} 