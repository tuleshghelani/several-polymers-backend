package com.inventory.service;

import com.inventory.dao.AttendanceDao;
import com.inventory.dao.EmployeeDao;
import com.inventory.dao.EmployeeWithdrawDao;
import com.inventory.dto.ApiResponse;
import com.inventory.dto.EmployeeWithdrawDto;
import com.inventory.dto.request.AttendanceDeleteRequestDto;
import com.inventory.dto.request.AttendancePdfRequestDto;
import com.inventory.dto.request.AttendanceRequestDto;
import com.inventory.dto.request.AttendanceSearchRequestDto;
import com.inventory.dto.request.PayrollSummaryRequestDto;
import com.inventory.entity.Attendance;
import com.inventory.entity.Employee;
import com.inventory.entity.UserMaster;
import com.inventory.exception.ValidationException;
import com.inventory.repository.AttendanceRepository;
import com.inventory.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private static final BigDecimal DEFAULT_REGULAR_HOURS = new BigDecimal("8.00");
    private static final Logger logger = LoggerFactory.getLogger(AttendanceService.class);
    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final UtilityService utilityService;
    private final AttendanceDao attendanceDao;
    private final AttendanceSummaryPdfService attendanceSummaryPdfService;
    private final EmployeeDao employeeDao;
    private final EmployeeWithdrawDao employeeWithdrawDao;

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> saveAttendance(AttendanceRequestDto request) {
        try {
            validateRequest(request);
            var currentUser = utilityService.getCurrentLoggedInUser();
            
            // Fetch all employees in one query
            Map<Long, Employee> employeeMap = employeeRepository.findAllById(request.getEmployeeIds())
                .stream()
                .collect(Collectors.toMap(Employee::getId, e -> e));
            
            List<Attendance> attendances = request.getEmployeeIds().stream()
                .map(employeeId -> {
                    Employee employee = employeeMap.get(employeeId);
                    if (employee == null) {
                        throw new ValidationException("Employee not found with ID: " + employeeId);
                    }
                    
                    return createAttendanceWithSalary(employee, request, currentUser);
                })
                .collect(Collectors.toList());
            
            attendanceRepository.saveAll(attendances);
            
            return ApiResponse.success("Attendance saved successfully", attendances.size());
        } catch (ValidationException e) {
            logger.error("Validation error: ", e);
            throw e;
        } catch (Exception e) {
            logger.error("Failed to save attendance", e);
            throw new ValidationException("Failed to save attendance: " + e.getMessage());
        }
    }

    private Attendance createAttendanceWithSalary(Employee employee, AttendanceRequestDto request, UserMaster currentUser) {
        // Calculate hours worked
        Duration duration = Duration.between(request.getStartDateTime(), request.getEndDateTime());
        BigDecimal hoursWorked = BigDecimal.valueOf(duration.toMinutes())
            .divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
        
        // Get regular hours (default to 8 if not set)
        BigDecimal regularHours = employee.getRegularHours() != null && employee.getRegularHours().compareTo(BigDecimal.ZERO) > 0 
            ? employee.getRegularHours() 
            : DEFAULT_REGULAR_HOURS;
        
        // Calculate overtime hours
        BigDecimal overtimeHours = hoursWorked.compareTo(regularHours) > 0 
            ? hoursWorked.subtract(regularHours)
            : BigDecimal.ZERO;
        
        // Calculate pays
        BigDecimal regularPay = calculateRegularPay(employee, regularHours);
        BigDecimal overtimePay = calculateOvertimePay(employee, overtimeHours);
        BigDecimal totalPay = regularPay.add(overtimePay);
        
        return Attendance.builder()
            .employee(employee)
            .startDateTime(request.getStartDateTime())
            .endDateTime(request.getEndDateTime())
            .regularHours(regularHours)
            .overtimeHours(overtimeHours)
            .regularPay(regularPay)
            .overtimePay(overtimePay)
            .totalPay(totalPay)
            .remarks(request.getRemarks())
            .client(currentUser.getClient())
            .createdBy(currentUser)
            .createdAt(OffsetDateTime.now())
            .build();
    }

    private BigDecimal calculateRegularPay(Employee employee, BigDecimal regularHours) {
        BigDecimal configuredPay = employee.getRegularPay() != null ? employee.getRegularPay() : BigDecimal.ZERO;
        
        // For FIXED wage type, pay per attendance day = monthly salary / 30 (always divide by 30)
        if ("FIXED".equalsIgnoreCase(employee.getWageType())) {
            return configuredPay.divide(BigDecimal.valueOf(30), 2, RoundingMode.HALF_UP);
        }
        
        // For HOURLY wage type, pay = hourly rate * regular hours
        if ("HOURLY".equalsIgnoreCase(employee.getWageType())) {
            return configuredPay.multiply(regularHours)
                .setScale(2, RoundingMode.HALF_UP);
        }
        
        // Default fallback (unknown wage type)
        return configuredPay.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateOvertimePay(Employee employee, BigDecimal overtimeHours) {
        if (!"HOURLY".equalsIgnoreCase(employee.getWageType()) || 
            overtimeHours.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal overtimeRate = employee.getOvertimePay() != null ? 
            employee.getOvertimePay() : BigDecimal.ZERO;
        
        return overtimeRate.multiply(overtimeHours)
            .setScale(2, RoundingMode.HALF_UP);
    }

    private void validateRequest(AttendanceRequestDto request) {
        if (request.getEmployeeIds() == null || request.getEmployeeIds().isEmpty()) {
            throw new ValidationException("No employee IDs provided");
        }
        
        if (request.getStartDateTime() == null) {
            throw new ValidationException("Start date time is required");
        }
        
        if (request.getEndDateTime() == null) {
            throw new ValidationException("End date time is required");
        }
        
        if (request.getEndDateTime().isBefore(request.getStartDateTime())) {
            throw new ValidationException("End date time cannot be before start date time");
        }
    }

    public ApiResponse<?> getAttendanceByEmployee(AttendanceSearchRequestDto request) {
        try {
            if (request.getEmployeeId() == null) {
                throw new ValidationException("Employee ID is required");
            }
            
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            Map<String, Object> result = attendanceDao.getAttendanceByEmployee(
                request.getEmployeeId(),
                currentUser.getClient().getId(),
                request.getPage(),
                request.getSize(),
                request
            );
            
            return ApiResponse.success("Attendance records retrieved successfully", result);
        } catch (Exception e) {
            throw new ValidationException("Failed to fetch attendance records: " + e.getMessage());
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public ApiResponse<?> deleteAttendances(AttendanceDeleteRequestDto request) {
        try {
            if (request.getAttendanceIds() == null || request.getAttendanceIds().isEmpty()) {
                throw new ValidationException("No attendance IDs provided");
            }

            UserMaster currentUser = utilityService.getCurrentLoggedInUser();

            List<Attendance> attendances = attendanceRepository.findAllById(request.getAttendanceIds());

            if (attendances.size() != request.getAttendanceIds().size()) {
                throw new ValidationException("One or more attendance records not found");
            }
            
            for (Attendance attendance : attendances) {
                if (!attendance.getClient().getId().equals(currentUser.getClient().getId())) {
                    throw new ValidationException("You are not authorized to delete some of these records");
                }
            }
            
            attendanceRepository.deleteAllById(request.getAttendanceIds());
            
            return ApiResponse.success("Attendance records deleted successfully", request.getAttendanceIds().size());
        } catch (Exception e) {
            throw new ValidationException("Failed to delete attendance records: " + e.getMessage());
        }
    }

    public byte[] generateAttendancePdf(AttendancePdfRequestDto request) {
        try {
            // Validate request
            if (request.getEmployeeId() == null) {
                throw new ValidationException("Employee ID is required");
            }
            
            // Get month start and end dates
            LocalDate startDate = request.getStartDate().withDayOfMonth(1);
            LocalDate endDate = startDate.plusMonths(1).minusDays(1);
            
            // Get employee details
            Map<String, Object> employeeData = employeeDao.getEmployeeDetail(request.getEmployeeId());
            
            // Get attendance records
            List<Map<String, Object>> attendanceRecords = attendanceDao.getMonthlyAttendance(
                request.getEmployeeId(), 
                startDate, 
                endDate
            );
            
            // Get withdraw records for the same period
            List<Map<String, Object>> withdrawRecords = getEmployeeWithdrawData(
                request.getEmployeeId(), 
                employeeData.get("clientId"), 
                startDate, 
                endDate
            );
            
            return attendanceSummaryPdfService.generatePdf(employeeData, attendanceRecords, withdrawRecords, startDate, endDate);
        } catch (Exception e) {
            throw new ValidationException("Failed to generate attendance PDF: " + e.getMessage());
        }
    }
    
    private List<Map<String, Object>> getEmployeeWithdrawData(Long employeeId, Object clientId, LocalDate startDate, LocalDate endDate) {
        try {
            EmployeeWithdrawDto dto = new EmployeeWithdrawDto();
            dto.setEmployeeId(employeeId);
            dto.setClientId((Long) clientId);
            dto.setStartDate(startDate.atStartOfDay().atZone(ZoneId.of("Asia/Kolkata")).toOffsetDateTime());
            dto.setEndDate(endDate.atTime(23, 59, 59).atZone(ZoneId.of("Asia/Kolkata")).toOffsetDateTime());
            dto.setCurrentPage(0);
            dto.setPerPageRecord(1000); // Get all records for the period
            
            return employeeWithdrawDao.search(dto).getContent();
        } catch (Exception e) {
            logger.error("Error fetching withdraw data for employee {}: {}", employeeId, e.getMessage());
            return List.of(); // Return empty list if error occurs
        }
    }
    
    public byte[] generatePayrollSummaryPdf(PayrollSummaryRequestDto request) {
        try {
            // Validate request
            if (request.getStartDate() == null || request.getEndDate() == null) {
                throw new ValidationException("Start date and end date are required");
            }
            
            if (request.getEndDate().isBefore(request.getStartDate())) {
                throw new ValidationException("End date cannot be before start date");
            }
            
            UserMaster currentUser = utilityService.getCurrentLoggedInUser();
            
            // Get attendance summaries for all employees
            List<Map<String, Object>> attendanceSummaries = attendanceDao.getAllEmployeesAttendanceSummary(
                currentUser.getClient().getId(), 
                request.getStartDate(), 
                request.getEndDate()
            );
            
            // Get withdraw summaries for all employees
            List<Map<String, Object>> withdrawSummaries = employeeWithdrawDao.getAllEmployeesWithdrawSummary(
                currentUser.getClient().getId(), 
                request.getStartDate(), 
                request.getEndDate()
            );
            
            return attendanceSummaryPdfService.generatePayrollSummaryPdf(
                attendanceSummaries, 
                withdrawSummaries, 
                request.getStartDate(), 
                request.getEndDate()
            );
        } catch (Exception e) {
            throw new ValidationException("Failed to generate payroll summary PDF: " + e.getMessage());
        }
    }
} 