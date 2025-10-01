package com.inventory.dao;

import com.inventory.dto.ProductionDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
public class ProductionDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> search(ProductionDto dto) {
        StringBuilder countSql = new StringBuilder("SELECT COUNT(p.id) FROM production p WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        appendConditions(countSql, params, dto);

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT p.id, p.bach_id, p.product_id, p.quantity, p.number_of_roll
            FROM production p
            WHERE 1=1
        """);
        appendConditions(sql, params, dto);
        sql.append(" ORDER BY p." + dto.getSortBy() + " " + dto.getSortDir().toUpperCase());
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
            map.put("numberOfRoll", row[4]);
            content.add(map);
        }
        response.put("content", content);
        response.put("totalRecords", totalRecords);
        response.put("pageSize", dto.getSize());
        return response;
    }

    private void appendConditions(StringBuilder sql, Map<String, Object> params, ProductionDto dto) {
        sql.append(" AND p.client_id = :clientId");
        params.put("clientId", dto.getClientId());
        if (dto.getBatchId() != null) {
            sql.append(" AND p.bach_id = :batchId");
            params.put("batchId", dto.getBatchId());
        }
        if (dto.getProductId() != null) {
            sql.append(" AND p.product_id = :productId");
            params.put("productId", dto.getProductId());
        }
    }
}


