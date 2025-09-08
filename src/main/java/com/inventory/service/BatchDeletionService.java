package com.inventory.service;

import com.inventory.dao.PurchaseDao;
import com.inventory.dao.QuotationDao;
import com.inventory.dao.SaleDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.request.BatchDeleteRequestDto;
import com.inventory.entity.*;
import com.inventory.exception.ValidationException;
import com.inventory.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class BatchDeletionService {
    private final PurchaseRepository purchaseRepository;
    private final SaleRepository saleRepository;
    private final QuotationRepository quotationRepository;
    private final AttendanceRepository attendanceRepository;
    private final PurchaseItemRepository purchaseItemRepository;
    private final SaleItemRepository saleItemRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final UtilityService utilityService;
    private final ProductQuantityService productQuantityService;
    private final PurchaseDao purchaseDao;
    private final SaleDao saleDao;
    private final QuotationDao quotationDao;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> deleteRecords(BatchDeleteRequestDto request) {
        try {
            validateRequest(request);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Map<String, Integer> deletionCounts = new HashMap<>();

            for (String type : request.getType()) {
                switch (type.toUpperCase()) {
                    case "PURCHASE" -> deletionCounts.put("PURCHASE", deletePurchases(request, currentUser));
                    case "SALE" -> deletionCounts.put("SALE", deleteSales(request, currentUser));
                    case "QUOTATION" -> deletionCounts.put("QUOTATION", deleteQuotations(request, currentUser));
                    case "ATTENDANCE" -> deletionCounts.put("ATTENDANCE", deleteAttendances(request, currentUser));
                }
            }

            return ApiResponse.success("Records deleted successfully", deletionCounts);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("Error in batch deletion: ", e);
            throw new ValidationException("Failed to delete records: " + e.getMessage());
        }
    }

    private void validateRequest(BatchDeleteRequestDto request) {
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new ValidationException("Start date and end date are required");
        }
        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new ValidationException("End date cannot be before start date");
        }
        if (request.getType() == null || request.getType().isEmpty()) {
            throw new ValidationException("At least one type must be specified");
        }
    }

    private int deletePurchases(BatchDeleteRequestDto request, UserMaster currentUser) {
        List<Purchase> purchases = purchaseDao.findByClientIdAndDateRange(
                currentUser.getClient().getId(),
            request.getStartDate().atStartOfDay(),
            request.getEndDate().plusDays(1).atStartOfDay()
        );

        for (Purchase purchase : purchases) {
            purchaseItemRepository.deleteByPurchaseId(purchase.getId());
        }
        
        purchaseRepository.deleteAll(purchases);
        return purchases.size();
    }

    private int deleteSales(BatchDeleteRequestDto request, UserMaster currentUser) {
        List<Sale> sales = saleDao.findByClientIdAndDateRange(
            currentUser.getClient().getId(),
            request.getStartDate().atStartOfDay(),
            request.getEndDate().atStartOfDay()
        );

        for (Sale sale : sales) {
            saleItemRepository.deleteBySaleId(sale.getId());
        }
        
        saleRepository.deleteAll(sales);
        return sales.size();
    }

    private int deleteQuotations(BatchDeleteRequestDto request, UserMaster currentUser) {
        List<Quotation> quotations = quotationDao.findByClientIdAndDateRange(
            currentUser.getClient().getId(),
            request.getStartDate(),
            request.getEndDate()
        );

        for (Quotation quotation : quotations) {
            quotationItemRepository.deleteByQuotationId(quotation.getId());
        }
        
        quotationRepository.deleteAll(quotations);
        return quotations.size();
    }

    private int deleteAttendances(BatchDeleteRequestDto request, UserMaster currentUser) {
        List<Attendance> attendances = attendanceRepository.findByClientIdAndDateRange(
            currentUser.getClient().getId(),
            request.getStartDate().atStartOfDay().atOffset(ZoneOffset.UTC),
            request.getEndDate().atStartOfDay().atOffset(ZoneOffset.UTC)
        );
        
        attendanceRepository.deleteAll(attendances);
        return attendances.size();
    }
} 