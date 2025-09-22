package com.inventory.dao;

import com.inventory.dto.MachineDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.*;

@Repository
public class MachineMasterDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> search(MachineDto dto) {
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("SELECT COUNT(m.id) FROM machine_master m WHERE 1=1");
        appendConditions(countSql, params, dto);

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT m.id, m.name, m.status
            FROM machine_master m
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
            map.put("name", row[1]);
            map.put("status", row[2]);
            content.add(map);
        }
        response.put("content", content);
        response.put("totalRecords", totalRecords);
        response.put("pageSize", dto.getSize());
        return response;
    }

    private void appendConditions(StringBuilder sql, Map<String, Object> params, MachineDto dto) {
        sql.append(" AND m.client_id = :clientId");
        params.put("clientId", dto.getClientId());

        if (StringUtils.hasText(dto.getSearch())) {
            sql.append(" AND LOWER(m.name) LIKE LOWER(:search)");
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }
        if (StringUtils.hasText(dto.getStatus())) {
            sql.append(" AND m.status = :status");
            params.put("status", dto.getStatus().trim());
        }
    }
}


