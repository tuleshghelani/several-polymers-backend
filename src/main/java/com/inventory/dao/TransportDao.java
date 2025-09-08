package com.inventory.dao;

import com.inventory.dto.SaleDto;
import com.inventory.dto.TransportDto;
import com.inventory.exception.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
@RequiredArgsConstructor
public class TransportDao {
    private final EntityManager entityManager;
    
    public Map<String, Object> searchTransports(TransportDto dto) {
        Map<String, Object> params = new HashMap<>();
        StringBuilder countQuery = new StringBuilder("SELECT COUNT(*) FROM transport t JOIN customer c ON t.customer_id = c.id WHERE t.client_id = :clientId");
        StringBuilder dataQuery = new StringBuilder("""
            SELECT 
                t.id, 
                t.created_at as createdAt, 
                c.name as customerName, 
                c.id as customerId,
                t.total_weight as totalWeight,
                t.total_bags as totalBags
            FROM (SELECT * FROM transport WHERE client_id = :clientId) t
            JOIN (SELECT * FROM customer WHERE client_id = :clientId) c ON t.customer_id = c.id
            WHERE 1=1
        """);
        params.put("clientId", dto.getClientId());
        buildWhereClause(countQuery, dataQuery, params, dto);
        
        // Add sorting
        String sortField = getSortField(dto.getSortBy());
        dataQuery.append(" ORDER BY ").append(sortField).append(" ").append(dto.getSortDir())
                .append(" LIMIT :perPageRecord OFFSET :offset");
        
        // Execute count query
        Query query = entityManager.createNativeQuery(countQuery.toString());
        setParameters(query, params);
        long totalRecords = ((Number) query.getSingleResult()).longValue();
        
        // Execute data query with pagination
        query = entityManager.createNativeQuery(dataQuery.toString());
        params.put("offset", dto.getCurrentPage() * dto.getPerPageRecord());
        params.put("perPageRecord", dto.getPerPageRecord());
        setParameters(query, params);
        
        List<Object[]> results = query.getResultList();
        return transformResults(results, totalRecords, dto);
    }
    
    private void buildWhereClause(StringBuilder countQuery, StringBuilder dataQuery, 
            Map<String, Object> params, TransportDto dto) {
        if (StringUtils.hasText(dto.getSearch())) {
            String search = "%" + dto.getSearch().toLowerCase() + "%";
            countQuery.append(" AND LOWER(c.name) LIKE :search");
            dataQuery.append(" AND LOWER(c.name) LIKE :search");
            params.put("search", search);
        }
        
        if (dto.getStartDate() != null) {
            countQuery.append(" AND t.created_at >= :startDate");
            dataQuery.append(" AND t.created_at >= :startDate");
            params.put("startDate", dto.getStartDate());
        }

        if (dto.getEndDate() != null) {
            countQuery.append(" AND t.created_at <= :endDate");
            dataQuery.append(" AND t.created_at <= :endDate");
            params.put("endDate", dto.getEndDate());
        }
    }

    private void setParameters(Query query, Map<String, Object> params) {
        params.forEach(query::setParameter);
    }

    private Map<String, Object> transformResults(List<Object[]> results, long totalRecords, TransportDto dto) {
        List<Map<String, Object>> transports = new ArrayList<>();

        for (Object[] row : results) {
            Map<String, Object> transport = new HashMap<>();
            transport.put("id", row[0]);
            transport.put("createdAt", row[1]);
            transport.put("customerName", row[2]);
            transport.put("customerId", row[3]);
            transport.put("totalWeight", row[4]);
            transport.put("totalBags", row[5]);
            transports.add(transport);
        }

        Map<String, Object> response = new HashMap<>();
        response.put("content", transports);
        response.put("totalElements", totalRecords);
        response.put("totalPages", (int) Math.ceil((double) totalRecords / dto.getPerPageRecord()));
        
        return response;
    }

