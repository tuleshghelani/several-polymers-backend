package com.inventory.entity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "daily_profit",
    indexes = {
        @Index(name = "idx_daily_sale_id", columnList = "sale_id"),
        @Index(name = "idx_daily_profit_date", columnList = "profit_date"),
        @Index(name = "idx_daily_profit_gross_profit", columnList = "gross_profit"),
        @Index(name = "idx_daily_profit_net_profit", columnList = "net_profit"),
        @Index(name = "idx_daily_profit_other_expenses", columnList = "other_expenses"),
        @Index(name = "idx_daily_profit_purchase_amount", columnList = "purchase_amount"),
        @Index(name = "idx_daily_profit_sale_amount", columnList = "sale_amount"),        
        @Index(name = "idx_daily_profit_client_id", columnList = "client_id")
    }
)
public class DailyProfit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sale_id", nullable = false, referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_daily_profit_sale_id_sale_id"))
    private Sale sale;
    
    @Column(name = "purchase_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal purchaseAmount;
    
    @Column(name = "sale_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal saleAmount;
    
    @Column(name = "gross_profit", nullable = false, precision = 10, scale = 2)
    private BigDecimal grossProfit;
    
    @Column(name = "other_expenses", precision = 10, scale = 2)
    private BigDecimal otherExpenses;
    
    @Column(name = "net_profit", nullable = false, precision = 10, scale = 2)
    private BigDecimal netProfit;
    
    @Column(name = "profit_date", nullable = false)
    private OffsetDateTime profitDate;
    
    @Column(name = "created_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_daily_profit_client_id_client_id"))
    private Client client;
}