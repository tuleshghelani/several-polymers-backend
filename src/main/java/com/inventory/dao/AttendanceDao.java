package com.inventory.dao;

import com.inventory.dto.request.AttendanceSearchRequestDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.hibernate.jpa.QueryHints;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
                e.id as employee_id, e.name as employee_name,
                a.regular_hours, a.overtime_hours, a.regular_pay, a.overtime_pay, a.total_pay
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
                "name", row[index++]
            ));

            attendance.put("regularHours", row[index++]);
            attendance.put("overtimeHours", row[index++]);
            attendance.put("regularPay", row[index++]);
            attendance.put("overtimePay", row[index++]);
            attendance.put("totalPay", row[index++]);
            
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
            a.regular_hours, a.overtime_hours, a.regular_pay, a.overtime_pay , a.total_pay        \s
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
                COALESCE(SUM(a.regular_hours), 0) as total_regular_hours,
                COALESCE(SUM(a.overtime_hours), 0) as total_overtime_hours,
                COALESCE(SUM(a.regular_pay), 0) as total_regular_pay,
                COALESCE(SUM(a.overtime_pay), 0) as total_overtime_pay,
                COALESCE(SUM(a.total_pay), 0) as total_pay
            FROM employee e
            LEFT JOIN attendance a ON e.id = a.employee_id 
                AND DATE(a.start_date_time) BETWEEN :startDate AND :endDate
            WHERE e.status = 'A' AND e.client_id = :clientId
            GROUP BY e.id, e.name, e.regular_pay, e.wage_type
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
            summary.put("totalRegularHours", row[index++]);
            summary.put("totalOvertimeHours", row[index++]);
            summary.put("totalRegularPay", row[index++]);
            summary.put("totalOvertimePay", row[index++]);
            summary.put("totalPay", row[index++]);
            
            summaries.add(summary);
        }
        
        return summaries;
    }
} 