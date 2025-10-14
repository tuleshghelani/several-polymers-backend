package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.PaymentHistoryDto;
import com.inventory.entity.Customer;
import com.inventory.entity.PaymentHistory;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.PaymentHistoryRepository;
import com.inventory.dao.PaymentHistoryDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PaymentHistoryService {
    private final PaymentHistoryRepository paymentHistoryRepository;
    private final CustomerRepository customerRepository;
    private final PaymentHistoryDao paymentHistoryDao;
    private final UtilityService utilityService;
    private final CustomerRemainingPaymentAmountService customerRemainingPaymentAmountService;

    @Transactional
    public ApiResponse<?> create(PaymentHistoryDto dto) {
        try {
            validatePaymentHistory(dto);
            
            // Check if customer exists
            Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found"));
            
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(customer.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to create payment history for this customer");
            }

            PaymentHistory paymentHistory = new PaymentHistory();
            mapDtoToEntity(dto, paymentHistory);
            
            paymentHistory.setCustomer(customer);
            paymentHistory.setCreatedBy(currentUser);
            paymentHistory.setUpdatedBy(currentUser);
            paymentHistory.setClient(currentUser.getClient());
            
            paymentHistory = paymentHistoryRepository.save(paymentHistory);
            
            // Update customer's remaining payment amount using the dedicated service
            updateCustomerRemainingAmount(customer.getId(), paymentHistory.getAmount(), paymentHistory.getIsReceived());
            
            return ApiResponse.success("Payment history created successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create payment history: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> update(PaymentHistoryDto dto) {
        try {
            if (dto.getId() == null) {
                throw new ValidationException("Payment history ID is required");
            }
            
            validatePaymentHistory(dto);
            
            PaymentHistory paymentHistory = paymentHistoryRepository.findById(dto.getId())
                .orElseThrow(() -> new ValidationException("Payment history not found"));

            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(paymentHistory.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to update this payment history");
            }
            
            // Store previous values for adjustment
            BigDecimal previousAmount = paymentHistory.getAmount();
            Boolean previousIsReceived = paymentHistory.getIsReceived();
            Long previousCustomerId = paymentHistory.getCustomer().getId();
            
            // Check if customer exists
            Customer customer = customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found"));
                
            if(customer.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to assign payment history to this customer");
            }

            mapDtoToEntity(dto, paymentHistory);
            
            paymentHistory.setCustomer(customer);
            paymentHistory.setUpdatedBy(currentUser);
            paymentHistory.setUpdatedAt(OffsetDateTime.now());
            
            paymentHistory = paymentHistoryRepository.save(paymentHistory);
            
            // Adjust customer's remaining payment amount using the dedicated service
            // First revert the previous transaction
            if (Boolean.TRUE.equals(previousIsReceived)) {
                customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                    previousCustomerId, previousAmount, false, true);
            } else {
                customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                    previousCustomerId, previousAmount, true, false);
            }
            
            // Then apply the new transaction
            updateCustomerRemainingAmount(customer.getId(), paymentHistory.getAmount(), paymentHistory.getIsReceived());
            
            return ApiResponse.success("Payment history updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update payment history: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> delete(PaymentHistoryDto dto) {
        try {
            if (dto.getId() == null) {
                throw new ValidationException("Payment history ID is required");
            }
            
            PaymentHistory paymentHistory = paymentHistoryRepository.findById(dto.getId())
                .orElseThrow(() -> new ValidationException("Payment history not found"));
                
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(paymentHistory.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to delete this payment history");
            }
            
            // Revert the payment effect on customer using the dedicated service
            if (Boolean.TRUE.equals(paymentHistory.getIsReceived())) {
                customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                    paymentHistory.getCustomer().getId(), paymentHistory.getAmount(), true, false);
            } else {
                customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(
                    paymentHistory.getCustomer().getId(), paymentHistory.getAmount(), false, true);
            }
            
            paymentHistoryRepository.delete(paymentHistory);
            return ApiResponse.success("Payment history deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete payment history: " + e.getMessage());
        }
    }

    public ApiResponse<Map<String, Object>> searchPaymentHistories(PaymentHistoryDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Map<String, Object> result = paymentHistoryDao.searchPaymentHistories(dto);
            return ApiResponse.success("Payment histories retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search payment histories: " + e.getMessage());
        }
    }

    public ApiResponse<?> getPaymentHistory(PaymentHistoryDto dto) {
        try {
            if (dto.getId() == null) {
                throw new ValidationException("Payment history ID is required");
            }
            
            Optional<PaymentHistory> paymentHistoryOpt = paymentHistoryRepository.findByIdAndClientId(
                dto.getId(), utilityService.getCurrentLoggedInUser().getClient().getId());
                
            if (paymentHistoryOpt.isEmpty()) {
                throw new ValidationException("Payment history not found");
            }
            
            PaymentHistory paymentHistory = paymentHistoryOpt.get();
            return ApiResponse.success("Payment history retrieved successfully", mapEntityToDto(paymentHistory));
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve payment history: " + e.getMessage());
        }
    }

    private void validatePaymentHistory(PaymentHistoryDto dto) {
        if (dto.getAmount() == null || dto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount is required and must be greater than zero");
        }
        
        if (dto.getCustomerId() == null) {
            throw new ValidationException("Customer ID is required");
        }
        
        if (!StringUtils.hasText(dto.getType())) {
            throw new ValidationException("Payment type is required");
        }
        
        if (!"C".equals(dto.getType()) && !"B".equals(dto.getType())) {
            throw new ValidationException("Invalid payment type. Must be 'C' (Cash) or 'B' (Bank)");
        }
    }

    private void mapDtoToEntity(PaymentHistoryDto dto, PaymentHistory paymentHistory) {
        paymentHistory.setAmount(dto.getAmount());
        paymentHistory.setType(dto.getType());
        paymentHistory.setRemarks(dto.getRemarks());
        paymentHistory.setIsReceived(dto.getIsReceived() != null ? dto.getIsReceived() : true);
        paymentHistory.setDate(dto.getDate() != null ? dto.getDate() : LocalDate.now());
    }

    private PaymentHistoryDto mapEntityToDto(PaymentHistory paymentHistory) {
        PaymentHistoryDto dto = new PaymentHistoryDto();
        dto.setId(paymentHistory.getId());
        dto.setAmount(paymentHistory.getAmount());
        dto.setCustomerId(paymentHistory.getCustomer().getId());
        dto.setCustomerName(paymentHistory.getCustomer().getName());
        dto.setType(paymentHistory.getType());
        dto.setRemarks(paymentHistory.getRemarks());
        dto.setIsReceived(paymentHistory.getIsReceived());
        dto.setDate(paymentHistory.getDate());
        dto.setCreatedAt(paymentHistory.getCreatedAt());
        dto.setUpdatedAt(paymentHistory.getUpdatedAt());
        
        if (paymentHistory.getCreatedBy() != null) {
            dto.setCreatedById(paymentHistory.getCreatedBy().getId());
            dto.setCreatedByName(paymentHistory.getCreatedBy().getFirstName() + " " + paymentHistory.getCreatedBy().getLastName());
        }
        
        if (paymentHistory.getUpdatedBy() != null) {
            dto.setUpdatedById(paymentHistory.getUpdatedBy().getId());
            dto.setUpdatedByName(paymentHistory.getUpdatedBy().getFirstName() + " " + paymentHistory.getUpdatedBy().getLastName());
        }
        
        return dto;
    }
    
    /**
     * Updates customer's remaining payment amount based on payment received or made
     * @param customerId The ID of the customer whose balance needs to be updated
     * @param amount The amount of payment
     * @param isReceived True if payment is received from customer, False if payment is made to customer
     */
    private void updateCustomerRemainingAmount(Long customerId, BigDecimal amount, Boolean isReceived) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        
        if (Boolean.TRUE.equals(isReceived)) {
            // Payment received from customer, this is like a "sale" where customer pays us
            customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(customerId, amount, false, true);
        } else {
            // Payment made to customer, this is like a "purchase" where we pay the customer
            customerRemainingPaymentAmountService.updateCustomerRemainingPaymentAmount(customerId, amount, true, false);
        }
    }
}