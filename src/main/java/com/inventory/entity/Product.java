package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.math.BigDecimal;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "product", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_product_category_name", columnNames = {"category_id", "name"})
    },
    indexes = {
        @Index(name = "idx_product_name", columnList = "name"),
        @Index(name = "idx_product_status", columnList = "status"),
        @Index(name = "idx_product_remaining_quantity", columnList = "remaining_quantity"),
        @Index(name = "idx_product_category_id", columnList = "category_id"),
        @Index(name = "idx_product_client_id", columnList = "client_id")
    }
)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 256)
    private String name;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_product_category_id_category_id"))
    private Category category;

    @Column(name = "purchase_amount", precision = 10, scale = 2, columnDefinition = "numeric(10,2) DEFAULT 0")
    private BigDecimal purchaseAmount = BigDecimal.valueOf(0.00);

    @Column(name = "sale_amount", precision = 10, scale = 2, columnDefinition = "numeric(10,2) DEFAULT 0")
    private BigDecimal saleAmount = BigDecimal.valueOf(0.00);
    
    @Column(name = "created_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_product_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @Column(name = "status", nullable = false, length = 2)
    private String status = "A";

    @Column(name = "remaining_quantity" , precision = 5, scale = 2, columnDefinition = "numeric(10,3) DEFAULT 0 ")
    private BigDecimal remainingQuantity;
    
    @Column(name = "description")
    private String description;
    
    @Column(name = "minimum_stock", precision = 10, scale = 2)
    private BigDecimal minimumStock;

    @Column(name = "tax_percentage", nullable = false, precision = 5, scale = 2, columnDefinition = "numeric(5,2) DEFAULT 18 ")
    private BigDecimal taxPercentage = BigDecimal.valueOf(18);

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_product_client_id_client_id"))
    private Client client;
}