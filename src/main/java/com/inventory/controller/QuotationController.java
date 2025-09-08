package com.inventory.controller;


import com.inventory.dto.ApiResponse;
import com.inventory.dto.QuotationDto;
import com.inventory.dto.request.QuotationRequestDto;
import com.inventory.dto.request.QuotationStatusUpdateDto;
import com.inventory.service.QuotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@RestController
@RequestMapping("/api/quotations")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
@Slf4j
public class QuotationController {
    private final QuotationService quotationService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<?>> createQuotation(@RequestBody com.inventory.dto.request.QuotationRequestDto request) {
        return ResponseEntity.ok(quotationService.createQuotation(request));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponse<?>> updateQuotation(
            @RequestBody com.inventory.dto.request.QuotationRequestDto request) {
        return ResponseEntity.ok(quotationService.updateQuotation(request));
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchQuotations(@RequestBody QuotationDto searchParams) {
        log.debug("Received search quotation request: {}", searchParams);
        return ResponseEntity.ok(quotationService.searchQuotations(searchParams));
    }

    @PostMapping("/detail")
    public ResponseEntity<?> getQuotationDetail(@RequestBody QuotationDto request) {
        log.debug("Received quotation detail request for ID: {}", request.getId());
        return ResponseEntity.ok(quotationService.getQuotationDetail(request));
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<byte[]> generateQuotationPdf(@RequestBody QuotationDto request) {
        log.debug("Received quotation PDF generation request for ID: {}", request.getId());
        byte[] pdfBytes = quotationService.generateQuotationPdf(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "quotation.pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    /*@PostMapping("/generate-dispatch-slip")
    public ResponseEntity<byte[]> generateDispatchSlipPdf(@RequestBody QuotationDto request) {
        log.debug("Received dispatch slip PDF generation request for ID: {}", request.getId());
        byte[] pdfBytes = quotationService.generateDispatchSlipPdf(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "dispatch-slip.pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }*/

    @PutMapping("/update-status")
    public ResponseEntity<ApiResponse<?>> updateQuotationStatus(@RequestBody com.inventory.dto.request.QuotationStatusUpdateDto request) {
        log.debug("Received quotation status update request for ID: {}", request.getId());
        return ResponseEntity.ok(quotationService.updateQuotationStatus(request));
    }

    @PostMapping("/delete")
    public ResponseEntity<ApiResponse<?>> deleteQuotation(@RequestBody com.inventory.dto.request.QuotationRequestDto request) {
        log.debug("Received quotation delete request for ID: {}", request.getQuotationId());
        return ResponseEntity.ok(quotationService.deleteQuotation(request));
    }
}
