package com.inventory.controller;

import com.inventory.dto.TransportMasterDto;
import com.inventory.service.TransportMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/transports")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class TransportMasterController {
    private final TransportMasterService transportMasterService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody TransportMasterDto request) {
        return ResponseEntity.ok(transportMasterService.create(request));
    }

    // Update: id comes in request body, not URL
    @PutMapping
    public ResponseEntity<?> update(@RequestBody TransportMasterDto request) {
        return ResponseEntity.ok(transportMasterService.update(request));
    }

    // Delete: id passed in request body
    @DeleteMapping
    public ResponseEntity<?> delete(@RequestBody TransportMasterDto request) {
        return ResponseEntity.ok(transportMasterService.delete(request.getId()));
    }

    @PostMapping("/getTransports")
    public ResponseEntity<?> getTransports(@RequestBody TransportMasterDto request) {
        return ResponseEntity.ok(transportMasterService.getTransports(request));
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody TransportMasterDto request) {
        return ResponseEntity.ok(transportMasterService.searchTransports(request));
    }

    // Details by id passed in body
    @PostMapping("/details")
    public ResponseEntity<?> getDetails(@RequestBody TransportMasterDto request) {
        return ResponseEntity.ok(transportMasterService.getDetails(request.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetails(@PathVariable Long id) {
        return ResponseEntity.ok(transportMasterService.getDetails(id));
    }
}


