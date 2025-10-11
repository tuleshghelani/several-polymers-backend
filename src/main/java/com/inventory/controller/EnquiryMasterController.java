package com.inventory.controller;

import com.inventory.dto.EnquiryMasterDto;
import com.inventory.service.EnquiryMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/enquiries")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class EnquiryMasterController {
    private final EnquiryMasterService enquiryMasterService;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody EnquiryMasterDto request) {
        return ResponseEntity.ok(enquiryMasterService.create(request));
    }

    // Update: id comes in request body, not URL
    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody EnquiryMasterDto request) {
        return ResponseEntity.ok(enquiryMasterService.update(request));
    }

    // Delete: id passed in request body
    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody EnquiryMasterDto request) {
        return ResponseEntity.ok(enquiryMasterService.delete(request.getId()));
    }

    @PostMapping("/getEnquiries")
    public ResponseEntity<?> getEnquiries(@RequestBody EnquiryMasterDto request) {
        return ResponseEntity.ok(enquiryMasterService.getEnquiries(request));
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody EnquiryMasterDto request) {
        return ResponseEntity.ok(enquiryMasterService.searchEnquiries(request));
    }

    // Details by id passed in body
    @PostMapping("/details")
    public ResponseEntity<?> getDetails(@RequestBody EnquiryMasterDto request) {
        return ResponseEntity.ok(enquiryMasterService.getDetails(request.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetails(@PathVariable Long id) {
        return ResponseEntity.ok(enquiryMasterService.getDetails(id));
    }

    // Update: id comes in path variable
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody EnquiryMasterDto request) {
        request.setId(id);
        return ResponseEntity.ok(enquiryMasterService.update(request));
    }

    // Delete: id passed in path variable
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(enquiryMasterService.delete(id));
    }
}
