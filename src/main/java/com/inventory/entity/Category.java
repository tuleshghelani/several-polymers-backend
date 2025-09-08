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
@Table(name = "category", uniqueConstraints={
        @UniqueConstraint( name = "uk_category_name",  columnNames ={"name"})
}, indexes = {
        @Index(name = "idx_category_name", columnList = "name"),
        @Index(name = "idx_category_client_id", columnList = "client_id")
})
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "name", nullable = false, length = 256)
    private String name;
    
    @Column(name = "created_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();
    
    @Column(name = "updated_at", length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    @Column(name = "remaining_quantity")
    private Integer remainingQuantity;
    
    @ManyToOne
    @JoinColumn(name = "created_by", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_category_created_by_user_master_id"))
    private UserMaster createdBy;
    
    @Column(name = "status", nullable = false, length = 2)
    private String status = "A";
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_category_client_id_client_id"))
    private Client client;
}