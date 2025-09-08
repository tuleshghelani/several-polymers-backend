package com.inventory.controller;

import com.inventory.dto.PowderCoatingProcessDto;
import com.inventory.dto.PowderCoatingProcessPdfDto;
import com.inventory.service.PowderCoatingProcessService;
import com.inventory.service.PdfGenerationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/powder-coating")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class PowderCoatingProcessController {
    private final PowderCoatingProcessService processService;
    private final PdfGenerationService pdfGenerationService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PowderCoatingProcessDto request) {
        return ResponseEntity.ok(processService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody PowderCoatingProcessDto request) {
        return ResponseEntity.ok(processService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(processService.delete(id));
    }

    @PostMapping("/search")
    public ResponseEntity<?> searchProcesses(@RequestBody PowderCoatingProcessDto request) {
        return ResponseEntity.ok(processService.searchProcesses(request));
    }

    @PostMapping("/return")
    public ResponseEntity<?> returnQuantity(@RequestBody PowderCoatingProcessDto request) {
        return ResponseEntity.ok(processService.returnQuantity(
            request.getId(), 
            request.getReturnQuantity(),
            request.getReturnDate()
        ));
    }

    @PostMapping("/getProcess")
    public ResponseEntity<?> getProcess(@RequestBody PowderCoatingProcessDto request) {
        return ResponseEntity.ok(processService.getProcess(request.getId()));
    }

    @PostMapping("/generate-pdf")
    public ResponseEntity<byte[]> generatePdf(@RequestBody PowderCoatingProcessPdfDto request) {
        byte[] pdfBytes = pdfGenerationService.generateEstimatePdf(request);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("filename", "estimate.pdf");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
} 