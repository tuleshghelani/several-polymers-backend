package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "powder_coating_process", indexes = {
    @Index(name = "idx_pcp_customer_id", columnList = "customer_id"),
    @Index(name = "idx_pcp_product_id", columnList = "product_id"),
    @Index(name = "idx_pcp_created_at", columnList = "created_at"),
    @Index(name = "idx_pcp_client_id", columnList = "client_id")
})
public class PowderCoatingProcess {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_pcp_customer_id_customer_id"))
    private Customer customer;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_pcp_product_id_product_id"))
    private Product product;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "remaining_quantity", nullable = false)
    private Integer remainingQuantity;

    @Column(name = "total_bags")
    private Integer totalBags;

    @Column(name = "remarks", columnDefinition = "varchar")
    private String remarks;

    @Column(name = "unit_price", precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "total_amount", precision = 12, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(name = "created_at", length = 29, 
        columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", length = 29, 
        columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_pcp_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @Column(name = "status", nullable = false, length = 2)
    private String status = "A";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_powder_coating_process_client_id_client_id"))
    private Client client;
} 