package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.PurchaseDto;
import com.inventory.dto.PurchaseRequestDto;
import com.inventory.service.PurchaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/purchases")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class PurchaseController {
    private final PurchaseService purchaseService;
    
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<?>> createPurchase(@RequestBody PurchaseRequestDto request) {
        return ResponseEntity.ok(purchaseService.createPurchase(request));
    }
    
    @PostMapping("/searchPurchase")
    public ResponseEntity<?> searchPurchases(@RequestBody PurchaseDto searchParams) {
        return ResponseEntity.ok(purchaseService.searchPurchases(searchParams));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(purchaseService.delete(id));
    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<?>> update(
//            @PathVariable Long id,
//            @RequestBody PurchaseRequestDto request) {
//        return ResponseEntity.ok(purchaseService.update(id, request));
//    }
    
    @PostMapping("/detail")
    public ResponseEntity<?> getPurchaseDetail(@RequestBody PurchaseDto request) {
        return ResponseEntity.ok(purchaseService.getPurchaseDetail(request));
    }
}