package com.inventory.dao;

import com.inventory.dto.RegisterRequest;
import com.inventory.exception.ValidationException;
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
public class UserDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> searchUsers(RegisterRequest dto) {
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("SELECT COUNT(u.id) FROM user_master u WHERE 1=1");
        appendSearchConditions(countSql, params, dto);

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT 
                u.id, u.first_name, u.last_name, u.phone_number, u.email,
                u.status, u.roles, u.created_at, u.updated_at, u.client_id
            FROM user_master u
            WHERE 1=1
        """);

        appendSearchConditions(sql, params, dto);
        sql.append("""
            ORDER BY u.created_at DESC
            LIMIT :pageSize OFFSET :offset
        """);

        Query query = entityManager.createNativeQuery(sql.toString());
        setQueryParameters(query, params, dto);

        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, dto.getSize());
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, RegisterRequest dto) {
        if (StringUtils.hasText(dto.getSearchTerm())) {
            sql.append(" AND (LOWER(u.first_name) LIKE LOWER(:searchTerm) OR " +
                      "LOWER(u.last_name) LIKE LOWER(:searchTerm) OR " +
                      "LOWER(u.phone_number) LIKE LOWER(:searchTerm) OR " +
                      "LOWER(u.email) LIKE LOWER(:searchTerm))");
            params.put("searchTerm", "%" + dto.getSearchTerm().trim() + "%");
        }
        
        if (StringUtils.hasText(dto.getStatus())) {
            sql.append(" AND u.status = :status");
            params.put("status", dto.getStatus().trim());
        }
        
        if (dto.getClientId() != null) {
            sql.append(" AND u.client_id = :clientId");
            params.put("clientId", dto.getClientId());
        }
    }

    private void setQueryParameters(Query query, Map<String, Object> params, RegisterRequest dto) {
        params.forEach(query::setParameter);
        query.setParameter("pageSize", dto.getSize());
        query.setParameter("offset", dto.getPage() * dto.getSize());
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, int pageSize) {
        List<Map<String, Object>> users = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", row[0]);
            user.put("firstName", row[1]);
            user.put("lastName", row[2]);
            user.put("phoneNumber", row[3]);
            user.put("email", row[4]);
            user.put("status", row[5]);
            user.put("roles", row[6]);
            user.put("createdAt", row[7]);
            user.put("updatedAt", row[8]);
            user.put("clientId", row[9]);
            users.add(user);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", users);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / pageSize));

        return response;
    }

    public Map<String, Object> getUserDetail(Long userId) {
        String query = """
            SELECT 
                u.id,
                u.first_name,
                u.last_name,
                u.phone_number,
                u.email,
                u.status,
                u.roles,
                u.created_at,
                u.updated_at,
                u.client_id
            FROM user_master u
            WHERE u.id = :userId
        """;
        
        Query nativeQuery = entityManager.createNativeQuery(query);
        nativeQuery.setParameter("userId", userId);
        
        Object[] result = (Object[]) nativeQuery.getSingleResult();
        
        if (result == null) {
            throw new ValidationException("User not found");
        }
        
        Map<String, Object> user = new HashMap<>();
        int index = 0;
        user.put("id", result[index++]);
        user.put("firstName", result[index++]);
        user.put("lastName", result[index++]);
        user.put("phoneNumber", result[index++]);
        user.put("email", result[index++]);
        user.put("status", result[index++]);
        user.put("roles", result[index++]);
        user.put("createdAt", result[index++]);
        user.put("updatedAt", result[index++]);
        user.put("clientId", result[index++]);
        return user;
    }

    public List<Map<String, Object>> getAllUsers(Long clientId) {
        String sql = """
            SELECT 
                u.id,
                u.first_name,
                u.last_name,
                u.phone_number,
                u.email,
                u.status
            FROM user_master u
            WHERE u.status = 'A' AND u.client_id = :clientId
            ORDER BY u.first_name ASC
        """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("clientId", clientId);
        
        List<Object[]> results = query.getResultList();
        List<Map<String, Object>> users = new ArrayList<>();
        
        for (Object[] row : results) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", row[0]);
            user.put("firstName", row[1]);
            user.put("lastName", row[2]);
            user.put("phoneNumber", row[3]);
            user.put("email", row[4]);
            user.put("status", row[5]);
            users.add(user);
        }
        
        return users;
    }
} 