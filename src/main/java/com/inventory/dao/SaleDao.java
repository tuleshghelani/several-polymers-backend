package com.inventory.dao;

import com.inventory.dto.SaleDto;
import com.inventory.entity.Sale;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Repository
public class SaleDao {
    @PersistenceContext
    private EntityManager entityManager;

    public Page<Map<String, Object>> searchSales(SaleDto dto) {
        try {
            StringBuilder countQuery = new StringBuilder();
            StringBuilder actualQuery = new StringBuilder();
            StringBuilder nativeQuery = new StringBuilder();
            Map<String, Object> params = new HashMap<>();

            actualQuery.append("""
                SELECT 
                    s.id, s.total_sale_amount, 
                    s.sale_date, s.invoice_number, c.name as customer_name, s.is_black,
                    s.transport_master_id, s.case_number, s.reference_name
                """);

            countQuery.append("SELECT COUNT(*) ");

            nativeQuery.append("""
                FROM (select * from sale s where s.client_id = :clientId) s 
                LEFT JOIN (select * from customer c where c.client_id = :clientId) c ON s.customer_id = c.id
                WHERE 1=1
                """);
            params.put("clientId", dto.getClientId());

            appendSearchConditions(nativeQuery, params, dto);

            countQuery.append(nativeQuery);
            nativeQuery.append(" ORDER BY s.id DESC LIMIT :perPageRecord OFFSET :offset");
            actualQuery.append(nativeQuery);

            Pageable pageable = PageRequest.of(dto.getCurrentPage(), dto.getPerPageRecord());
            Query countQueryObj = entityManager.createNativeQuery(countQuery.toString());
            Query query = entityManager.createNativeQuery(actualQuery.toString());

            setQueryParameters(query, countQueryObj, params, dto);

            Long totalCount = ((Number) countQueryObj.getSingleResult()).longValue();
            @SuppressWarnings("unchecked")
            List<Object[]> results = query.getResultList();
            List<Map<String, Object>> sales = transformResults(results);

            return new PageImpl<>(sales, pageable, totalCount);
        } catch (Exception e) {
            e.printStackTrace();
            return new PageImpl<>(new ArrayList<>(), 
                PageRequest.of(dto.getCurrentPage(), dto.getPerPageRecord()), 0L);
        }
    }

    private void appendSearchConditions(StringBuilder sql, Map<String, Object> params, SaleDto
            dto) {
        if (!Objects.isNull(dto.getSearch()) && dto.getSearch().trim().length() > 0) {
            sql.append("""
                AND (LOWER(s.invoice_number) LIKE :search)
                """);
            params.put("search", "%" + dto.getSearch().toLowerCase().trim() + "%");
        }
        if(!Objects.isNull(dto.getStartDate())) {
            sql.append("""
                AND (s.sale_date >= :startDate)
                """);
            params.put("startDate", dto.getStartDate());
        }
        if(!Objects.isNull(dto.getEndDate())) {
            sql.append("""
                AND (s.sale_date <= :endDate)
                """);
            params.put("endDate", dto.getEndDate());
        }
        
        if(!Objects.isNull(dto.getCustomerId())) {
            sql.append("""
                AND s.customer_id = :customerId
                """);
            params.put("customerId", dto.getCustomerId());
        }
        if(!Objects.isNull(dto.getTransportMasterId())) {
            sql.append("""
                AND s.transport_master_id = :transportMasterId
                """);
            params.put("transportMasterId", dto.getTransportMasterId());
        }
        if(!Objects.isNull(dto.getCaseNumber()) && dto.getCaseNumber().trim().length() > 0) {
            sql.append("""
                AND LOWER(s.case_number) LIKE :caseNumber
                """);
            params.put("caseNumber", "%" + dto.getCaseNumber().toLowerCase().trim() + "%");
        }
        if(!Objects.isNull(dto.getReferenceName()) && dto.getReferenceName().trim().length() > 0) {
            sql.append("""
                AND LOWER(s.reference_name) LIKE :referenceName
                """);
            params.put("referenceName", "%" + dto.getReferenceName().toLowerCase().trim() + "%");
        }
    }

    private void setQueryParameters(Query query, Query countQuery, Map<String, Object> params, SaleDto dto) {
        params.forEach((key, value) -> {
            query.setParameter(key, value);
            countQuery.setParameter(key, value);
        });

        query.setParameter("perPageRecord", dto.getPerPageRecord());
        query.setParameter("offset", (long) dto.getCurrentPage() * dto.getPerPageRecord());
    }

