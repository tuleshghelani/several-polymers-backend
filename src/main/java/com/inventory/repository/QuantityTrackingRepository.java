package com.inventory.repository;

import com.inventory.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuantityTrackingRepository extends JpaRepository<Product, Long> {
    @Query(value = """
        SELECT COALESCE(SUM(p.remaining_quantity), 0)
        FROM product p 
        WHERE p.category_id = :categoryId and p.remaining_quantity !=0
        """, nativeQuery = true)
    Integer getCategoryTotalQuantity(@Param("categoryId") Long categoryId);
    
    @Query(value = """
        SELECT COALESCE(SUM(pu.remaining_quantity), 0)
        FROM purchase pu 
        WHERE pu.product_id = :productId and pu.remaining_quantity !=0
        """, nativeQuery = true)
    Integer getProductTotalQuantity(@Param("productId") Long productId);
} 