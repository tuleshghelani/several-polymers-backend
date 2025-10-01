package com.inventory.controller;

import com.inventory.dto.BachDto;
import com.inventory.service.BachService;
import com.inventory.dto.request.BachUpsertRequestDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/batchs")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class BachController {
    private final BachService bachService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody BachDto request) {
        return ResponseEntity.ok(bachService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody BachDto request) {
        return ResponseEntity.ok(bachService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(bachService.delete(id));
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody BachDto request) {
        return ResponseEntity.ok(bachService.search(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> details(@PathVariable Long id) {
        return ResponseEntity.ok(bachService.getDetails(id));
    }

    @PostMapping("/full-details")
    public ResponseEntity<?> fullDetails(@RequestBody BachDto request) {
        return ResponseEntity.ok(bachService.getFullDetails(request));
    }

    @PostMapping("/upsert")
    public ResponseEntity<?> upsert(@RequestBody BachUpsertRequestDto request) {
        return ResponseEntity.ok(bachService.upsert(request));
    }
}


