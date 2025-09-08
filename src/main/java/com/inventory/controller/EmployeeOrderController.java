package com.inventory.controller;

import com.inventory.dto.EmployeeOrderDto;
import com.inventory.service.EmployeeOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/employee-orders")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class EmployeeOrderController {
    private final EmployeeOrderService employeeOrderService;
    
    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody EmployeeOrderDto request) {
        return ResponseEntity.ok(employeeOrderService.create(request));
    }

    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody EmployeeOrderDto request) {
        return ResponseEntity.ok(employeeOrderService.update(request));
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(employeeOrderService.delete(id));
    }
    
    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody EmployeeOrderDto request) {
        return ResponseEntity.ok(employeeOrderService.searchEmployeeOrders(request));
    }
    
    @PostMapping("/detail")
    public ResponseEntity<?> getEmployeeOrderDetail(@RequestBody EmployeeOrderDto request) {
        return ResponseEntity.ok(employeeOrderService.getEmployeeOrderDetail(request));
    }
} 