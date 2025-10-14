package com.inventory.dao;

import com.inventory.dto.PaymentHistoryDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PaymentHistoryDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> searchPaymentHistories(PaymentHistoryDto dto) {
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("SELECT COUNT(ph.id) FROM payment_history ph WHERE ph.client_id = :clientId");
        params.put("clientId", dto.getClientId());

        if (dto.getCustomerId() != null) {
            countSql.append(" AND ph.customer_id = :customerId");
            params.put("customerId", dto.getCustomerId());
        }

        if (StringUtils.hasText(dto.getSearch())) {
            countSql.append(" AND (ph.remarks LIKE :search)");
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }
        
        if (dto.getStartDate() != null) {
            countSql.append(" AND ph.date >= :startDate");
            params.put("startDate", dto.getStartDate());
        }
        
        if (dto.getEndDate() != null) {
            countSql.append(" AND ph.date <= :endDate");
            params.put("endDate", dto.getEndDate());
        }

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT 
                ph.id, ph.amount, ph.customer_id, c.name as customer_name,
                ph.type, ph.remarks, ph.is_received,
                ph.created_at, ph.updated_at, ph.date,
                u1.first_name as created_by_first_name, u1.last_name as created_by_last_name,
                u2.first_name as updated_by_first_name, u2.last_name as updated_by_last_name
            FROM payment_history ph
            LEFT JOIN customer c ON ph.customer_id = c.id
            LEFT JOIN user_master u1 ON ph.created_by = u1.id
            LEFT JOIN user_master u2 ON ph.updated_by = u2.id
            WHERE ph.client_id = :clientId
        """);

        if (dto.getCustomerId() != null) {
            sql.append(" AND ph.customer_id = :customerId");
            params.put("customerId", dto.getCustomerId());
        }

        if (StringUtils.hasText(dto.getSearch())) {
            sql.append(" AND (LOWER(c.name) LIKE LOWER(:search) OR ph.remarks LIKE :search)");
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }
        
        if (dto.getStartDate() != null) {
            sql.append(" AND ph.date >= :startDate");
            params.put("startDate", dto.getStartDate());
        }
        
        if (dto.getEndDate() != null) {
            sql.append(" AND ph.date <= :endDate");
            params.put("endDate", dto.getEndDate());
        }

        sql.append(" ORDER BY ph.created_at DESC LIMIT :pageSize OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);
        query.setParameter("pageSize", dto.getPerPageRecord());
        query.setParameter("offset", (long) dto.getCurrentPage() * dto.getPerPageRecord());

        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, dto.getPerPageRecord());
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, int pageSize) {
        List<Map<String, Object>> paymentHistories = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> paymentHistory = new HashMap<>();
            paymentHistory.put("id", row[0]);
            paymentHistory.put("amount", row[1]);
            paymentHistory.put("customerId", row[2]);
            paymentHistory.put("customerName", row[3]);
            paymentHistory.put("type", row[4]);
            paymentHistory.put("remarks", row[5]);
            paymentHistory.put("isReceived", row[6]);
            paymentHistory.put("createdAt", row[7]);
            paymentHistory.put("updatedAt", row[8]);
            paymentHistory.put("date", row[9]);
            paymentHistory.put("createdByName", (row[10] != null ? row[10] : "") + " " + (row[11] != null ? row[11] : ""));
            paymentHistory.put("updatedByName", (row[12] != null ? row[12] : "") + " " + (row[13] != null ? row[13] : ""));
            paymentHistories.add(paymentHistory);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", paymentHistories);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / pageSize));

        return response;
    }
}