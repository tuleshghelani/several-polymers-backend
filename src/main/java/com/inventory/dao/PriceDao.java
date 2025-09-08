package com.inventory.dao;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import lombok.RequiredArgsConstructor;

@Repository
@RequiredArgsConstructor
public class PriceDao {
    private final EntityManager entityManager;
    
    @Transactional
    public Map<String, Object> getLastPrices(Long productId, Long customerId) {
        String query = """
                SELECT\s
                    COALESCE(p.purchase_amount, 0) AS last_purchase_price,
                    COALESCE(s.unit_price, p.sale_amount, 0) AS last_sale_price
                FROM\s
                    (SELECT * from product p where p.id = :productId) p
                FULL OUTER JOIN\s
                    (SELECT s.unit_price\s
                     FROM sale s\s
                     JOIN purchase p ON s.purchase_id = p.id
                     WHERE s.customer_id = :customerId AND p.product_id = :productId
                     ORDER BY s.id DESC\s
                     LIMIT 1) s
                ON TRUE
            """;
            
        try {
            Object[] result = (Object[]) entityManager.createNativeQuery(query)
                .setParameter("productId", productId)
                .setParameter("customerId", customerId)
                .getSingleResult();
                
            Map<String, Object> prices = new HashMap<>();
            prices.put("lastPurchasePrice", result[0]);
            prices.put("lastSalePrice", result[1]);
            
            return prices;
        } catch (NoResultException e) {
            e.printStackTrace();
            Map<String, Object> emptyPrices = new HashMap<>();
            emptyPrices.put("lastPurchasePrice", BigDecimal.ZERO);
            emptyPrices.put("lastSalePrice", BigDecimal.ZERO);
            return emptyPrices;
        }
    }
} 