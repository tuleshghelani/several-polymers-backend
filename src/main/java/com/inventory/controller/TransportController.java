package com.inventory.controller;

import com.inventory.dto.TransportDto;
import com.inventory.dto.TransportPdfDto;
import com.inventory.service.TransportPdfGenerationService;
import com.inventory.service.TransportService;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transport")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class TransportController {
    private final TransportService transportService;
    private final TransportPdfGenerationService pdfGenerationService;
    
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody TransportDto request) {
        return ResponseEntity.ok(transportService.create(request));
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody TransportDto request) {
        return ResponseEntity.ok(transportService.create(request));
    }
    
    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody TransportDto request) {
        return ResponseEntity.ok(transportService.searchTransports(request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(transportService.delete(id));
    }
    
    @PostMapping("/detail")
    public ResponseEntity<?> getTransportDetail(@RequestBody TransportDto request) {
        return ResponseEntity.ok(transportService.getTransportDetail(request));
    } 

    @PostMapping("/generate-pdf")
    public ResponseEntity<byte[]> generatePdf(@RequestBody TransportPdfDto request) {
        byte[] pdfBytes = pdfGenerationService.generateTransportPdf(request);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "transport.pdf");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
} 