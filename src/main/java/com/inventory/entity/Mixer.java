package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mixer", indexes = {
        @Index(name = "idx_mixer_batch_id", columnList = "bach_id"),
        @Index(name = "idx_mixer_product_id", columnList = "product_id"),
        @Index(name = "idx_mixer_client_id", columnList = "client_id")
})
public class Mixer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bach_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_mixer_batch_id_batch_id"))
    private Batch batch;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_mixer_product_id_product_id"))
    private Product product;

    @Column(name = "quantity", precision = 10, scale = 3, columnDefinition = "numeric(10,3)")
    private BigDecimal quantity = BigDecimal.ZERO;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_mixer_client_id_client_id"))
    private Client client;
}


