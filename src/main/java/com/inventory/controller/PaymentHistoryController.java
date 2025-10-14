package com.inventory.controller;

import com.inventory.dto.PaymentHistoryDto;
import com.inventory.service.PaymentHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment-history")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class PaymentHistoryController {
    private final PaymentHistoryService paymentHistoryService;

    @PostMapping("/save")
    public ResponseEntity<?> create(@RequestBody PaymentHistoryDto request) {
        return ResponseEntity.ok(paymentHistoryService.create(request));
    }

    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody PaymentHistoryDto request) {
        return ResponseEntity.ok(paymentHistoryService.update(request));
    }

    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody PaymentHistoryDto request) {
        return ResponseEntity.ok(paymentHistoryService.delete(request));
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchPaymentHistories(@RequestBody PaymentHistoryDto request) {
        return ResponseEntity.ok(paymentHistoryService.searchPaymentHistories(request));
    }

    @PostMapping("/get-detail")
    public ResponseEntity<?> getPaymentHistory(@RequestBody PaymentHistoryDto request) {
        return ResponseEntity.ok(paymentHistoryService.getPaymentHistory(request));
    }
}