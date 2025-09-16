package com.inventory.controller;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.SaleDto;
import com.inventory.dto.SaleRequestDto;
import com.inventory.service.SaleService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/sales")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class SaleController {
    private final SaleService saleService;
    
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<?>> createPurchase(@RequestBody SaleRequestDto request) {
        return ResponseEntity.ok(saleService.createSale(request));
    }
    
    @PostMapping("/searchSale")
    public ResponseEntity<?> searchPurchases(@RequestBody SaleDto searchParams) {
        return ResponseEntity.ok(saleService.searchSales(searchParams));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<?>> delete(@PathVariable Long id) {
        return ResponseEntity.ok(saleService.delete(id));
    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<ApiResponse<?>> update(
//            @PathVariable Long id,
//            @RequestBody PurchaseRequestDto request) {
//        return ResponseEntity.ok(purchaseService.update(id, request));
//    }
    
    @PostMapping("/detail")
    public ResponseEntity<?> getSaleDetail(@RequestBody SaleDto request) {
        return ResponseEntity.ok(saleService.getSaleDetail(request));
    }

    @PostMapping("/createFromQuotationItems")
    public ResponseEntity<ApiResponse<?>> createFromQuotationItems(@RequestBody SaleDto request) {
        return ResponseEntity.ok(saleService.createSaleFromQuotationItems(request));
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<byte[]> generateSalePdf(@RequestBody SaleDto request) {
        byte[] pdfBytes = saleService.generateSalePdf(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "sale-invoice.pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}