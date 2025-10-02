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
@Table(name = "batch", indexes = {
        @Index(name = "idx_batch_date", columnList = "date"),
        @Index(name = "idx_batch_shift", columnList = "shift"),
        @Index(name = "idx_batch_machine_id", columnList = "machine_id"),
        @Index(name = "idx_batch_name", columnList = "name"),
        @Index(name = "idx_batch_client_id", columnList = "client_id")
})
public class Batch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_batch_created_by_user_master_id"))
    private UserMaster createdBy;

    @Column(name = "created_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "date", nullable = false)
    private LocalDate date;

    @Column(name = "shift", nullable = false, length = 2)
    private String shift;

    @Column(name = "name", nullable = false, length = 32)
    private String name;
    
    @Column(name = "operator", columnDefinition = "text")
    private String operator;

    @Column(name = "resign_bag_use", precision = 10, scale = 3, columnDefinition = "numeric(10,3)")
    private BigDecimal resignBagUse = BigDecimal.ZERO;

    @Column(name = "resign_bag_opening_stock", precision = 10, scale = 3, columnDefinition = "numeric(10,3)")
    private BigDecimal resignBagOpeningStock = BigDecimal.ZERO;

    @Column(name = "cpw_bag_use", precision = 10, scale = 3, columnDefinition = "numeric(10,3)")
    private BigDecimal cpwBagUse = BigDecimal.ZERO;

    @Column(name = "cpw_bag_opening_stock", precision = 10, scale = 3, columnDefinition = "numeric(10,3)")
    private BigDecimal cpwBagOpeningStock = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "machine_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_batch_machine_id_machine_master_id"))
    private MachineMaster machine;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_batch_client_id_client_id"))
    private Client client;
}


