package com.inventory.dao;

import com.inventory.dto.MixerDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class MixerDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> search(MixerDto dto) {
        StringBuilder countSql = new StringBuilder("SELECT COUNT(m.id) FROM mixer m WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        appendConditions(countSql, params, dto);

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT m.id, m.batch_id, m.product_id, m.quantity
            FROM mixer m
            WHERE 1=1
        """);
        appendConditions(sql, params, dto);
        sql.append(" ORDER BY m." + dto.getSortBy() + " " + dto.getSortDir().toUpperCase());
        sql.append(" LIMIT :pageSize OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);
        query.setParameter("pageSize", dto.getSize());
        query.setParameter("offset", dto.getPage() * dto.getSize());

        @SuppressWarnings("unchecked")
        List<Object[]> results = (List<Object[]>) query.getResultList();
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> content = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", row[0]);
            map.put("batchId", row[1]);
            map.put("productId", row[2]);
            map.put("quantity", row[3]);
            content.add(map);
        }
        response.put("content", content);
        response.put("totalRecords", totalRecords);
        response.put("pageSize", dto.getSize());
        return response;
    }

    private void appendConditions(StringBuilder sql, Map<String, Object> params, MixerDto dto) {
        sql.append(" AND m.client_id = :clientId");
        params.put("clientId", dto.getClientId());
        if (dto.getBatchId() != null) {
            sql.append(" AND m.batch_id = :batchId");
            params.put("batchId", dto.getBatchId());
        }
        if (dto.getProductId() != null) {
            sql.append(" AND m.product_id = :productId");
            params.put("productId", dto.getProductId());
        }
    }
}


