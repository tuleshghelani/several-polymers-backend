package com.inventory.controller;

import com.inventory.dto.DailyProfitDto;
import com.inventory.service.DailyProfitService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/profits")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class DailyProfitController {
    private final DailyProfitService dailyProfitService;

    @PostMapping("/search")
    public ResponseEntity<?> searchDailyProfits(@RequestBody DailyProfitDto request) {
        return ResponseEntity.ok(dailyProfitService.searchDailyProfits(request));
    }
} 