package com.inventory.controller;

import com.inventory.dto.BrandDto;
import com.inventory.service.BrandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brands")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class BrandController {
    private final BrandService brandService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BrandDto request) {
        return ResponseEntity.ok(brandService.create(request));
    }

    // Update: id comes in request body, not URL
    @PutMapping
    public ResponseEntity<?> update(@RequestBody BrandDto request) {
        return ResponseEntity.ok(brandService.update(request));
    }

    // Delete: id passed in request body
    @DeleteMapping
    public ResponseEntity<?> delete(@RequestBody BrandDto request) {
        return ResponseEntity.ok(brandService.delete(request.getId()));
    }

    @PostMapping("/getBrands")
    public ResponseEntity<?> getBrands(@RequestBody BrandDto request) {
        return ResponseEntity.ok(brandService.getBrands(request));
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody BrandDto request) {
        return ResponseEntity.ok(brandService.searchBrands(request));
    }

    // Details by id passed in body
    @PostMapping("/details")
    public ResponseEntity<?> getDetails(@RequestBody BrandDto request) {
        return ResponseEntity.ok(brandService.getDetails(request.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetails(@PathVariable Long id) {
        return ResponseEntity.ok(brandService.getDetails(id));
    }
}


