package com.inventory.controller;

import com.inventory.dto.PowderCoatingReturnDto;
import com.inventory.service.PowderCoatingReturnService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/powder-coating-returns")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class PowderCoatingReturnController {
    private final PowderCoatingReturnService returnService;

    @PostMapping("/search")
    public ResponseEntity<?> searchReturns(@RequestBody PowderCoatingReturnDto request) {
        return ResponseEntity.ok(returnService.searchReturns(request));
    }
    
    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody PowderCoatingReturnDto request) {
        return ResponseEntity.ok(returnService.delete(request.getId()));
    }
    
    @PostMapping("/update")
    public ResponseEntity<?> update(@RequestBody PowderCoatingReturnDto request) {
        return ResponseEntity.ok(returnService.update(request.getId(), request));
    }

    @PostMapping("/getByProcessId")
    public ResponseEntity<?> getByProcessId(@RequestBody PowderCoatingReturnDto request) {
        return ResponseEntity.ok(returnService.getByProcessId(request.getProcessId()));
    }
} 