package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.LastPriceRequestDto;
import com.inventory.service.PriceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/price")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class PriceController {
    private final PriceService priceService;
    
    @PostMapping("/latest")
    public ResponseEntity<ApiResponse<?>> getLastPrices(@RequestBody LastPriceRequestDto request) {
        return ResponseEntity.ok(priceService.getLastPrices(request));
    }
} 