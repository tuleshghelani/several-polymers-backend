package com.inventory.dao;

import com.inventory.dto.PowderCoatingReturnDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.*;

@Repository
public class PowderCoatingReturnDao {
    @PersistenceContext
    private EntityManager entityManager;
    
    public Map<String, Object> searchReturns(PowderCoatingReturnDto dto) {
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("""
            SELECT COUNT(r.id)
            FROM (SELECT * FROM powder_coating_return WHERE client_id = :clientId) r
            LEFT JOIN (SELECT * FROM powder_coating_process WHERE client_id = :clientId) p ON r.process_id = p.id
            LEFT JOIN (SELECT * FROM customer WHERE client_id = :clientId) c ON p.customer_id = c.id
            LEFT JOIN (SELECT * FROM product WHERE client_id = :clientId) pr ON p.product_id = pr.id
            WHERE 1=1
        """);
        params.put("clientId", dto.getClientId());

        appendSearchConditions(countSql, params, dto);
        
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT 
                r.id,
                r.return_quantity,
                r.created_at,
                p.id as process_id,
                p.quantity as total_quantity,
                p.remaining_quantity,
                c.name as customer_name,
                pr.name as product_name
            FROM (SELECT * FROM powder_coating_return WHERE client_id = :clientId) r
            LEFT JOIN (SELECT * FROM powder_coating_process WHERE client_id = :clientId) p ON r.process_id = p.id
            LEFT JOIN (SELECT * FROM customer WHERE client_id = :clientId) c ON p.customer_id = c.id
            LEFT JOIN (SELECT * FROM product WHERE client_id = :clientId) pr ON p.product_id = pr.id
            WHERE 1=1
        """);

        appendSearchConditions(sql, params, dto);
        
        sql.append(" ORDER BY r.created_at DESC LIMIT :pageSize OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());
        setQueryParameters(query, params, dto);

        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, dto);
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, PowderCoatingReturnDto dto) {
        if (dto.getProcessId() != null) {
            sql.append(" AND r.process_id = :processId");
            params.put("processId", dto.getProcessId());
        }

        if (StringUtils.hasText(dto.getSearch())) {
            sql.append("""
                AND (LOWER(c.name) LIKE LOWER(:search)
                OR LOWER(pr.name) LIKE LOWER(:search))
            """);
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }
    }

    private void setQueryParameters(Query query, Map<String, Object> params, PowderCoatingReturnDto dto) {
        params.forEach(query::setParameter);
        query.setParameter("pageSize", dto.getPerPageRecord());
        query.setParameter("offset", dto.getCurrentPage() * dto.getPerPageRecord());
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, PowderCoatingReturnDto dto) {
        List<Map<String, Object>> returns = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> returnRecord = new HashMap<>();
            returnRecord.put("id", row[0]);
            returnRecord.put("returnQuantity", row[1]);
            returnRecord.put("createdAt", row[2]);
            returnRecord.put("processId", row[3]);
            returnRecord.put("totalQuantity", row[4]);
            returnRecord.put("remainingQuantity", row[5]);
            returnRecord.put("customerName", row[6]);
            returnRecord.put("productName", row[7]);
            returns.add(returnRecord);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", returns);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / dto.getPerPageRecord()));
        
        return response;
    }
} 