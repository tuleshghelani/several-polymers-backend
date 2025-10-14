package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "payment_history", indexes = {
    @Index(name = "idx_payment_history_customer_id", columnList = "customer_id"),
    @Index(name = "idx_payment_history_type", columnList = "type"),
    @Index(name = "idx_payment_history_created_at", columnList = "created_at"),
    @Index(name = "idx_payment_history_client_id", columnList = "client_id")
})
public class PaymentHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "amount", precision = 10, scale = 2, nullable = false)
    private BigDecimal amount;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", referencedColumnName = "id", nullable = false, 
                foreignKey = @ForeignKey(name = "fk_payment_history_customer_id"))
    private Customer customer;
    
    @Column(name = "type", length = 1, nullable = false)
    private String type; // 'C' for Cash, 'B' for Bank
    
    @Column(name = "remarks", length = 1000)
    private String remarks;
    
    @Column(name = "is_received", nullable = false, columnDefinition = "BOOL DEFAULT TRUE")
    private Boolean isReceived = true;
    
    @Column(name = "date")
    private LocalDate date;
    
    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", 
                foreignKey = @ForeignKey(name = "fk_payment_history_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id", 
                foreignKey = @ForeignKey(name = "fk_payment_history_updated_by_user_master_id"))
    private UserMaster updatedBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", 
                foreignKey = @ForeignKey(name = "fk_payment_history_client_id_client_id"))
    private Client client;
    
    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}