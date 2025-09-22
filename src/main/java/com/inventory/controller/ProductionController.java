package com.inventory.controller;

import com.inventory.dto.ProductionDto;
import com.inventory.service.ProductionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/productions")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class ProductionController {
    private final ProductionService productionService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProductionDto request) {
        return ResponseEntity.ok(productionService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody ProductionDto request) {
        return ResponseEntity.ok(productionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(productionService.delete(id));
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody ProductionDto request) {
        return ResponseEntity.ok(productionService.search(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> details(@PathVariable Long id) {
        return ResponseEntity.ok(productionService.getDetails(id));
    }
}