    public Map<String, Object> getTransportDetail(Long transportId, Long clientId) {
        String query = """
            SELECT 
                t.id,
                t.created_at as createdAt,
                t.total_weight as totalWeight,
                t.total_bags as totalBags,
                c.id as customerId,
                c.name as customerName
            FROM (SELECT * FROM transport WHERE client_id = :clientId) t
            LEFT JOIN (SELECT * FROM customer WHERE client_id = :clientId) c ON t.customer_id = c.id
            WHERE t.id = :transportId
        """;
        
        Query nativeQuery = entityManager.createNativeQuery(query);
        nativeQuery.setParameter("transportId", transportId);
        nativeQuery.setParameter("clientId", clientId);

        Object[] result = (Object[]) nativeQuery.getSingleResult();
        
        if (result == null) {
            throw new ValidationException("Transport not found");
        }
        
        Map<String, Object> transport = new HashMap<>();
        transport.put("id", result[0]);
        transport.put("createdAt", result[1]);
        transport.put("totalWeight", result[2]);
        transport.put("totalBags", result[3]);
        transport.put("customerId", result[4]);
        transport.put("customerName", result[5]);

        // Get bags with items, purchases and sales
        String detailQuery = """
            SELECT 
                b.id as bag_id,
                b.weight as bag_weight,
                b.number_of_bags as number_of_bags,
                b.total_bag_weight as total_bag_weight,
                ti.id as item_id,
                ti.product_id,
                ti.quantity,
                ti.remarks,
                pur.id as purchase_id,
                pur.unit_price as purchase_unit_price,
                pur.discount as purchase_discount,
                pur.discount_amount as purchase_discount_amount,
                pur.discount_price as purchase_discount_price,
                pur.total_amount as purchase_total_amount,
                s.id as sale_id,
                s.unit_price as sale_unit_price,
                s.discount as sale_discount,
                s.discount_amount as sale_discount_amount,
                s.discount_price as sale_discount_price,
                s.total_amount as sale_total_amount
            FROM transport_bag b
            LEFT JOIN (
                SELECT * FROM transport_items 
                WHERE transport_id = :transportId AND client_id = :clientId
            ) ti ON ti.transport_bag_id = b.id
            LEFT JOIN (
                SELECT * FROM purchase 
                WHERE transport_id = :transportId AND client_id = :clientId
            ) pur ON pur.transport_item_id = ti.id
            LEFT JOIN (
                SELECT * FROM sale 
                WHERE transport_id = :transportId AND client_id = :clientId
            ) s ON s.transport_item_id = ti.id
            WHERE b.transport_id = :transportId
            ORDER BY b.id, ti.id
        """;
        
        Query detailNativeQuery = entityManager.createNativeQuery(detailQuery);
        detailNativeQuery.setParameter("transportId", transportId);
        detailNativeQuery.setParameter("clientId", clientId);

        List<Object[]> detailResults = detailNativeQuery.getResultList();
        Map<Long, Map<String, Object>> bagsMap = new LinkedHashMap<>();
        
        for (Object[] row : detailResults) {
            Long bagId = ((Number) row[0]).longValue();
            int i =4;
            
            Map<String, Object> bag = bagsMap.computeIfAbsent(bagId, k -> {
                Map<String, Object> newBag = new HashMap<>();
                newBag.put("id", row[0]);
                newBag.put("weight", row[1]);
                newBag.put("numberOfBags", row[2]);
                newBag.put("totalBagWeight", row[3]);
                newBag.put("items", new ArrayList<>());
                return newBag;
            });
            
            if (row[4] != null) {  // If there are items
                Map<String, Object> item = new HashMap<>();
                item.put("id", row[i++]);
                item.put("productId", row[i++]);
                Integer numberOfBags = (Integer) row[2];
                Integer quantity = (Integer) row[i++];
                item.put("quantity", BigDecimal.valueOf(quantity).divide(BigDecimal.valueOf(numberOfBags)));
                item.put("totalQuantity", BigDecimal.valueOf(quantity));
                item.put("remarks", row[i++]);
                
                // Add purchase details
                Map<String, Object> purchase = new HashMap<>();
                purchase.put("id", row[i++]);
                purchase.put("unitPrice", row[i++]);
                purchase.put("discount", row[i++]);
                purchase.put("discountAmount", row[i++]);
                purchase.put("discountPrice", row[i++]);
                purchase.put("totalAmount", row[i++]);
                item.put("purchase", purchase);
                
                // Add sale details
                Map<String, Object> sale = new HashMap<>();
                sale.put("id", row[i++]);
                sale.put("unitPrice", row[i++]);
                sale.put("discount", row[i++]);
                sale.put("discountAmount", row[i++]);
                sale.put("discountPrice", row[i++]);
                sale.put("totalAmount", row[i++]);
                item.put("sale", sale);
                
                ((List<Map<String, Object>>) bag.get("items")).add(item);
            }
        }
        
        transport.put("bags", new ArrayList<>(bagsMap.values()));
        
        // Calculate totals
        String totalsQuery = """
            SELECT 
                COALESCE(SUM(pur.total_amount), 0) as total_purchase,
                COALESCE(SUM(s.total_amount), 0) as total_sale,
                COALESCE(SUM(s.total_amount - pur.total_amount), 0) as total_profit
            FROM (
                SELECT id FROM transport_items 
                WHERE transport_id = :transportId AND client_id = :clientId
            ) ti
            LEFT JOIN (
                SELECT transport_item_id, total_amount 
                FROM purchase 
                WHERE transport_id = :transportId AND client_id = :clientId
            ) pur ON pur.transport_item_id = ti.id
            LEFT JOIN (
                SELECT transport_item_id, total_amount 
                FROM sale 
                WHERE transport_id = :transportId AND client_id = :clientId
            ) s ON s.transport_item_id = ti.id
        """;
        
        Query totalsNativeQuery = entityManager.createNativeQuery(totalsQuery);
        totalsNativeQuery.setParameter("transportId", transportId);
        totalsNativeQuery.setParameter("clientId", clientId);
        Object[] totals = (Object[]) totalsNativeQuery.getSingleResult();
        
        Map<String, Object> summary = new HashMap<>();
        summary.put("totalPurchaseAmount", totals[0]);
        summary.put("totalSaleAmount", totals[1]);
        summary.put("totalProfit", totals[2]);
        
        transport.put("summary", summary);
        
        return transport;
    }

