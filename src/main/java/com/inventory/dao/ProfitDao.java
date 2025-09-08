package com.inventory.dao;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

@Repository
public class ProfitDao {
    @PersistenceContext
    private EntityManager entityManager;
    
    public Map<String, Object> getDailyProfitSummary(OffsetDateTime startDate, OffsetDateTime endDate, Long clientId) {
        String sql = """
            SELECT 
                DATE(dp.profit_date) as date,
                SUM(dp.gross_profit) as gross_profit,
                SUM(dp.other_expenses) as total_expenses,
                SUM(dp.net_profit) as net_profit
            FROM (SELECT * FROM daily_profit WHERE client_id = :clientId) dp
            WHERE dp.profit_date BETWEEN :startDate AND :endDate
            GROUP BY DATE(dp.profit_date)
            ORDER BY DATE(dp.profit_date)
        """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("clientId", clientId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        
        List<Object[]> results = query.getResultList();
        
        // Transform results
        List<Map<String, Object>> profits = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> profit = new HashMap<>();
            profit.put("date", row[0]);
            profit.put("grossProfit", row[1]);
            profit.put("totalExpenses", row[2]);
            profit.put("netProfit", row[3]);
            profits.add(profit);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", profits);
        response.put("totalElements", profits.size());
        
        return response;
    }
    
    public Map<String, Object> getProductWiseProfitSummary(OffsetDateTime startDate, OffsetDateTime endDate, Long clientId) {
        String sql = """
            SELECT 
                p.id as product_id,
                p.name as product_name,
                SUM(dp.gross_profit) as gross_profit,
                SUM(dp.other_expenses) as total_expenses,
                SUM(dp.net_profit) as net_profit
            FROM (SELECT * FROM daily_profit WHERE client_id = :clientId) dp
            JOIN (SELECT * FROM sale WHERE client_id = :clientId) s ON dp.sale_id = s.id
            JOIN (SELECT * FROM purchase WHERE client_id = :clientId) pu ON s.purchase_id = pu.id
            JOIN (SELECT * FROM product WHERE client_id = :clientId) p ON pu.product_id = p.id
            WHERE dp.profit_date BETWEEN :startDate AND :endDate
            GROUP BY p.id, p.name
            ORDER BY SUM(dp.net_profit) DESC
        """;
        
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("clientId", clientId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);
        
        List<Object[]> results = query.getResultList();
        
        // Transform results
        List<Map<String, Object>> profits = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> profit = new HashMap<>();
            profit.put("productId", row[0]);
            profit.put("productName", row[1]);
            profit.put("grossProfit", row[2]);
            profit.put("totalExpenses", row[3]);
            profit.put("netProfit", row[4]);
            profits.add(profit);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", profits);
        response.put("totalElements", profits.size());
        
        return response;
    }
}