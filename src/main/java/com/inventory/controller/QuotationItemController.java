package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.request.QuotationItemProductionUpdateDto;
import com.inventory.dto.request.QuotationItemCreatedRollUpdateDto;
import com.inventory.dto.request.QuotationItemNumberOfRollUpdateDto;
import com.inventory.dto.request.QuotationItemRequestDto;
import com.inventory.dto.request.QuotationItemStatusUpdateDto;
import com.inventory.dto.request.QuotationItemQuantityUpdateDto;
import com.inventory.service.QuotationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quotation-items")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Slf4j
public class QuotationItemController {

    private final QuotationService quotationService;

    @PutMapping("/status")
    public ResponseEntity<ApiResponse<?>> updateStatus(@RequestBody QuotationItemStatusUpdateDto request) {
        log.debug("Update quotation item status for ID: {}", request.getId());
        return ResponseEntity.ok(quotationService.updateQuotationItemStatus(request));
    }

    @PutMapping("/production")
    public ResponseEntity<ApiResponse<?>> updateProduction(@RequestBody QuotationItemProductionUpdateDto request) {
        log.debug("Update quotation item production for ID: {}", request.getId());
        return ResponseEntity.ok(quotationService.updateQuotationItemProduction(request));
    }

    @PutMapping("/created-roll")
    public ResponseEntity<ApiResponse<?>> updateCreatedRoll(@RequestBody QuotationItemCreatedRollUpdateDto request) {
        log.debug("Update quotation item createdRoll for ID: {}", request.getId());
        return ResponseEntity.ok(quotationService.updateQuotationItemCreatedRoll(request));
    }

    @PutMapping("/number-of-roll")
    public ResponseEntity<ApiResponse<?>> updateNumberOfRoll(@RequestBody QuotationItemNumberOfRollUpdateDto request) {
        log.debug("Update quotation item numberOfRoll for ID: {}", request.getId());
        return ResponseEntity.ok(quotationService.updateQuotationItemNumberOfRoll(request));
    }

    @PostMapping("/search")
    public ResponseEntity<ApiResponse<?>> search(@RequestBody QuotationItemRequestDto request) {
        return ResponseEntity.ok(quotationService.searchQuotationItems(request));
    }

    @PutMapping("/quantity")
    public ResponseEntity<ApiResponse<?>> updateQuantity(@RequestBody QuotationItemQuantityUpdateDto request) {
        log.debug("Update quotation item quantity for ID: {}", request.getId());
        return ResponseEntity.ok(quotationService.updateQuotationItemQuantity(request));
    }
}


