package com.inventory.controller;

import com.inventory.dto.EmployeeWithdrawDto;
import com.inventory.service.EmployeeWithdrawService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee-withdraws")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class EmployeeWithdrawController {
    private final EmployeeWithdrawService service;

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody EmployeeWithdrawDto request) {
        return ResponseEntity.ok(service.create(request));
    }

    @PutMapping("/")
    public ResponseEntity<?> update(@RequestBody EmployeeWithdrawDto request) {
        return ResponseEntity.ok(service.update(request.getId(), request));
    }

    @DeleteMapping("/")
    public ResponseEntity<?> delete(@RequestBody EmployeeWithdrawDto request) {
        return ResponseEntity.ok(service.delete(request.getId()));
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody EmployeeWithdrawDto request) {
        return ResponseEntity.ok(service.search(request));
    }

    @PostMapping("/detail")
    public ResponseEntity<?> detail(@RequestBody EmployeeWithdrawDto request) {
        return ResponseEntity.ok(service.detail(request.getId()));
    }
}


