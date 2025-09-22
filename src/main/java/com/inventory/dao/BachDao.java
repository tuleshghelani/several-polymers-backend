package com.inventory.dao;

import com.inventory.dto.BachDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.*;

@Repository
public class BachDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> search(BachDto dto) {
        StringBuilder countSql = new StringBuilder("SELECT COUNT(b.id) FROM bach b WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        appendConditions(countSql, params, dto);

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT b.id, b.date, b.shift, b.name, b.resign_bag_use, b.resign_bag_opening_stock,
                   b.cpw_bag_use, b.cpw_bag_opening_stock, b.machine_id
            FROM bach b
            WHERE 1=1
        """);
        appendConditions(sql, params, dto);
        sql.append(" ORDER BY b." + dto.getSortBy() + " " + dto.getSortDir().toUpperCase());
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
            map.put("date", row[1]);
            map.put("shift", row[2]);
            map.put("name", row[3]);
            map.put("resignBagUse", row[4]);
            map.put("resignBagOpeningStock", row[5]);
            map.put("cpwBagUse", row[6]);
            map.put("cpwBagOpeningStock", row[7]);
            map.put("machineId", row[8]);
            content.add(map);
        }
        response.put("content", content);
        response.put("totalRecords", totalRecords);
        response.put("pageSize", dto.getSize());
        return response;
    }

    private void appendConditions(StringBuilder sql, Map<String, Object> params, BachDto dto) {
        sql.append(" AND b.client_id = :clientId");
        params.put("clientId", dto.getClientId());

        if (dto.getDate() != null) {
            sql.append(" AND b.date = :date");
            params.put("date", dto.getDate());
        }
        if (StringUtils.hasText(dto.getShift())) {
            sql.append(" AND b.shift = :shift");
            params.put("shift", dto.getShift().trim());
        }
        if (dto.getMachineId() != null) {
            sql.append(" AND b.machine_id = :machineId");
            params.put("machineId", dto.getMachineId());
        }
    }
}