    private String getSortField(String sortBy) {
        // Map frontend sort fields to actual entity fields
        return switch (sortBy.toLowerCase()) {
            case "customername" -> "c.name";
            case "totalweight" -> "t.totalWeight";
            case "totalbags" -> "t.totalBags";
            default -> "t." + sortBy;
        };
    }

    public Map<String, Object> getTransportPdfData(Long transportId, Long clientId) {
        String query = """
            SELECT 
                t.id,
                t.created_at as createdAt,
                t.total_weight as totalWeight,
                t.total_bags as totalBags,
                c.id as customerId,
                c.name as customerName,
                c.address as customerAddress,
                c.mobile as customerMobile,
                c.gst as customerGst
            FROM (SELECT * FROM transport WHERE client_id = :clientId) t
            LEFT JOIN (SELECT * FROM customer WHERE client_id = :clientId) c ON t.customer_id = c.id
            WHERE t.id = :transportId
        """;
        
        Query nativeQuery = entityManager.createNativeQuery(query);
        nativeQuery.setParameter("transportId", transportId);
        nativeQuery.setParameter("clientId", clientId);
        Object[] result = (Object[]) nativeQuery.getSingleResult();
        
        if (result == null) {
            throw new ValidationException("Transport not found");
        }
        
        Map<String, Object> transport = new HashMap<>();
        transport.put("id", result[0]);
        transport.put("createdAt", result[1]);
        transport.put("totalWeight", result[2]);
        transport.put("totalBags", result[3]);
        transport.put("customerName", result[5]);
        transport.put("customerAddress", result[6]);
        transport.put("customerMobile", result[7]);
        transport.put("customerGst", result[8]);

        // Get bags with items
        String bagQuery = """
            SELECT 
                b.id as bag_id,
                b.weight as bag_weight,
                b.number_of_bags as number_of_bags,
                b.total_bag_weight as total_bag_weight,
                ti.id as item_id,
                p.id as product_id,
                p.name as product_name,
                ti.quantity,
                ti.remarks
            FROM transport_bag b
            LEFT JOIN transport_items ti ON ti.transport_bag_id = b.id
            LEFT JOIN product p ON ti.product_id = p.id
            WHERE b.transport_id = :transportId
            ORDER BY b.id, ti.id
        """;
        
        Query bagNativeQuery = entityManager.createNativeQuery(bagQuery);
        bagNativeQuery.setParameter("transportId", transportId);
        
        List<Object[]> bagResults = bagNativeQuery.getResultList();
        Map<Long, Map<String, Object>> bagsMap = new HashMap<>();
        
        for (Object[] row : bagResults) {
            Long bagId = ((Number) row[0]).longValue();
            
            Map<String, Object> bag = bagsMap.computeIfAbsent(bagId, k -> {
                Map<String, Object> newBag = new HashMap<>();
                newBag.put("id", row[0]);
                newBag.put("weight", row[1]);
                newBag.put("numberOfBags", row[2]);
                newBag.put("totalBagWeight", row[3]);
                newBag.put("items", new ArrayList<>());
                return newBag;
            });
            
            if (row[4] != null) {  // If there are items
                Map<String, Object> item = new HashMap<>();
                item.put("id", row[4]);
                item.put("productId", row[5]);
                item.put("productName", row[6]);
                item.put("quantity", row[7]);
                item.put("remarks", row[8]);
                
                ((List<Map<String, Object>>) bag.get("items")).add(item);
            }
        }
        
        transport.put("bags", new ArrayList<>(bagsMap.values()));
        return transport;
    }
} 