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
        StringBuilder countSql = new StringBuilder("SELECT COUNT(b.id) FROM batch b WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        appendConditions(countSql, params, dto);

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT b.id, b.date, b.shift, b.name, b.operator, b.resign_bag_use, b.resign_bag_opening_stock,
                   b.cpw_bag_use, b.cpw_bag_opening_stock, b.machine_id
            FROM batch b
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
            map.put("operator", row[4]);
            map.put("resignBagUse", row[5]);
            map.put("resignBagOpeningStock", row[6]);
            map.put("cpwBagUse", row[7]);
            map.put("cpwBagOpeningStock", row[8]);
            map.put("machineId", row[9]);
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
        if (dto.getStartDate() != null) {
            sql.append(" AND b.date >= :startDate");
            params.put("startDate", dto.getStartDate());
        }
        if (dto.getEndDate() != null) {
            sql.append(" AND b.date <= :endDate");
            params.put("endDate", dto.getEndDate());
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

    public List<Long> findBatchIdsForExport(BachDto dto) {
        StringBuilder sql = new StringBuilder("SELECT b.id FROM batch b WHERE 1=1");
        Map<String, Object> params = new HashMap<>();
        appendConditions(sql, params, dto);
        // Default ordering for export
        sql.append(" ORDER BY b." + dto.getSortBy() + " " + dto.getSortDir().toUpperCase());

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) query.getResultList();
        List<Long> result = new ArrayList<>();
        for (Number n : ids) {
            if (n != null) result.add(n.longValue());
        }
        return result;
    }

    /**
     * Fetches all batch data for export in a single query with joins
     * Returns batch data with machine information
     */
    public List<Object[]> findAllBatchesForExport(BachDto dto) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT b.id, b.date, b.shift, b.name, b.operator, b.resign_bag_use, ");
        sql.append("b.resign_bag_opening_stock, b.cpw_bag_use, b.cpw_bag_opening_stock, ");
        sql.append("m.name as machine_name ");
        sql.append("FROM (select * from batch b where 1=1 ");
        Map<String, Object> params = new HashMap<>();
        appendConditions(sql, params, dto);
        sql.append(") b ");
        sql.append("LEFT JOIN machine_master m ON b.machine_id = m.id ");
        sql.append(" ORDER BY b." + dto.getSortBy() + " " + dto.getSortDir().toUpperCase());

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> result = (List<Object[]>) query.getResultList();
        return result;
    }

    /**
     * Fetches all mixer data for given batch IDs in a single query with product information
     */
    public List<Object[]> findAllMixersForExport(List<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return new ArrayList<>();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT m.batch_id, m.quantity, p.name as product_name ");
        sql.append("FROM (select * from mixer where batch_id in (:batchIds)) m ");
        sql.append("LEFT JOIN product p ON m.product_id = p.id ");
        sql.append("ORDER BY m.batch_id, m.id ");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("batchIds", batchIds);

        @SuppressWarnings("unchecked")
        List<Object[]> result = (List<Object[]>) query.getResultList();
        return result;
    }

    /**
     * Fetches all production data for given batch IDs in a single query with product information
     */
    public List<Object[]> findAllProductionsForExport(List<Long> batchIds) {
        if (batchIds == null || batchIds.isEmpty()) {
            return new ArrayList<>();
        }

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT p.batch_id, p.quantity, p.number_of_roll, p.is_wastage, pr.name as product_name ");
        sql.append("FROM (select * from production where batch_id in (:batchIds)) p ");
        sql.append("LEFT JOIN product pr ON p.product_id = pr.id ");
        sql.append("ORDER BY p.batch_id, p.id ");

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("batchIds", batchIds);

        @SuppressWarnings("unchecked")
        List<Object[]> result = (List<Object[]>) query.getResultList();
        return result;
    }
}


