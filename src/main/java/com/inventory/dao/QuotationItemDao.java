package com.inventory.dao;

import com.inventory.dto.request.QuotationItemRequestDto;
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
public class QuotationItemDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> search(QuotationItemRequestDto dto, Long clientId) {
        StringBuilder countSql = new StringBuilder();
        Map<String, Object> params = new HashMap<>();

        countSql.append("SELECT COUNT(qi.id) FROM quotation_items qi WHERE 1=1");
        appendSearchConditions(countSql, params, dto, clientId);

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        StringBuilder sql = new StringBuilder();
        sql.append("""
            SELECT 
                qi.id, qi.quotation_id, qi.product_id, qi.quantity, qi.unit_price,
                qi.discount_price, qi.quotation_discount_percentage, qi.quotation_discount_amount,
                qi.quotation_discount_price, qi.tax_percentage, qi.tax_amount, qi.final_price,
                qi.client_id, qi.brand_id, qi.number_of_roll, qi.created_roll, qi.weight_per_roll,
                qi.remarks, qi.is_production, qi.quotation_item_status,
                p.name as product_name, b.name as brand_name
            FROM quotation_items qi
            left join product p on p.id = qi.product_id
            left join brand b on b.id = qi.brand_id
            WHERE 1=1
        """);

        appendSearchConditions(sql, params, dto, clientId);
        sql.append(" ORDER BY qi." + dto.getSortBy() + " " + dto.getSortDir().toUpperCase());
        sql.append(" LIMIT :pageSize OFFSET :offset");

        Query query = entityManager.createNativeQuery(sql.toString());
        params.forEach(query::setParameter);
        query.setParameter("pageSize", dto.getSize());
        query.setParameter("offset", dto.getPage() * dto.getSize());

        @SuppressWarnings("unchecked")
        List<Object[]> rows = query.getResultList();
        List<Map<String, Object>> content = new ArrayList<>();
        for (Object[] r : rows) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", r[0]);
            m.put("quotationId", r[1]);
            m.put("productId", r[2]);
            m.put("quantity", r[3]);
            m.put("unitPrice", r[4]);
            m.put("discountPrice", r[5]);
            m.put("quotationDiscountPercentage", r[6]);
            m.put("quotationDiscountAmount", r[7]);
            m.put("quotationDiscountPrice", r[8]);
            m.put("taxPercentage", r[9]);
            m.put("taxAmount", r[10]);
            m.put("finalPrice", r[11]);
            m.put("clientId", r[12]);
            m.put("brandId", r[13]);
            m.put("numberOfRoll", r[14]);
            m.put("createdRoll", r[15]);
            m.put("weightPerRoll", r[16]);
            m.put("remarks", r[17]);
            m.put("isProduction", r[18]);
            m.put("quotationItemStatus", r[19]);
            m.put("productName", r[20]);
            m.put("brandName", r[21]);
            content.add(m);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", content);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / dto.getSize()));
        return response;
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params,
                                        QuotationItemRequestDto dto, Long clientId) {
        sql.append(" AND qi.client_id = :clientId");
        params.put("clientId", clientId);

        if (dto.getIsProduction() != null) {
            sql.append(" AND qi.is_production = :isProduction");
            params.put("isProduction", dto.getIsProduction());
        }
        if (StringUtils.hasText(dto.getQuotationItemStatus())) {
            sql.append(" AND qi.quotation_item_status = :status");
            params.put("status", dto.getQuotationItemStatus().trim());
        }
        if (dto.getBrandId() != null) {
            sql.append(" AND qi.brand_id = :brandId");
            params.put("brandId", dto.getBrandId());
        }
    }
}


