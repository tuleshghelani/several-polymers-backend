package com.inventory.service;

import com.inventory.dao.EmployeeWithdrawDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.EmployeeWithdrawDto;
import com.inventory.entity.Employee;
import com.inventory.entity.EmployeeWithdraw;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.EmployeeRepository;
import com.inventory.repository.EmployeeWithdrawRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EmployeeWithdrawService {
    private final EmployeeWithdrawRepository employeeWithdrawRepository;
    private final EmployeeRepository employeeRepository;
    private final EmployeeWithdrawDao employeeWithdrawDao;
    private final UtilityService utilityService;

    @Transactional
    public ApiResponse<?> create(EmployeeWithdrawDto dto) {
        try {
            validate(dto);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Employee employee = employeeRepository.findById(dto.getEmployeeId())
                .orElseThrow(() -> new ValidationException("Employee not found"));
            if (employee.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to create for this employee");
            }

            EmployeeWithdraw entity = new EmployeeWithdraw();
            entity.setEmployee(employee);
            entity.setPayment(dto.getPayment());
            entity.setWithdrawDate(dto.getWithdrawDate() != null ? dto.getWithdrawDate() : LocalDate.now());
            entity.setRemarks(dto.getRemarks());
            entity.setCreatedBy(currentUser);
            entity.setClient(currentUser.getClient());

            employeeWithdrawRepository.save(entity);
            return ApiResponse.success("Employee withdraw created successfully");
        } catch (ValidationException e) {
            e.printStackTrace();
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to create withdraw: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> update(Long id, EmployeeWithdrawDto dto) {
        try {
            validate(dto);
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            EmployeeWithdraw entity = employeeWithdrawRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Withdraw not found"));
            if (entity.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to update this withdraw");
            }

            if (dto.getEmployeeId() != null && !dto.getEmployeeId().equals(entity.getEmployee().getId())) {
                Employee employee = employeeRepository.findById(dto.getEmployeeId())
                    .orElseThrow(() -> new ValidationException("Employee not found"));
                if (employee.getClient().getId() != currentUser.getClient().getId()) {
                    throw new ValidationException("You are not authorized to use this employee");
                }
                entity.setEmployee(employee);
            }

            if (dto.getPayment() != null) {
                if (dto.getPayment().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new ValidationException("Payment must be greater than 0");
                }
                entity.setPayment(dto.getPayment());
            }
            if (dto.getWithdrawDate() != null) {
                entity.setWithdrawDate(dto.getWithdrawDate());
            }
            if (dto.getRemarks() != null) {
                entity.setRemarks(dto.getRemarks());
            }

            employeeWithdrawRepository.save(entity);
            return ApiResponse.success("Employee withdraw updated successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to update withdraw: " + e.getMessage());
        }
    }

    @Transactional
    public ApiResponse<?> delete(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            EmployeeWithdraw entity = employeeWithdrawRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Withdraw not found"));
            if (entity.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to delete this withdraw");
            }
            employeeWithdrawRepository.delete(entity);
            return ApiResponse.success("Employee withdraw deleted successfully");
        } catch (ValidationException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationException("Failed to delete withdraw: " + e.getMessage());
        }
    }

    public ApiResponse<?> detail(Long id) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            EmployeeWithdraw entity = employeeWithdrawRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Withdraw not found"));
            if (entity.getClient().getId() != currentUser.getClient().getId()) {
                throw new ValidationException("You are not authorized to view this withdraw");
            }
            EmployeeWithdrawDto dto = new EmployeeWithdrawDto();
            dto.setId(entity.getId());
            dto.setEmployeeId(entity.getEmployee().getId());
            dto.setEmployeeName(entity.getEmployee().getName());
            dto.setPayment(entity.getPayment());
            dto.setWithdrawDate(entity.getWithdrawDate());
            dto.setRemarks(entity.getRemarks());
            return ApiResponse.success("Withdraw detail fetched", dto);
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch detail: " + e.getMessage());
        }
    }

    public ApiResponse<?> search(EmployeeWithdrawDto dto) {
        try {
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            dto.setClientId(currentUser.getClient().getId());
            Page<Map<String, Object>> page = employeeWithdrawDao.search(dto);
            return ApiResponse.success("Withdraws fetched successfully", page);
        } catch (Exception e) {
            e.printStackTrace();
            throw new ValidationException("Failed to search withdraws: " + e.getMessage());
        }
    }

    private void validate(EmployeeWithdrawDto dto) {
        if (dto.getEmployeeId() == null) {
            throw new ValidationException("Employee is required");
        }
        if (dto.getPayment() == null || dto.getPayment().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Payment must be greater than 0");
        }
    }
}


