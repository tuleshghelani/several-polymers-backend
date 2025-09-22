package com.inventory.controller;

import com.inventory.dto.MixerDto;
import com.inventory.service.MixerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/mixers")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class MixerController {
    private final MixerService mixerService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MixerDto request) {
        return ResponseEntity.ok(mixerService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody MixerDto request) {
        return ResponseEntity.ok(mixerService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(mixerService.delete(id));
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody MixerDto request) {
        return ResponseEntity.ok(mixerService.search(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> details(@PathVariable Long id) {
        return ResponseEntity.ok(mixerService.getDetails(id));
    }
}


