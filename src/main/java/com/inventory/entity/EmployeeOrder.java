package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employee_orders", indexes = {
    @Index(name = "idx_employee_orders_product_id", columnList = "product_id"),
    @Index(name = "idx_employee_orders_status", columnList = "status"),
    @Index(name = "idx_employee_orders_created_at", columnList = "created_at"),
    @Index(name = "idx_employee_orders_client_id", columnList = "client_id")
})
public class EmployeeOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_employee_orders_product_id_product_id"))
    private Product product;
    
    @Column(name = "employee_ids", columnDefinition = "bigint[]")
    private List<Long> employeeIds;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "remarks", length = 512)
    private String remarks;
    
    @Column(name = "status", nullable = false, length = 2)
    private String status = "O";
    
    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_employee_orders_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_employee_orders_client_id_client_id"))
    private Client client;
} 