package com.inventory.dao;

import com.inventory.dto.FollowUpDto;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.util.*;

@Repository
public class FollowUpDao {
    @PersistenceContext
    private EntityManager entityManager;

    public List<Map<String, Object>> getFollowUps(FollowUpDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("dto must not be null");
        }
        StringBuilder sql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        sql.append("""
            SELECT 
                f.id,
                f.follow_up_status,
                f.next_action_date,
                f.description,
                f.enquiry_id,
                f.client_id,
                e.name as enquiry_name
            FROM follow_up f
            LEFT JOIN enquiry_master e ON f.enquiry_id = e.id
            WHERE 1=1
        """);

        if (StringUtils.hasText(dto.getSearch())) {
            sql.append(" AND (LOWER(f.description) LIKE LOWER(:search) OR LOWER(f.follow_up_status) LIKE LOWER(:search) OR LOWER(e.name) LIKE LOWER(:search))");
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }

        sql.append(" AND f.client_id = :clientId");
        params.put("clientId", dto.getClientId());

        sql.append(" ORDER BY f.id DESC");

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);

        @SuppressWarnings("unchecked")
        List<Object[]> results = (List<Object[]>) query.getResultList();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (Object[] row : results) {
            if (row[0] != null) {
                Map<String, Object> map = new HashMap<>(8);
                map.put("id", row[0]);
                map.put("followUpStatus", row[1]);
                map.put("nextActionDate", row[2]);
                map.put("description", row[3]);
                map.put("enquiryId", row[4]);
                map.put("clientId", row[5]);
                map.put("enquiryName", row[6]);
                rows.add(map);
            }
        }
        return rows;
    }

    public Map<String, Object> searchFollowUps(FollowUpDto dto) {
        if (dto == null) {
            throw new IllegalArgumentException("dto must not be null");
        }
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("""
            SELECT COUNT(f.id)
            FROM follow_up f
            LEFT JOIN enquiry_master e ON f.enquiry_id = e.id
            WHERE 1=1
        """);

        appendConditions(countSql, params, dto);

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT 
                f.id,
                f.follow_up_status,
                f.next_action_date,
                f.description,
                f.enquiry_id,
                f.client_id,
                e.name as enquiry_name
            FROM follow_up f
            LEFT JOIN enquiry_master e ON f.enquiry_id = e.id
            WHERE 1=1
        """);

        appendConditions(sql, params, dto);

        sql.append("""
                ORDER BY f.%s %s
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
                Map<String, Object> map = new HashMap<>(8);
                map.put("id", row[0]);
                map.put("followUpStatus", row[1]);
                map.put("nextActionDate", row[2]);
                map.put("description", row[3]);
                map.put("enquiryId", row[4]);
                map.put("clientId", row[5]);
                map.put("enquiryName", row[6]);
                content.add(map);
            }
        }
        response.put("content", content);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / dto.getSize()));
        return response;
    }

    private void appendConditions(StringBuilder sql, Map<String, Object> params, FollowUpDto dto) {
        if (StringUtils.hasText(dto.getSearch())) {
            sql.append(" AND (LOWER(f.description) LIKE LOWER(:search) OR LOWER(f.follow_up_status) LIKE LOWER(:search) OR LOWER(e.name) LIKE LOWER(:search))");
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }
        if (dto.getEnquiryId() != null) {
            sql.append(" AND f.enquiry_id = :enquiryId");
            params.put("enquiryId", dto.getEnquiryId());
        }
        if (dto.getClientId() != null) {
            sql.append(" AND f.client_id = :clientId");
            params.put("clientId", dto.getClientId());
        }
        if (dto.getFollowUpStatus() != null) {
            sql.append(" AND f.follow_up_status = :followUpStatus");
            params.put("followUpStatus", dto.getFollowUpStatus());
        }
        if (dto.getStartDate() != null) {
            sql.append(" AND f.next_action_date >= :startDate");
            params.put("startDate", dto.getStartDate().atStartOfDay());
        }
        if (dto.getEndDate() != null) {
            sql.append(" AND f.next_action_date <= :endDate");
            params.put("endDate", dto.getEndDate().atStartOfDay().plusDays(1));
        }
    }
}
