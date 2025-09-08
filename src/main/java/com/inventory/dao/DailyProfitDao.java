package com.inventory.dao;

import com.inventory.dto.DailyProfitDto;
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
public class DailyProfitDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> searchDailyProfits(DailyProfitDto dto) {
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("""
            SELECT COUNT(dp.id)
            FROM (SELECT * FROM daily_profit WHERE client_id = :clientId) dp
            JOIN (SELECT * FROM sale WHERE client_id = :clientId) s ON dp.sale_id = s.id
            JOIN (SELECT * FROM purchase WHERE client_id = :clientId) p ON s.purchase_id = p.id
            JOIN (SELECT * FROM product WHERE client_id = :clientId) pr ON p.product_id = pr.id
        """);
        params.put("clientId", dto.getClientId());

        appendSearchConditions(countSql, params, dto);
        
        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT 
                dp.id,
                dp.purchase_amount,
                dp.sale_amount,
                dp.gross_profit,
                dp.other_expenses,
                dp.net_profit,
                dp.profit_date,
                s.invoice_number as sale_invoice,
                pr.name as product_name,
                pr.id as product_id
            FROM (SELECT * FROM daily_profit WHERE client_id = :clientId) dp
            JOIN (SELECT * FROM sale WHERE client_id = :clientId) s ON dp.sale_id = s.id
            JOIN (SELECT * FROM purchase WHERE client_id = :clientId) p ON s.purchase_id = p.id
            JOIN (SELECT * FROM product WHERE client_id = :clientId) pr ON p.product_id = pr.id
            WHERE 1=1
        """);
        params.put("clientId", dto.getClientId());

        appendSearchConditions(sql, params, dto);
        sql.append("""
                ORDER BY dp.%s %s
                LIMIT :pageSize OFFSET :offset
            """.formatted(dto.getSortBy(), dto.getSortDir().toUpperCase()));

        Query query = entityManager.createNativeQuery(sql.toString());
        setQueryParameters(query, params, dto);

        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, dto);
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, DailyProfitDto dto) {
        if (StringUtils.hasText(dto.getSearch())) {
            sql.append("""
                AND (LOWER(pr.name) LIKE LOWER(:search)
                OR LOWER(s.invoice_number) LIKE LOWER(:search))
            """);
            params.put("search", "%" + dto.getSearch().trim() + "%");
        }

        if (dto.getStartDate() != null) {
            sql.append(" AND dp.profit_date >= :startDate");
            params.put("startDate", dto.getStartDate());
        }

        if (dto.getEndDate() != null) {
            sql.append(" AND dp.profit_date <= :endDate");
            params.put("endDate", dto.getEndDate());
        }

        if (dto.getProductId() != null) {
            sql.append(" AND pr.id = :productId");
            params.put("productId", dto.getProductId());
        }
    }

    private void setQueryParameters(Query query, Map<String, Object> params, DailyProfitDto dto) {
        params.forEach(query::setParameter);
        query.setParameter("pageSize", dto.getSize());
        query.setParameter("offset", dto.getPage() * dto.getSize());
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, DailyProfitDto dto) {
        List<Map<String, Object>> profits = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> profit = new HashMap<>();
            profit.put("id", row[0]);
            profit.put("purchaseAmount", row[1]);
            profit.put("saleAmount", row[2]);
            profit.put("grossProfit", row[3]);
            profit.put("otherExpenses", row[4]);
            profit.put("netProfit", row[5]);
            profit.put("profitDate", row[6]);
            profit.put("saleInvoice", row[7]);
            profit.put("productName", row[8]);
            profit.put("productId", row[9]);
            profits.add(profit);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", profits);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / dto.getSize()));

        return response;
    }
} 