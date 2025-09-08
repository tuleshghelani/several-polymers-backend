package com.inventory.service;

import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.inventory.dao.ProfitDao;
import com.inventory.dto.ProfitRequestDto;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProfitService {
    private final ProfitDao profitDao;
    private static final long MAX_DATE_RANGE_DAYS = 62;
    private final UtilityService utilityService;
    
    public Map<String, Object> getDailyProfits(ProfitRequestDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            validateRequest(request);
            return profitDao.getDailyProfitSummary(
                request.getStartDate(),
                request.getEndDate().withHour(23).withMinute(59).withSecond(59),
                request.getClientId()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to get daily profits: " + e.getMessage());
        }
    }
    
    public Map<String, Object> getProductWiseProfits(ProfitRequestDto request) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            request.setClientId(currentUser.getClient().getId());
            validateRequest(request);
            return (Map<String, Object>) profitDao.getProductWiseProfitSummary(
                request.getStartDate(),
                request.getEndDate().withHour(23).withMinute(59).withSecond(59),
                request.getClientId()
            );
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to get product wise profits: " + e.getMessage());
        }
    }
    
    private void validateRequest(ProfitRequestDto request) {
        if (request == null) {
            throw new ValidationException("Request cannot be null");
        }
        if (request.getStartDate() == null) {
            throw new ValidationException("Start date is required");
        }
        if (request.getEndDate() == null) {
            throw new ValidationException("End date is required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("End date cannot be before start date");
        }
        
        // Calculate days between dates
        long daysBetween = ChronoUnit.DAYS.between(
            request.getStartDate().toLocalDate(), 
            request.getEndDate().toLocalDate()
        );
        
        if (daysBetween > MAX_DATE_RANGE_DAYS) {
            throw new ValidationException("Date range cannot exceed 62 days (2 months)");
        }
    }
} 