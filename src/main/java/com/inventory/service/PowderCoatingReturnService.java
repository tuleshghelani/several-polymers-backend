package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.PowderCoatingReturnDto;
import com.inventory.dao.PowderCoatingReturnDao;
import com.inventory.entity.PowderCoatingProcess;
import com.inventory.entity.PowderCoatingReturn;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.PowderCoatingReturnRepository;
import com.inventory.repository.PowderCoatingProcessRepository;
import com.inventory.service.UtilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.Map;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PowderCoatingReturnService {
    private final PowderCoatingReturnDao returnDao;
    private final PowderCoatingReturnRepository returnRepository;
    private final PowderCoatingProcessRepository processRepository;
    private final UtilityService utilityService;

    public ApiResponse<Map<String, Object>> searchReturns(PowderCoatingReturnDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            validateSearchRequest(dto);
            Map<String, Object> result = returnDao.searchReturns(dto);
            return ApiResponse.success("Return history retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search return history: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            if (id == null) {
                throw new ValidationException("ID is required for deletion");
            }

            PowderCoatingReturn returnRecord = returnRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Return record not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(returnRecord.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to delete this return record");
            }
            
            // Restore the remaining quantity
            PowderCoatingProcess process = returnRecord.getProcess();
            process.setRemainingQuantity(process.getRemainingQuantity() + returnRecord.getReturnQuantity());
            processRepository.save(process);
            
            returnRepository.delete(returnRecord);
            return ApiResponse.success("Return record deleted successfully");
        } catch (Exception e) {
            throw new ValidationException("Failed to delete return record: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> update(Long id, PowderCoatingReturnDto dto) {
        try {
            validateUpdateRequest(id, dto);
            
            PowderCoatingReturn returnRecord = returnRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Return record not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(returnRecord.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to update this return record");
            }

            PowderCoatingProcess process = returnRecord.getProcess();

            process.setRemainingQuantity(process.getRemainingQuantity() + returnRecord.getReturnQuantity());

            if (process.getRemainingQuantity() == 0) {
                process.setStatus("C");
            } else {
                process.setStatus("A");
            }
            if (process.getRemainingQuantity() < dto.getReturnQuantity()) {
                throw new ValidationException("Return quantity cannot be greater than remaining quantity");
            }
            
            process.setRemainingQuantity(process.getRemainingQuantity() - dto.getReturnQuantity());
            processRepository.save(process);
            
            returnRecord.setReturnQuantity(dto.getReturnQuantity());
            returnRepository.save(returnRecord);
            
            return ApiResponse.success("Return record updated successfully");
        } catch (Exception e) {
            throw new ValidationException("Failed to update return record: " + e.getMessage());
        }
    }

    private void validateSearchRequest(PowderCoatingReturnDto dto) {
        if (dto == null) {
            throw new ValidationException("Request cannot be null");
        }
        if (dto.getCurrentPage() == null) {
            dto.setCurrentPage(0);
        }
        if (dto.getPerPageRecord() == null) {
            dto.setPerPageRecord(10);
        }
    }

    private void validateUpdateRequest(Long id, PowderCoatingReturnDto dto) {
        if (dto == null) {
            throw new ValidationException("Request cannot be null");
        }
        if (id == null) {
            throw new ValidationException("ID is required for update");
        }
        if (dto.getReturnQuantity() == null || dto.getReturnQuantity() <= 0) {
            throw new ValidationException("Valid return quantity is required");
        }
    }

    public ApiResponse<?> getByProcessId(Long processId) {
        try {
            if (processId == null) {
                throw new ValidationException("Process ID is required");
            }
            PowderCoatingProcess process = processRepository.findById(processId)
                .orElseThrow(() -> new ValidationException("Process not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(process.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to delete this return record");
            }

            List<Map<String, Object>> returns = returnRepository.findByProcessId(processId)
                .stream()
                .map(returnRecord -> {
                    Map<String, Object> returnMap = new HashMap<>();
                    returnMap.put("id", returnRecord.getId());
                    returnMap.put("returnQuantity", returnRecord.getReturnQuantity());
                    returnMap.put("createdAt", returnRecord.getCreatedAt());
                    return returnMap;
                })
                .collect(Collectors.toList());

            return ApiResponse.success("Return records retrieved successfully", returns);
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to retrieve return records: " + e.getMessage());
        }
    }
} 