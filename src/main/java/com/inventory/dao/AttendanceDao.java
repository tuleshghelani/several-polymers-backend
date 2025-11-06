package com.inventory.dao;

import com.inventory.dto.AttendanceDto;
import com.inventory.dto.EmployeeDto;
import com.inventory.dto.request.AttendanceSearchRequestDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.jpa.QueryHints;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class AttendanceDao {
    @PersistenceContext
    private EntityManager entityManager;
    
    public Map<String, Object> getAttendanceByEmployee(Long employeeId, Long clientId, Integer page, Integer size, AttendanceSearchRequestDto request) {
        StringBuilder baseCondition = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        baseCondition.append(" WHERE a.employee_id = :employeeId AND a.client_id = :clientId");
        params.put("employeeId", employeeId);
        params.put("clientId", clientId);
        
        if (request.getStartDate() != null && request.getEndDate() != null) {
            baseCondition.append(" AND a.start_date_time >= :startDate AND a.start_date_time <= :endDate");
            params.put("startDate", request.getStartDate());
            params.put("endDate", request.getEndDate().plusDays(1));
        }
        
        // Count Query
        String countSql = "SELECT COUNT(a.id) FROM attendance a" + baseCondition;
        Query countQuery = entityManager.createNativeQuery(countSql);
        setQueryParameters(countQuery, params);
        Long totalRecords = ((Number) countQuery.getSingleResult()).longValue();
        
        // Main Query
        String sql = """
            SELECT 
                a.id, a.start_date_time, a.end_date_time, 
                a.remarks, a.created_at,
                e.id as employee_id, e.name as employee_name, e.mobile_number,
                a.regular_hours, a.overtime_hours, a.regular_pay, a.overtime_pay, a.total_pay,
                a.shift
            FROM attendance a
            JOIN employee e ON a.employee_id = e.id
        """ + baseCondition + 
        " ORDER BY a.start_date_time DESC LIMIT :pageSize OFFSET :offset";
        
        Query query = entityManager.createNativeQuery(sql);
        setQueryParameters(query, params);
        query.setParameter("pageSize", size);
        query.setParameter("offset", page * size);
        
        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, size);
    }
    
    private void setQueryParameters(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }
    
    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, int pageSize) {
        List<Map<String, Object>> attendances = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> attendance = new HashMap<>();
            int index = 0;
            attendance.put("id", row[index++]);
            attendance.put("startDateTime", row[index++]);
            attendance.put("endDateTime", row[index++]);
            attendance.put("remarks", row[index++]);
            attendance.put("createdAt", row[index++]);
            
            attendance.put("employee", Map.of(
                "id", row[index++],
                "name", row[index++],
                "mobileNumber", row[index++]
            ));

            attendance.put("regularHours", row[index++]);
            attendance.put("overtimeHours", row[index++]);
            attendance.put("regularPay", row[index++]);
            attendance.put("overtimePay", row[index++]);
            attendance.put("totalPay", row[index++]);
            attendance.put("shift", row[index++]);
            
            attendances.add(attendance);
        }
        
        return Map.of(
            "content", attendances,
            "totalElements", totalRecords,
            "totalPages", (totalRecords + pageSize - 1) / pageSize
        );
    }
    
    public List<Map<String, Object>> getMonthlyAttendance(Long employeeId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            select DATE(a.start_date_time) as attendance_date, a.start_date_time, a.end_date_time,
            a.regular_hours, a.overtime_hours, a.regular_pay, a.overtime_pay , a.total_pay, a.shift       \s
                FROM
                    attendance a        \s
                WHERE a.employee_id = :employeeId
                AND DATE(a.start_date_time) BETWEEN :startDate AND :endDate
        """;
        
        Query query = entityManager.createNativeQuery(sql)
            .setParameter("employeeId", employeeId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setHint(QueryHints.HINT_FETCH_SIZE, 50);
        
        List<Object[]> results = query.getResultList();
        return transformAttendanceResults(results);
    }
    
    private List<Map<String, Object>> transformAttendanceResults(List<Object[]> results) {
        List<Map<String, Object>> attendances = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> attendance = new HashMap<>();
            int index = 0;
            
            // Map fields based on the query columns from getMonthlyAttendance()
            attendance.put("attendance_date", row[index++]);
            attendance.put("start_date_time", row[index++]);
            attendance.put("end_date_time", row[index++]);
            attendance.put("regular_hours", row[index++]);
            attendance.put("overtime_hours", row[index++]);
            attendance.put("regular_pay", row[index++]);
            attendance.put("overtime_pay", row[index++]);
            attendance.put("total_pay", row[index++]);
            attendance.put("shift", row[index++]);
            attendances.add(attendance);
        }
        
        return attendances;
    }
    
    public List<Map<String, Object>> getAllEmployeesAttendanceSummary(Long clientId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                e.id as employee_id,
                e.name as employee_name,
                e.regular_pay,
                e.wage_type,
                e.regular_hours,
                COALESCE(SUM(a.regular_hours), 0) as total_regular_hours,
                COALESCE(SUM(a.overtime_hours), 0) as total_overtime_hours,
                COALESCE(SUM(a.regular_pay), 0) as total_regular_pay,
                COALESCE(SUM(a.overtime_pay), 0) as total_overtime_pay,
                COALESCE(SUM(a.total_pay), 0) as total_pay
            FROM employee e
            LEFT JOIN attendance a ON e.id = a.employee_id 
                AND DATE(a.start_date_time) BETWEEN :startDate AND :endDate
            WHERE e.status = 'A' AND e.client_id = :clientId
            GROUP BY e.id, e.name, e.regular_pay, e.wage_type, e.regular_hours
            ORDER BY e.name ASC
        """;
        
        Query query = entityManager.createNativeQuery(sql)
            .setParameter("clientId", clientId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate);
        
        List<Object[]> results = query.getResultList();
        return transformAttendanceSummaryResults(results);
    }
    
    private List<Map<String, Object>> transformAttendanceSummaryResults(List<Object[]> results) {
        List<Map<String, Object>> summaries = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> summary = new HashMap<>();
            int index = 0;
            
            summary.put("employeeId", row[index++]);
            summary.put("employeeName", row[index++]);
            summary.put("regularPay", row[index++]);
            summary.put("wageType", row[index++]);
            summary.put("regularHours", row[index++]);
            summary.put("totalRegularHours", row[index++]);
            summary.put("totalOvertimeHours", row[index++]);
            summary.put("totalRegularPay", row[index++]);
            summary.put("totalOvertimePay", row[index++]);
            summary.put("totalPay", row[index++]);
            
            summaries.add(summary);
        }
        
        return summaries;
    }
    
    /**
     * Get all attendance records with pagination and filters
     * Optimized for performance with proper indexing and efficient joins
     * Returns Map with content and pagination metadata
     */
    public Map<String, Object> getAllAttendanceWithFilters(Long clientId, AttendanceDto request) {
        StringBuilder baseCondition = new StringBuilder();
        Map<String, Object> params = new HashMap<>();
        
        // Base condition
        baseCondition.append(" WHERE a.client_id = :clientId");
        params.put("clientId", clientId);
        
        // Apply filters dynamically
        if (request.getEmployeeId() != null) {
            baseCondition.append(" AND a.employee_id = :employeeId");
            params.put("employeeId", request.getEmployeeId());
        }
        
        if (request.getStartDate() != null && request.getEndDate() != null) {
            baseCondition.append(" AND DATE(a.start_date_time) BETWEEN :startDate AND :endDate");
            params.put("startDate", request.getStartDate());
            params.put("endDate", request.getEndDate());
        } else if (request.getStartDate() != null) {
            baseCondition.append(" AND DATE(a.start_date_time) >= :startDate");
            params.put("startDate", request.getStartDate());
        } else if (request.getEndDate() != null) {
            baseCondition.append(" AND DATE(a.start_date_time) <= :endDate");
            params.put("endDate", request.getEndDate());
        }
        
        if (request.getShift() != null && !request.getShift().isBlank()) {
            baseCondition.append(" AND a.shift = :shift");
            params.put("shift", request.getShift());
        }
        
        if (request.getEmployeeName() != null && !request.getEmployeeName().isBlank()) {
            baseCondition.append(" AND LOWER(e.name) LIKE LOWER(:employeeName)");
            params.put("employeeName", "%" + request.getEmployeeName() + "%");
        }
        
        if (request.getEmployeeMobile() != null && !request.getEmployeeMobile().isBlank()) {
            baseCondition.append(" AND e.mobile_number LIKE :employeeMobile");
            params.put("employeeMobile", "%" + request.getEmployeeMobile() + "%");
        }
        
        // Count Query
        String countSql = "SELECT COUNT(a.id) FROM attendance a " +
                         "JOIN employee e ON a.employee_id = e.id" +
                         baseCondition;
        Query countQuery = entityManager.createNativeQuery(countSql);
        setQueryParameters(countQuery, params);
        Long totalRecords = ((Number) countQuery.getSingleResult()).longValue();
        
        // Determine sort column
        String sortColumn = getSortColumn(request.getSortBy());
        String sortDirection = "desc".equalsIgnoreCase(request.getSortDir()) ? "DESC" : "ASC";
        
        // Main Query with proper joins for performance
        String sql = """
            SELECT 
                a.id, 
                a.start_date_time, 
                a.end_date_time, 
                a.regular_hours, 
                a.overtime_hours, 
                a.regular_pay, 
                a.overtime_pay, 
                a.total_pay,
                a.remarks, 
                a.shift,
                a.created_at,
                e.id as employee_id, 
                e.name as employee_name, 
                e.mobile_number as employee_mobile,
                e.email as employee_email,
                e.designation as employee_designation,
                e.department as employee_department,
                e.status as employee_status
            FROM attendance a
            JOIN employee e ON a.employee_id = e.id
            LEFT JOIN user_master u ON a.created_by = u.id
        """ + baseCondition + 
        " ORDER BY " + sortColumn + " " + sortDirection + 
        " LIMIT :pageSize OFFSET :offset";
        
        Query query = entityManager.createNativeQuery(sql);
        setQueryParameters(query, params);
        query.setParameter("pageSize", request.getSize());
        query.setParameter("offset", request.getPage() * request.getSize());
        
        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        
        return buildAttendanceResponse(results, totalRecords, request);
    }
    
    /**
     * Map sort field to actual database column
     */
    private String getSortColumn(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return "a.id";
        }
        
        return switch (sortBy.toLowerCase()) {
            case "id" -> "a.id";
            case "startdatetime", "startdate" -> "a.start_date_time";
            case "enddatetime", "enddate" -> "a.end_date_time";
            case "employeename", "employee" -> "e.name";
            case "shift" -> "a.shift";
            case "totalpay" -> "a.total_pay";
            case "createdat" -> "a.created_at";
            default -> "a.id";
        };
    }
    
    /**
     * Build paginated response with proper mapping
     * Returns Map with content and pagination metadata
     */
    private Map<String, Object> buildAttendanceResponse(
            List<Object[]> results, 
            Long totalRecords, 
            AttendanceDto request) {
        
        List<AttendanceDto> attendances = new ArrayList<>();
        
        for (Object[] row : results) {
            int index = 0;
            
            // Build Attendance DTO - read in order from SQL query
            AttendanceDto attendance = AttendanceDto.builder()
                .id(getLongValue(row[index++]))                          // 0: a.id
                .startDateTime(getOffsetDateTimeValue(row[index++]))     // 1: a.start_date_time
                .endDateTime(getOffsetDateTimeValue(row[index++]))       // 2: a.end_date_time
                .regularHours(getBigDecimalValue(row[index++]))          // 3: a.regular_hours
                .overtimeHours(getBigDecimalValue(row[index++]))         // 4: a.overtime_hours
                .regularPay(getBigDecimalValue(row[index++]))            // 5: a.regular_pay
                .overtimePay(getBigDecimalValue(row[index++]))           // 6: a.overtime_pay
                .totalPay(getBigDecimalValue(row[index++]))              // 7: a.total_pay
                .remarks(getStringValue(row[index++]))                   // 8: a.remarks
                .shift(getStringValue(row[index++]))                     // 9: a.shift
                .createdAt(getOffsetDateTimeValue(row[index++]))         // 10: a.created_at
                .build();
            
            // Build Employee DTO
            EmployeeDto employee = new EmployeeDto();
            employee.setId(getLongValue(row[index++]));                  // 11: e.id
            employee.setName(getStringValue(row[index++]));              // 12: e.name
            employee.setMobileNumber(getStringValue(row[index++]));      // 13: e.mobile_number
            employee.setEmail(getStringValue(row[index++]));             // 14: e.email
            employee.setDesignation(getStringValue(row[index++]));       // 15: e.designation
            employee.setDepartment(getStringValue(row[index++]));        // 16: e.department
            employee.setStatus(getStringValue(row[index++]));            // 17: e.status
            
            // Set employee to attendance
            attendance.setEmployee(employee);
            
            attendances.add(attendance);
        }
        
        int totalPages = (int) ((totalRecords + request.getSize() - 1) / request.getSize());
        
        // Build Map response
        Map<String, Object> response = new HashMap<>();
        response.put("content", attendances);
        response.put("currentPage", request.getPage());
        response.put("pageSize", request.getSize());
        response.put("totalElements", totalRecords);
        response.put("totalPages", totalPages);
        response.put("isFirst", request.getPage() == 0);
        response.put("isLast", request.getPage() >= totalPages - 1);
        response.put("hasNext", request.getPage() < totalPages - 1);
        response.put("hasPrevious", request.getPage() > 0);
        
        return response;
    }
    
    // Utility methods for safe type conversion
    private Long getLongValue(Object value) {
        if (value == null) return null;
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
    
    private String getStringValue(Object value) {
        return value != null ? value.toString() : null;
    }
    
    private BigDecimal getBigDecimalValue(Object value) {
        if (value == null) return null;
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        if (value instanceof Number) {
            return new BigDecimal(value.toString());
        }
        return null;
    }
    
    private OffsetDateTime getOffsetDateTimeValue(Object value) {
        if (value == null) return null;

        if (value instanceof java.time.Instant) {
            return ((java.time.Instant) value).atOffset(java.time.ZoneOffset.UTC);
        }
        return null;
    }
} 