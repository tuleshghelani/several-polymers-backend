package com.inventory.controller;

import com.inventory.dto.FollowUpDto;
import com.inventory.service.FollowUpService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/follow-ups")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class FollowUpController {
    private final FollowUpService followUpService;
    
    @PostMapping("/by-enquiry")
    public ResponseEntity<?> getFollowUpsByEnquiryIdPost(@RequestBody FollowUpDto request) {
        return ResponseEntity.ok(followUpService.getFollowUpsByEnquiryId(request.getEnquiryId()));
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody FollowUpDto request) {
        return ResponseEntity.ok(followUpService.create(request));
    }

    // Update: id comes in request body, not URL
    @PutMapping("/update")
    public ResponseEntity<?> update(@RequestBody FollowUpDto request) {
        return ResponseEntity.ok(followUpService.update(request));
    }

    // Delete: id passed in request body
    @PostMapping("/delete")
    public ResponseEntity<?> delete(@RequestBody FollowUpDto request) {
        return ResponseEntity.ok(followUpService.delete(request.getId()));
    }

    @PostMapping("/getFollowUps")
    public ResponseEntity<?> getFollowUps(@RequestBody FollowUpDto request) {
        return ResponseEntity.ok(followUpService.getFollowUps(request));
    }

    @PostMapping("/search")
    public ResponseEntity<?> search(@RequestBody FollowUpDto request) {
        return ResponseEntity.ok(followUpService.searchFollowUps(request));
    }

    // Details by id passed in body
    @PostMapping("/details")
    public ResponseEntity<?> getDetails(@RequestBody FollowUpDto request) {
        return ResponseEntity.ok(followUpService.getDetails(request.getId()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDetails(@PathVariable Long id) {
        return ResponseEntity.ok(followUpService.getDetails(id));
    }

    // Update: id comes in path variable
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody FollowUpDto request) {
        request.setId(id);
        return ResponseEntity.ok(followUpService.update(request));
    }

    // Delete: id passed in path variable
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        return ResponseEntity.ok(followUpService.delete(id));
    }
}
