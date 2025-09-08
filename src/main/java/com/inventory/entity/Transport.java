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
@Table(name = "transport", indexes = {
    @Index(name = "idx_transport_customer_id", columnList = "customer_id"),
    @Index(name = "idx_transport_created_at", columnList = "created_at"),
    @Index(name = "idx_transport_client_id", columnList = "client_id")
})
public class Transport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false, referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_transport_customer_id_customer_id"))
    private Customer customer;

    @Column(name = "total_weight", precision = 10, scale = 3)
    private BigDecimal totalWeight;

    @Column(name = "total_bags")
    private Integer totalBags;
    
    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_transport_created_by_user_master_id"))
    private UserMaster createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_transport_client_id_client_id"))
    private Client client;
} 