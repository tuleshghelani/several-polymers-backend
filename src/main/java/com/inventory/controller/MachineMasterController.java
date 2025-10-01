package com.inventory.controller;

import com.inventory.dto.MachineDto;
import com.inventory.service.MachineMasterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/machines")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class MachineMasterController {
    private final MachineMasterService machineMasterService;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody MachineDto request) {
        return ResponseEntity.ok(machineMasterService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody MachineDto request) {
        return ResponseEntity.ok(machineMasterService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(machineMasterService.delete(id));
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody MachineDto request) {
        return ResponseEntity.ok(machineMasterService.search(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> details(@PathVariable Long id) {
        return ResponseEntity.ok(machineMasterService.getDetails(id));
    }

    @GetMapping("/list")
    public ResponseEntity<?> getMachineList() {
        return ResponseEntity.ok(machineMasterService.getMachineList());
    }
}


