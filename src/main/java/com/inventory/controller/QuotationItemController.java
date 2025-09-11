package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.request.QuotationItemProductionUpdateDto;
import com.inventory.dto.request.QuotationItemStatusUpdateDto;
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
}