    private List<Map<String, Object>> transformResults(List<Object[]> results) {
        List<Map<String, Object>> sales = new ArrayList<>();
        for (Object[] row : results) {
            Map<String, Object> sale = new HashMap<>();
            int i = 0;
            sale.put("id", row[i++]);
            sale.put("totalSaleAmount", row[i++]);
            sale.put("saleDate", row[i++]);
            sale.put("invoiceNumber", row[i++]);
            sale.put("customerName", row[i++]);
            sale.put("is_black", row[i++]);
            sale.put("transportMasterId", row[i++]);
            sale.put("caseNumber", row[i++]);
            sale.put("referenceName", row[i++]);
            sales.add(sale);
        }
        return sales;
    }

    public Map<String, Object> getSaleDetail(Long saleId, Long clientId) {
            String sql = """
            SELECT 
                s.id, s.invoice_number, s.sale_date, s.total_sale_amount,
                s.created_at, s.updated_at, s.customer_id, s.created_by, s.is_black,
                s.transport_master_id, s.case_number, s.reference_name,
                s.sale_discount_percentage, s.sale_discount_amount,
                si.id as item_id, si.quantity, si.unit_price, si.discount_percentage,
                si.discount_amount, si.discount_price, si.tax_percentage, si.tax_amount, si.final_price, 
                si.product_id, si.remarks, si.number_of_roll, si.weight_per_roll
            FROM (SELECT * FROM sale WHERE id = :saleId AND client_id = :clientId) s
            LEFT JOIN (SELECT * FROM sale_items si WHERE sale_id = :saleId) si ON s.id = si.sale_id
            WHERE s.id = :saleId
        """;

        Query query = entityManager.createNativeQuery(sql)
            .setParameter("saleId", saleId)
            .setParameter("clientId", clientId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return transformToDetailResponse(results);
    }

    public Map<String, Object> getSalePdfDetail(Long saleId, Long clientId) {
        String sql = """
            SELECT 
                s.id, s.invoice_number, s.sale_date, s.total_sale_amount,
                c.name as customer_name, c.address, c.mobile, c.gst,
                s.sale_discount_percentage, s.sale_discount_amount,
                si.id as item_id, si.quantity, si.unit_price, si.discount_amount,
                si.discount_price, si.tax_percentage, si.tax_amount,
                p.name as product_name, p.tax_percentage,
                si.number_of_roll, si.weight_per_roll,
                s.transport_master_id, s.case_number, s.reference_name,
                tm.name as transport_master_name
            FROM (SELECT * FROM sale WHERE id = :saleId AND client_id = :clientId) s
            LEFT JOIN (SELECT * FROM customer WHERE client_id = :clientId) c ON s.customer_id = c.id
            LEFT JOIN (SELECT * FROM sale_items WHERE sale_id = :saleId) si ON s.id = si.sale_id
            LEFT JOIN (SELECT * FROM product WHERE client_id = :clientId) p ON si.product_id = p.id
            LEFT JOIN (SELECT * FROM transport_master WHERE client_id = :clientId) tm ON s.transport_master_id = tm.id
            WHERE s.id = :saleId
        """;

        Query query = entityManager.createNativeQuery(sql)
                .setParameter("saleId", saleId)
                .setParameter("clientId", clientId);

        @SuppressWarnings("unchecked")
        List<Object[]> results = query.getResultList();
        return transformToPdfDetail(results);
    }

    private Map<String, Object> transformToPdfDetail(List<Object[]> results) {
        if (results.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> response = new HashMap<>();
        Object[] first = results.get(0);

        response.put("id", first[0]);
        response.put("invoiceNumber", first[1]);
        response.put("saleDate", first[2]);
        response.put("totalAmount", first[3] != null ? first[3] : BigDecimal.ZERO);
        response.put("customerName", first[4]);
        response.put("address", first[5]);
        response.put("contactNumber", first[6]);
        response.put("customerGst", first[7]);
        response.put("saleDiscountPercentage", first[8] != null ? first[8] : BigDecimal.ZERO);
        response.put("saleDiscountAmount", first[9] != null ? first[9] : BigDecimal.ZERO);

        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : results) {
            if (row[10] == null) continue; // no item (shifted due to 2 new columns)
            BigDecimal quantity = toBigDecimal(row[11]);
            BigDecimal unitPrice = toBigDecimal(row[12]);
            BigDecimal discountAmount = toBigDecimal(row[13]);
            BigDecimal discountPrice = toBigDecimal(row[14]);
            BigDecimal taxPercentage = toBigDecimal(row[15]);
            BigDecimal taxAmount = toBigDecimal(row[16]);
            String productName = Objects.toString(row[17], "");
            BigDecimal productTaxPercentage = toBigDecimal(row[18]);
            Integer numberOfRoll = row[19] != null ? ((Number) row[19]).intValue() : 0;
            BigDecimal weightPerRoll = toBigDecimal(row[20]);

            BigDecimal price = discountPrice.compareTo(BigDecimal.ZERO) > 0
                    ? discountPrice
                    : unitPrice.multiply(quantity);
            BigDecimal finalPrice = price.add(taxAmount);

            Map<String, Object> item = new HashMap<>();
            item.put("id", row[10]);
            item.put("productName", productName);
            item.put("quantity", quantity);
            item.put("unitPrice", unitPrice);
            item.put("price", price);
            item.put("taxPercentage", taxPercentage);
            item.put("taxAmount", taxAmount);
            item.put("finalPrice", finalPrice);
            item.put("numberOfRoll", numberOfRoll);
            item.put("weightPerRoll", weightPerRoll);
            items.add(item);
        }
        response.put("items", items);

        // transport and references (placed at root for now)
        response.put("transportMasterId", first[21]);
        response.put("transportMasterName", first[24]);
        response.put("caseNumber", first[22]);
        response.put("referenceName", first[23]);

        return response;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) return BigDecimal.ZERO;
        if (value instanceof BigDecimal) return (BigDecimal) value;
        return new BigDecimal(value.toString());
    }

