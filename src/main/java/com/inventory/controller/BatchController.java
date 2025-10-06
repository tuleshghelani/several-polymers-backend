package com.inventory.controller;

import com.inventory.dto.BachDto;
import com.inventory.service.BachService;
import com.inventory.dto.request.BachUpsertRequestDto;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/batchs")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class BatchController {
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

    @PostMapping("/export")
    public ResponseEntity<byte[]> export(@RequestBody BachDto request) {
        byte[] excel = bachService.exportBachMixerProductionExcel(request);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=bach-report.xlsx");
        return new ResponseEntity<>(excel, headers, HttpStatus.OK);
    }
}


