package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "powder_coating_return", indexes = {
    @Index(name = "idx_pcr_process_id", columnList = "process_id"),
    @Index(name = "idx_pcr_created_at", columnList = "created_at"),
    @Index(name = "idx_pcr_client_id", columnList = "client_id")
})
public class PowderCoatingReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "process_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_pcr_process_id"))
    private PowderCoatingProcess process;
    
    @Column(name = "return_quantity", nullable = false)
    private Integer returnQuantity;
    
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private UserMaster createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_powder_coating_return_client_id_client_id"))
    private Client client;
} 