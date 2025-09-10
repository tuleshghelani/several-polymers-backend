package com.inventory.dao;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.inventory.entity.Quotation;
import org.springframework.stereotype.Repository;

import com.inventory.dto.QuotationDto;
import com.inventory.exception.ValidationException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

import java.time.LocalDate;

@Repository
public class QuotationDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Map<String, Object> searchQuotations(QuotationDto searchParams) {
        Map<String, Object> params = new HashMap<>();
        params.put("clientId", searchParams.getClientId());

        StringBuilder nativeQuery = buildNativeQuery();
        StringBuilder conditions = buildSearchConditions(params, searchParams);

        // Count Query
        String countSql = buildCountQuery(nativeQuery.toString(), conditions.toString());
        Query countQuery = entityManager.createNativeQuery(countSql);
        setQueryParameters(countQuery, params, null);
        Long totalRecords = ((Number) countQuery.getSingleResult()).longValue();

        // Main Query with pagination
        String mainSql = buildMainQuery(nativeQuery.toString(), conditions.toString(), searchParams);
        Query query = entityManager.createNativeQuery(mainSql)
                .setHint(org.hibernate.annotations.QueryHints.FETCH_SIZE, 100)
                .setHint(org.hibernate.annotations.QueryHints.CACHEABLE, true);
        setQueryParameters(query, params, searchParams);

        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, searchParams.getPerPageRecord());
    }

    private StringBuilder buildNativeQuery() {
        return new StringBuilder("""
            FROM (SELECT * FROM quotation q WHERE q.client_id = :clientId) q
            LEFT JOIN (SELECT * FROM customer c WHERE c.client_id = :clientId) c 
            ON q.customer_id = c.id
            WHERE 1=1
            """);
    }

    private String buildMainQuery(String nativeQuery, String conditions, QuotationDto searchParams) {
        return new StringBuilder()
                .append("SELECT q.id, q.quote_number, q.quote_date,")
                .append(" q.total_amount, q.status, COALESCE(c.name, q.customer_name, '') as customer_name, ")
                .append(" q.valid_until, q.remarks, q.terms_conditions ")
                .append(nativeQuery)
                .append(conditions)
                .append(" ORDER BY q.").append(searchParams.getSortBy()).append(" ")
                .append(searchParams.getSortDir())
                .append(" LIMIT :pageSize OFFSET :offset")
                .toString();
    }

    private String buildCountQuery(String nativeQuery, String conditions) {
        return new StringBuilder()
                .append("SELECT COUNT(*) ")
                .append(nativeQuery)
                .append(conditions)
                .toString();
    }

    private StringBuilder buildSearchConditions(Map<String, Object> params, QuotationDto searchParams) {
        StringBuilder conditions = new StringBuilder();

        if (searchParams.getSearch() != null && !searchParams.getSearch().trim().isEmpty()) {
            conditions.append(" AND (q.quote_number LIKE :search OR c.name LIKE :search)");
            params.put("search", "%" + searchParams.getSearch().trim() + "%");
        }
        if (searchParams.getStartDate() != null) {
            conditions.append(" AND q.quote_date >= :startDate");
            params.put("startDate", searchParams.getStartDate());
        }
        if (searchParams.getEndDate() != null) {
            conditions.append(" AND q.quote_date <= :endDate");
            params.put("endDate", searchParams.getEndDate());
        }
        if (searchParams.getStatus() != null) {
            conditions.append(" AND q.status = :status");
            params.put("status", searchParams.getStatus());
        }
        if(searchParams.getCustomerId() != null) {
            conditions.append(" AND q.customer_id = :customerId");
            params.put("customerId", searchParams.getCustomerId());
        }

        return conditions;
    }

    private void setQueryParameters(Query query, Map<String, Object> params, QuotationDto searchParams) {
        params.forEach((key, value) -> query.setParameter(key, value));
        if (searchParams != null) {
            query.setParameter("pageSize", searchParams.getPerPageRecord());
            query.setParameter("offset", searchParams.getCurrentPage() * searchParams.getPerPageRecord());
        }
    }

    private Map<String, Object> transformResults(List<Object[]> results, Long totalRecords, Integer pageSize) {
        List<Map<String, Object>> quotations = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> quotation = new HashMap<>();
            int index = 0;
            quotation.put("id", row[index++]);
            quotation.put("quoteNumber", row[index++]);
            quotation.put("quoteDate", row[index++]);
            quotation.put("totalAmount", row[index++]);
            quotation.put("status", row[index++]);
            quotation.put("customerName", row[index++]);
            quotation.put("validUntil", row[index++]);
            quotation.put("remarks", row[index++]);
            quotation.put("termsConditions", row[index++]);
            quotations.add(quotation);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", quotations);
        response.put("totalElements", totalRecords);
        response.put("pageSize", pageSize);
        response.put("totalPages", (totalRecords + pageSize - 1) / pageSize);

        return response;
    }

    public Map<String, Object> getQuotationDetail(QuotationDto request) {
        StringBuilder sql = new StringBuilder("""
            SELECT 
                q.id, q.quote_number, q.quote_date, q.valid_until,
                q.total_amount, q.status, q.remarks, q.terms_conditions,
                c.id as customer_id, q.customer_name, q.contact_number,
                q.tax_amount as quotation_tax_amount, q.quotation_discount_percentage, 
                q.quotation_discount_amount,
                q.transport_master_id, q.case_number, q.packaging_and_forwading_charges,
                qi.id as item_id, qi.quantity, qi.unit_price,
                qi.tax_percentage, qi.tax_amount, qi.final_price,
                p.id as product_id, p.name as product_name, 
                qi.discount_price, qi.quotation_discount_price,
                b.id as brand_id, b.name as brand_name,
                qi.number_of_roll, qi.weight_per_roll, qi.remarks
            FROM (select * from quotation q where q.client_id = :clientId and q.id = :quotationId) q
            LEFT JOIN (select * from customer c where c.client_id = :clientId) c ON q.customer_id = c.id
            LEFT JOIN (select * from quotation_items qi where qi.client_id = :clientId) qi ON q.id = qi.quotation_id
            LEFT JOIN (select * from product p where p.client_id = :clientId) p ON qi.product_id = p.id
            LEFT JOIN (select * from brand b where b.client_id = :clientId) b ON qi.brand_id = b.id
            WHERE q.id = :quotationId 
        """);

        Query query = entityManager.createNativeQuery(sql.toString());
        query.setParameter("quotationId", request.getId());
        query.setParameter("clientId", request.getClientId());

        List<Object[]> results = query.getResultList();
        return transformDetailResults(results);
    }

    private Map<String, Object> transformDetailResults(List<Object[]> results) {
        if (results.isEmpty()) {
            throw new ValidationException("Quotation not found");
        }

        Map<String, Object> quotation = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();

        // Set quotation details from first row
        Object[] firstRow = results.get(0);
        int index = 0;
        quotation.put("id", firstRow[index++]);
        quotation.put("quoteNumber", firstRow[index++]);
        quotation.put("quoteDate", firstRow[index++]);
        quotation.put("validUntil", firstRow[index++]);
        quotation.put("totalAmount", firstRow[index++]);
        quotation.put("status", firstRow[index++]);
        quotation.put("remarks", firstRow[index++]);
        quotation.put("termsConditions", firstRow[index++]);
        quotation.put("customerId", firstRow[index++]);
        quotation.put("customerName", firstRow[index++]);
        quotation.put("contactNumber", firstRow[index++]);
        quotation.put("quotationTaxAmount", firstRow[index++]);
        quotation.put("quotationDiscountPercentage", firstRow[index++]);
        quotation.put("quotationDiscountAmount", firstRow[index++]);
        quotation.put("transportMasterId", firstRow[index++]);
        quotation.put("caseNumber", firstRow[index++]);
        quotation.put("packagingAndForwadingCharges", firstRow[index++]);

        // Process items
        for (Object[] row : results) {
            index = 17;
            Map<String, Object> item = new HashMap<>();
            item.put("id", row[index++]);
            item.put("quantity", row[index++]);
            item.put("unitPrice", row[index++]);
            item.put("taxPercentage", row[index++]);
            item.put("taxAmount", row[index++]);
            item.put("finalPrice", row[index++]);
            item.put("productId", row[index++]);
            item.put("productName", row[index++]);
            item.put("discountPrice", row[index++]);
            item.put("quotationDiscountPrice", row[index++]);
            item.put("brandId", row[index++]);
            item.put("brandName", row[index++]);
            item.put("numberOfRoll", row[index++]);
            item.put("weightPerRoll", row[index++]);
            item.put("remarks", row[index++]);
            items.add(item);
        }

        quotation.put("items", items);
        return quotation;
    }

    public List<Quotation> findByClientIdAndDateRange(Long clientId, LocalDate startDate, LocalDate endDate) {
        String sql = """
            SELECT * FROM quotation q 
            WHERE q.client_id = :clientId 
            AND q.quote_date BETWEEN :startDate AND :endDate
        """;

        Query query = entityManager.createNativeQuery(sql, Quotation.class);
        query.setParameter("clientId", clientId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }
}
