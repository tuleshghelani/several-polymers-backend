package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "brand", indexes = {
        @Index(name = "idx_brand_name", columnList = "name"),
        @Index(name = "idx_brand_client_id", columnList = "client_id"),
        @Index(name = "idx_brand_created_at", columnList = "created_at")
})
public class Brand {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "status", nullable = false, length = 2)
    private String status = "A";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_brand_client_id"))
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_brand_created_by"))
    private UserMaster createdBy;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_brand_updated_by"))
    private UserMaster updatedBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}


