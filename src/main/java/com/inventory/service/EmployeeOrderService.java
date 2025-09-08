package com.inventory.service;

import com.inventory.dto.ApiResponse;
import com.inventory.dto.EmployeeOrderDto;
import com.inventory.entity.EmployeeOrder;
import com.inventory.entity.Product;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.EmployeeOrderRepository;
import com.inventory.repository.EmployeeRepository;
import com.inventory.repository.ProductRepository;
import com.inventory.dao.EmployeeOrderDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeOrderService {
    private final EmployeeOrderRepository employeeOrderRepository;
    private final ProductRepository productRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeOrderDao employeeOrderDao;
    private final UtilityService utilityService;

    @Transactional
    public ApiResponse<?> create(EmployeeOrderDto dto) {
        try {
            validateEmployeeOrder(dto);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            EmployeeOrder order = new EmployeeOrder();
            mapDtoToEntity(dto, order);
            order.setClient(currentUser.getClient());
            order.setCreatedBy(currentUser);
            
            order = employeeOrderRepository.save(order);
            return ApiResponse.success("Employee order created successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to create employee order: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> update(EmployeeOrderDto dto) {
        try {
            if (dto.getId() == null) {
                throw new ValidationException("Order ID is required");
            }
            
            validateEmployeeOrder(dto);
            
            EmployeeOrder order = employeeOrderRepository.findById(dto.getId())
                .orElseThrow(() -> new ValidationException("Employee order not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(order.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to update this employee order");
            }
            order.setClient(currentUser.getClient());
            order.setCreatedBy(currentUser);
            mapDtoToEntity(dto, order);
            order.setUpdatedAt(OffsetDateTime.now());
            
            order = employeeOrderRepository.save(order);
            return ApiResponse.success("Employee order updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update employee order: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            EmployeeOrder order = employeeOrderRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Employee order not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(order.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to delete this employee order");
            }
            employeeOrderRepository.delete(order);
            return ApiResponse.success("Employee order deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete employee order: " + e.getMessage());
        }
    }

    public ApiResponse<?> searchEmployeeOrders(EmployeeOrderDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            validateSearchRequest(dto);
            Map<String, Object> result = employeeOrderDao.searchEmployeeOrders(dto);
            return ApiResponse.success("Employee orders retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to search employee orders: " + e.getMessage());
        }
    }

    public ApiResponse<?> getEmployeeOrderDetail(EmployeeOrderDto dto) {
        try {
            if (dto.getId() == null) {
                throw new ValidationException("Order ID is required");
            }
            
            EmployeeOrder order = employeeOrderRepository.findById(dto.getId())
                .orElseThrow(() -> new ValidationException("Employee order not found"));
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            if(order.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to view this employee order");
            }

            return ApiResponse.success("Employee order detail retrieved successfully", mapEntityToDto(order));
        } catch (Exception e) {
            throw new ValidationException("Failed to get employee order detail: " + e.getMessage());
        }
    }

    private void validateEmployeeOrder(EmployeeOrderDto dto) {
        if (dto.getProductId() == null) {
            throw new ValidationException("Product ID is required");
        }
        if (dto.getEmployeeIds() == null || dto.getEmployeeIds().isEmpty()) {
            throw new ValidationException("At least one employee must be assigned");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0) {
            throw new ValidationException("Valid quantity is required");
        }
        
        // Validate product exists
        productRepository.findById(dto.getProductId())
            .orElseThrow(() -> new ValidationException("Product not found"));
            
        // Validate all employees exist
        dto.getEmployeeIds().forEach(empId -> 
            employeeRepository.findById(empId)
                .orElseThrow(() -> new ValidationException("Employee not found with ID: " + empId))
        );
    }

    private void validateSearchRequest(EmployeeOrderDto dto) {
        if (dto.getCurrentPage() == null) {
            dto.setCurrentPage(0);
        }
        if (dto.getPerPageRecord() == null) {
            dto.setPerPageRecord(10);
        }
    }

    private void mapDtoToEntity(EmployeeOrderDto dto, EmployeeOrder order) {
        Product product = productRepository.findById(dto.getProductId())
            .orElseThrow(() -> new ValidationException("Product not found"));
            
        order.setProduct(product);
        order.setEmployeeIds(dto.getEmployeeIds());
        order.setQuantity(dto.getQuantity());
        order.setRemarks(dto.getRemarks());
        order.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "O");
    }

    private EmployeeOrderDto mapEntityToDto(EmployeeOrder order) {
        EmployeeOrderDto dto = new EmployeeOrderDto();
        dto.setId(order.getId());
        dto.setProductId(order.getProduct().getId());
        dto.setProductName(order.getProduct().getName());
        dto.setEmployeeIds(order.getEmployeeIds());
        dto.setQuantity(order.getQuantity());
        dto.setRemarks(order.getRemarks());
        dto.setStatus(order.getStatus());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());
        return dto;
    }
} 