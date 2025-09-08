package com.inventory.controller;

import com.inventory.dao.ProfitDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.ProfitRequestDto;
import com.inventory.exception.ValidationException;
import com.inventory.service.ProfitService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profits")
@CrossOrigin(origins = "*", allowedHeaders = "*")
@RequiredArgsConstructor
public class ProfitController {
    private final ProfitService profitService;
    private final Logger logger = LoggerFactory.getLogger(ProfitController.class);

    @PostMapping("/daily")
    public ResponseEntity<ApiResponse<?>> getDailyProfits(@RequestBody ProfitRequestDto request) {
        try {
            logger.info("Received daily profits request for dates: {} to {}", request.getStartDate(), request.getEndDate());
            Map<String, Object> result = profitService.getDailyProfits(request);
            return ResponseEntity.ok(ApiResponse.success("Daily profits retrieved successfully", result));
        } catch (ValidationException ve) {
            logger.error("Validation error in getDailyProfits: {}", ve.getMessage());
            return ResponseEntity.badRequest().body(ApiResponse.error(ve.getMessage()));
        } catch (Exception e) {
            logger.error("Error in getDailyProfits: ", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("An unexpected error occurred. Please try again later."));
        }
    }

    @PostMapping("/products")
    public ResponseEntity<?> getProductWiseProfits(@RequestBody ProfitRequestDto request) {
        return ResponseEntity.ok(profitService.getProductWiseProfits(request));
    }
}
