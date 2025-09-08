package com.inventory.dao;

import com.inventory.dto.EmployeeOrderDto;
import com.inventory.exception.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class EmployeeOrderDao {
    private final EntityManager entityManager;
    
    public Map<String, Object> searchEmployeeOrders(EmployeeOrderDto dto) {
        StringBuilder countQuery = new StringBuilder("SELECT COUNT(*) FROM (SELECT * FROM employee_orders WHERE client_id = :clientId) eo JOIN (SELECT * FROM product WHERE client_id = :clientId) p ON eo.product_id = p.id");
        StringBuilder dataQuery = new StringBuilder("""
            SELECT 
                eo.id, 
                eo.created_at as createdAt,
                eo.updated_at as updatedAt,
                p.id as productId,
                p.name as productName,
                eo.employee_ids as employeeIds,
                eo.quantity,
                eo.remarks,
                eo.status
            FROM (SELECT * FROM employee_orders WHERE client_id = :clientId) eo
            JOIN (SELECT * FROM product WHERE client_id = :clientId) p ON eo.product_id = p.id 
            WHERE 1=1
        """);
        
        Map<String, Object> params = new HashMap<>();
        params.put("clientId", dto.getClientId());
        buildWhereClause(countQuery, dataQuery, params, dto);
        
        dataQuery.append(" ORDER BY eo.").append(dto.getSortBy())
                .append(" ").append(dto.getSortDir())
                .append(" LIMIT :perPageRecord OFFSET :offset");
        
        Query query = entityManager.createNativeQuery(countQuery.toString());
        setParameters(query, params);
        long totalRecords = ((Number) query.getSingleResult()).longValue();
        
        query = entityManager.createNativeQuery(dataQuery.toString());
        params.put("offset", dto.getCurrentPage() * dto.getPerPageRecord());
        params.put("perPageRecord", dto.getPerPageRecord());
        setParameters(query, params);
        
        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, dto);
    }
    
    private void buildWhereClause(StringBuilder countQuery, StringBuilder dataQuery,
            Map<String, Object> params, EmployeeOrderDto dto) {
        if (dto != null && StringUtils.hasText(dto.getSearch())) {
            String search = "%" + dto.getSearch().toLowerCase() + "%";
            countQuery.append(" AND (LOWER(p.name) LIKE :search OR LOWER(eo.remarks) LIKE :search)");
            dataQuery.append(" AND (LOWER(p.name) LIKE :search OR LOWER(eo.remarks) LIKE :search)");
            params.put("search", search);
        }
        
        if (dto.getStartDate() != null) {
            countQuery.append(" AND eo.created_at >= :startDate");
            dataQuery.append(" AND eo.created_at >= :startDate");
            params.put("startDate", dto.getStartDate());
        }

        if (dto.getEndDate() != null) {
            countQuery.append(" AND eo.created_at <= :endDate");
            dataQuery.append(" AND eo.created_at <= :endDate");
            params.put("endDate", dto.getEndDate().plusDays(1));
        }
        
        if (StringUtils.hasText(dto.getStatus())) {
            countQuery.append(" AND eo.status = :status");
            dataQuery.append(" AND eo.status = :status");
            params.put("status", dto.getStatus());
        }
    }
    
    private void setParameters(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }
    
    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, EmployeeOrderDto dto) {
        List<Map<String, Object>> orders = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> order = new HashMap<>();
            order.put("id", row[0]);
            order.put("createdAt", row[1]);
            order.put("updatedAt", row[2]);
            order.put("productId", row[3]);
            order.put("productName", row[4]);
            order.put("employeeIds", row[5]);
            order.put("quantity", row[6]);
            order.put("remarks", row[7]);
            order.put("status", row[8]);
            orders.add(order);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", orders);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / dto.getPerPageRecord()));
        
        return response;
    }
} 