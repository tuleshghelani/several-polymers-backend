package com.inventory.repository;

import com.inventory.entity.PurchaseItem;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PurchaseItemRepository extends JpaRepository<PurchaseItem, Long> {
//    @Query(value = """
//        SELECT pi FROM PurchaseItem pi
//        JOIN FETCH pi.product
//        WHERE pi.purchase.id = :purchaseId
//        """)
    @QueryHints(@QueryHint(name = org.hibernate.jpa.QueryHints.HINT_FETCH_SIZE, value = "100"))
    List<PurchaseItem> findByPurchaseId(@Param("purchaseId") Long purchaseId);

    @Modifying
    @Query("DELETE FROM PurchaseItem pi WHERE pi.purchase.id = :purchaseId")
    void deleteByPurchaseId(@Param("purchaseId") Long purchaseId);
}