package com.inventory.dao;

import com.inventory.dto.TransportMasterDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.*;

@Repository
public class TransportMasterDao {
    @PersistenceContext
    private EntityManager entityManager;

    public List<Map<String, Object>> getTransports(TransportMasterDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("dto must not be null");
        }
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        sql.append("""
            SELECT 
                t.id,
                t.name,
                t.mobile,
                t.gst,
                t.remarks,
                t.status
            FROM transport_master t
            WHERE 1=1
        """);

        if (StringUtils.hasText(dto.getSearch())) {
            sql.append(" AND (LOWER(t.name) LIKE LOWER(:search) OR LOWER(t.mobile) LIKE LOWER(:search))");
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }

        sql.append(" AND t.client_id = :clientId");
        params.put("clientId", dto.getClientId());

        sql.append(" ORDER BY t.id DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> results = (List<Object[]>) query.getResultList();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] row : results) {
            if (row[0] != null) {
                Map<String, Object> map = new HashMap<>(8);
                map.put("id", row[0]);
                map.put("name", row[1]);
                map.put("mobile", row[2]);
                map.put("gst", row[3]);
                map.put("remarks", row[4]);
                map.put("status", row[5]);
                rows.add(map);
            }
        }
        return rows;
    }

    public Map<String, Object> searchTransports(TransportMasterDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("dto must not be null");
        }
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("""
            SELECT COUNT(t.id)
            FROM transport_master t
            WHERE 1=1
        """);

        appendConditions(countSql, params, dto);

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT 
                t.id,
                t.name,
                t.mobile,
                t.gst,
                t.remarks,
                t.status
            FROM transport_master t
            WHERE 1=1
        """);

        appendConditions(sql, params, dto);

        sql.append("""
                ORDER BY t.%s %s
                LIMIT :pageSize OFFSET :offset
            """.formatted(dto.getSortBy(), dto.getSortDir().toUpperCase()));

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);
        query.setParameter("pageSize", dto.getSize());
        query.setParameter("offset", dto.getPage() * dto.getSize());

        @SuppressWarnings("unchecked")
        List<Object[]> results = (List<Object[]>) query.getResultList();

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> content = new ArrayList<>();
        for (Object[] row : results) {
            if (row[0] != null) {
                Map<String, Object> map = new HashMap<>(10);
                map.put("id", row[0]);
                map.put("name", row[1]);
                map.put("mobile", row[2]);
                map.put("gst", row[3]);
                map.put("remarks", row[4]);
                map.put("status", row[5]);
                content.add(map);
            }
        }
        response.put("content", content);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / dto.getSize()));
        return response;
    }

    private void appendConditions(StringBuilder sql, Map<String, Object> params, TransportMasterDto dto) {
        if (StringUtils.hasText(dto.getSearch())) {
            sql.append(" AND (LOWER(t.name) LIKE LOWER(:search) OR LOWER(t.mobile) LIKE LOWER(:search))");
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }
        sql.append(" AND t.client_id = :clientId");
        params.put("clientId", dto.getClientId());
    }
}


