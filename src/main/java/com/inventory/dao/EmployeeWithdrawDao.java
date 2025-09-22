package com.inventory.dao;

import com.inventory.dto.EmployeeWithdrawDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.*;

@Repository
public class EmployeeWithdrawDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Page<Map<String, Object>> search(EmployeeWithdrawDto dto) {
        StringBuilder select = new StringBuilder();
        StringBuilder fromWhere = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        select.append("SELECT ew.id, e.name AS employee_name, ew.employee_id, ew.withdraw_date, ew.payment, ew.remarks, ew.created_at ");
        fromWhere.append("FROM (select * from employee_withdraw ew where ew.client_id = :clientId) ew ");
        fromWhere.append("LEFT JOIN (select id, name from employee where client_id = :clientId) e ON e.id = ew.employee_id ");
        fromWhere.append("WHERE 1=1 ");
        params.put("clientId", dto.getClientId());

        if (dto.getEmployeeId() != null) {
            fromWhere.append("AND ew.employee_id = :employeeId ");
            params.put("employeeId", dto.getEmployeeId());
        }
        if (dto.getStartDate() != null) {
            fromWhere.append("AND ew.withdraw_date >= :startDate ");
            params.put("startDate", dto.getStartDate().toLocalDate());
        }
        if (dto.getEndDate() != null) {
            fromWhere.append("AND ew.withdraw_date <= :endDate ");
            params.put("endDate", dto.getEndDate().toLocalDate());
        }
        if (dto.getSearch() != null && StringUtils.hasText(dto.getSearch())) {
            fromWhere.append("AND (LOWER(e.name) LIKE LOWER(:search) OR LOWER(ew.remarks) LIKE LOWER(:search)) ");
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) ");
        countSql.append(fromWhere);

        StringBuilder dataSql = new StringBuilder();
        dataSql.append(select).append(fromWhere).append(" ORDER BY ew.id DESC LIMIT :limit OFFSET :offset");

        Pageable pageable = PageRequest.of(dto.getCurrentPage(), dto.getPerPageRecord());

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        Query dataQuery = entityManager.createNativeQuery(dataSql.toString());
        params.forEach(dataQuery::setParameter);
        dataQuery.setParameter("limit", pageable.getPageSize());
        dataQuery.setParameter("offset", pageable.getPageNumber() * pageable.getPageSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = dataQuery.getResultList();
        List<Map<String, Object>> content = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r[0]);
            m.put("employeeName", r[1]);
            m.put("employeeId", r[2]);
            m.put("withdrawDate", r[3]);
            m.put("payment", r[4]);
            m.put("remarks", r[5]);
            m.put("createdAt", r[6]);
            content.add(m);
        }

        return new PageImpl<>(content, pageable, total);
    }
    
    public List<Map<String, Object>> getAllEmployeesWithdrawSummary(Long clientId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT 
                e.id as employee_id,
                e.name as employee_name,
                COALESCE(SUM(ew.payment), 0) as total_withdraw
            FROM (select * from employee e where e.client_id = :clientId and e.status = 'A') e
            LEFT JOIN employee_withdraw ew ON e.id = ew.employee_id 
                AND ew.withdraw_date BETWEEN :startDate AND :endDate
            GROUP BY e.id, e.name
            ORDER BY e.name ASC
        """;
        
        Query query = entityManager.createNativeQuery(sql)
            .setParameter("clientId", clientId)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate);
        
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> summaries = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> summary = new HashMap<>();
            summary.put("employeeId", row[0]);
            summary.put("employeeName", row[1]);
            summary.put("totalWithdraw", row[2]);
            summaries.add(summary);
        }
        
        return summaries;
    }
}


