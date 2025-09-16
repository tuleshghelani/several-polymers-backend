package com.inventory.repository;

import com.inventory.entity.QuotationItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuotationItemRepository extends JpaRepository<QuotationItem, Long> {

    @Modifying
    @Query("DELETE FROM QuotationItem qi WHERE qi.quotation.id = :quotationId")
    void deleteByQuotationId(Long quotationId);

    List<QuotationItem> findByQuotationId(Long quotationId);

    @Modifying
    @Query("UPDATE QuotationItem qi SET qi.quotationItemStatus = :status WHERE qi.id = :id")
    int updateQuotationItemStatusById(Long id, String status);

    @Modifying
    @Query("UPDATE QuotationItem qi SET qi.isProduction = :isProduction WHERE qi.id = :id")
    int updateIsProductionById(Long id, boolean isProduction);

    @Modifying
    @Query("UPDATE QuotationItem qi SET qi.createdRoll = :createdRoll WHERE qi.id = :id")
    int updateCreatedRollById(Long id, Integer createdRoll);

    @Modifying
    @Query("UPDATE QuotationItem qi SET qi.numberOfRoll = :numberOfRoll WHERE qi.id = :id")
    int updateNumberOfRollById(Long id, Integer numberOfRoll);
}