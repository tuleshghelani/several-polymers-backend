package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.PowderCoatingProcessDto;
import com.inventory.entity.PowderCoatingProcess;
import com.inventory.entity.PowderCoatingReturn;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.PowderCoatingProcessRepository;
import com.inventory.repository.PowderCoatingReturnRepository;
import com.inventory.repository.CustomerRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.dao.PowderCoatingProcessDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PowderCoatingProcessService {
    private final PowderCoatingProcessRepository processRepository;
    private final PowderCoatingReturnRepository returnRepository;
    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;
    private final PowderCoatingProcessDao processDao;
    private final UtilityService utilityService;

    @Transactional
    public ApiResponse<?> create(PowderCoatingProcessDto dto) {
        try {
            validateProcess(dto);
            
            PowderCoatingProcess process = new PowderCoatingProcess();
            process.setCustomer(customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found")));
            process.setProduct(productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ValidationException("Product not found")));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            process.setClient(currentUser.getClient());
            process.setCreatedBy(currentUser);

            process.setQuantity(dto.getQuantity());
            process.setRemainingQuantity(dto.getQuantity());
            process.setTotalBags(dto.getTotalBags());
            process.setUnitPrice(dto.getUnitPrice());
            process.setTotalAmount(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));
            process.setRemarks(dto.getRemarks());
            process.setStatus(dto.getStatus() != null ? dto.getStatus() : "A");
            
            processRepository.save(process);
            return ApiResponse.success("Process created successfully");
        } catch (Exception e) {
            throw new ValidationException("Failed to create process: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> update(Long id, PowderCoatingProcessDto dto) {
        try {
            validateProcess(dto);
            
            PowderCoatingProcess process = processRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Process not found"));
            
            process.setCustomer(customerRepository.findById(dto.getCustomerId())
                .orElseThrow(() -> new ValidationException("Customer not found")));
            process.setProduct(productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ValidationException("Product not found")));
            
            // Calculate total returned quantity
            Integer totalReturnedQuantity = returnRepository.findByProcessId(id)
                .stream()
                .map(PowderCoatingReturn::getReturnQuantity)
                .reduce(0, Integer::sum);
            
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(process.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to update this powder coating process");
            }
            
            process.setQuantity(dto.getQuantity());
            process.setRemainingQuantity(dto.getQuantity() - totalReturnedQuantity);
            process.setTotalBags(dto.getTotalBags());
            process.setUnitPrice(dto.getUnitPrice());
            process.setTotalAmount(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getQuantity())));
            process.setRemarks(dto.getRemarks());
            process.setUpdatedAt(OffsetDateTime.now());
            
            // Update status based on remaining quantity
            if (process.getRemainingQuantity() <= 0) {
                process.setStatus("C");
            } else {
                process.setStatus(dto.getStatus() != null ? dto.getStatus() : process.getStatus());
            }
            
            processRepository.save(process);
            return ApiResponse.success("Process updated successfully");
        } catch (Exception e) {
            throw new ValidationException("Failed to update process: " + e.getMessage());
        }
    }

    public ApiResponse<?> delete(Long id) {
        try {
            PowderCoatingProcess process = processRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Process not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(process.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to delete this powder coating process");
            }
            processRepository.delete(process);
            return ApiResponse.success("Process deleted successfully");
        } catch (Exception e) {
            throw new ValidationException("Failed to delete process: " + e.getMessage());
        }
    }

    public ApiResponse<Map<String, Object>> searchProcesses(PowderCoatingProcessDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Map<String, Object> result = processDao.searchProcesses(dto);
            return ApiResponse.success("Processes retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search processes: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> returnQuantity(Long processId, Integer returnQuantity, OffsetDateTime returnDate) {
        try {
            if (returnQuantity <= 0) {
                throw new ValidationException("Return quantity must be greater than 0");
            }

            PowderCoatingProcess process = processRepository.findById(processId)
                .orElseThrow(() -> new ValidationException("Process not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(process.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to return quantity for this process");
            }


            if (process.getRemainingQuantity() < returnQuantity) {
                throw new ValidationException("Return quantity cannot be greater than remaining quantity");
            }

            process.setRemainingQuantity(process.getRemainingQuantity() - returnQuantity);

            if (process.getRemainingQuantity() == 0) {
                process.setStatus("C");
            }
            
            process.setUpdatedAt(OffsetDateTime.now());
            processRepository.save(process);

            // Create return record with custom date if provided
            PowderCoatingReturn returnRecord = new PowderCoatingReturn();
            returnRecord.setProcess(process);
            returnRecord.setReturnQuantity(returnQuantity);
            returnRecord.setCreatedBy(utilityService.getCurrentLoggedInUser());
            if (returnDate != null) {
                returnRecord.setCreatedAt(returnDate);
            }
            returnRepository.save(returnRecord);

            return ApiResponse.success("Quantity returned successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to return quantity: " + e.getMessage());
        }
    }

    private void validateProcess(PowderCoatingProcessDto dto) {
        if (dto.getCustomerId() == null) {
            throw new ValidationException("Customer is required");
        }
        if (dto.getProductId() == null) {
            throw new ValidationException("Product is required");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new ValidationException("Valid quantity is required");
        }
        if (dto.getTotalBags() == null || dto.getTotalBags() <= 0) {
            throw new ValidationException("Total bags must be greater than 0");
        }
        if (dto.getUnitPrice() == null || dto.getUnitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Unit price must be greater than 0");
        }
        dto.setTotalAmount(dto.getUnitPrice().multiply(BigDecimal.valueOf(dto.getTotalBags())));
    }

    private PowderCoatingProcessDto mapToDto(PowderCoatingProcess process) {
        PowderCoatingProcessDto dto = new PowderCoatingProcessDto();
        dto.setId(process.getId());
        dto.setCustomerId(process.getCustomer().getId());
        dto.setProductId(process.getProduct().getId());
        dto.setQuantity(process.getQuantity());
        dto.setRemainingQuantity(process.getRemainingQuantity());
        dto.setStatus(process.getStatus());
        dto.setCustomerName(process.getCustomer().getName());
        dto.setProductName(process.getProduct().getName());
        dto.setCreatedAt(process.getCreatedAt());
        dto.setClientId(process.getClient().getId());
        return dto;
    }

    public ApiResponse<?> getProcess(Long id) {
        try {
            if (id == null) {
                throw new ValidationException("Process ID is required");
            }
            
            PowderCoatingProcess process = processRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Process not found"));
            
            Map<String, Object> processDetails = new HashMap<>();
            processDetails.put("id", process.getId());
            processDetails.put("customerId", process.getCustomer().getId());
            processDetails.put("productId", process.getProduct().getId());
            processDetails.put("quantity", process.getQuantity());
            processDetails.put("remainingQuantity", process.getRemainingQuantity());
            processDetails.put("status", process.getStatus());
            processDetails.put("unitPrice", process.getUnitPrice());
            processDetails.put("totalAmount", process.getTotalAmount());
            processDetails.put("totalBags", process.getTotalBags());
            processDetails.put("remarks", process.getRemarks());
            processDetails.put("createdAt", process.getCreatedAt());
            processDetails.put("customerName", process.getCustomer().getName());
            processDetails.put("productName", process.getProduct().getName());
            
            return ApiResponse.success("Process retrieved successfully", processDetails);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve process: " + e.getMessage());
        }
    }

    private void mapDtoToEntity(PowderCoatingProcessDto dto, PowderCoatingProcess process) {
        process.setTotalBags(dto.getTotalBags());
        process.setUnitPrice(dto.getUnitPrice());
        process.setTotalAmount(dto.getTotalAmount());
    }
} 