    private Map<String, Object> transformToDetailResponse(List<Object[]> results) {
        if (results.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Object> response = new HashMap<>();
        Object[] firstRow = results.get(0);

        // Set sale details with null checks
        response.put("id", firstRow[0]);
        response.put("invoiceNumber", firstRow[1] != null ? firstRow[1] : "");
        response.put("saleDate", firstRow[2] != null ? firstRow[2] : "");
        response.put("totalAmount", firstRow[3] != null ? firstRow[3] : BigDecimal.ZERO);
        response.put("createdAt", firstRow[4] != null ? firstRow[4] : "");
        response.put("updatedAt", firstRow[5] != null ? firstRow[5] : "");
        response.put("customerId", firstRow[6]);
        response.put("createdBy", firstRow[7]);
        response.put("isBlack", firstRow[8]);
        response.put("transportMasterId", firstRow[9]);
        response.put("caseNumber", firstRow[10]);
        response.put("referenceName", firstRow[11]);
        response.put("saleDiscountPercentage", firstRow[12] != null ? firstRow[12] : BigDecimal.ZERO);
        response.put("saleDiscountAmount", firstRow[13] != null ? firstRow[13] : BigDecimal.ZERO);

        // Set items
        List<Map<String, Object>> items = new ArrayList<>();
        for (Object[] row : results) {
            if (row[14] != null) { // if item exists
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", row[14]);
                itemMap.put("quantity", row[15] != null ? row[15] : 0);
                itemMap.put("unitPrice", row[16] != null ? row[16] : BigDecimal.ZERO);
                itemMap.put("discountPercentage", row[17] != null ? row[17] : 0);
                itemMap.put("discountAmount", row[18] != null ? row[18] : BigDecimal.ZERO);
                itemMap.put("discountPrice", row[19] != null ? row[19] : BigDecimal.ZERO);
                itemMap.put("taxPercentage", row[20] != null ? row[20] : BigDecimal.ZERO);
                itemMap.put("taxAmount", row[21] != null ? row[21] : BigDecimal.ZERO);
                itemMap.put("finalPrice", row[22] != null ? row[22] : BigDecimal.ZERO);
                itemMap.put("productId", row[23]);
                itemMap.put("remarks", row[24] != null ? row[24] : "");
                itemMap.put("numberOfRoll", row[25] != null ? row[25] : 0);
                itemMap.put("weightPerRoll", row[26] != null ? row[26] : BigDecimal.ZERO);
                items.add(itemMap);
            }
        }
        response.put("items", items);

        return response;
    }

    public List<Sale> findByClientIdAndDateRange(Long clientId, LocalDateTime startDate, LocalDateTime endDate) {
        String sql = """
            SELECT * FROM sale s 
            WHERE s.client_id = :clientId 
            AND s.sale_date BETWEEN :startDate AND :endDate
        """;

        Query query = entityManager.createNativeQuery(sql, Sale.class);
        query.setParameter("clientId", clientId);
        query.setParameter("startDate", startDate);
        query.setParameter("endDate", endDate);

        return query.getResultList();
    }
}