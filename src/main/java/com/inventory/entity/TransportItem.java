package com.inventory.entity;

import jakarta.persistence.*;
import lombok.*;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transport_items", indexes = {
    @Index(name = "idx_transport_items_product_id", columnList = "product_id"),
    @Index(name = "idx_transport_items_transport_bag_id", columnList = "transport_bag_id"),
    @Index(name = "idx_transport_items_transport_id", columnList = "transport_id"),
    @Index(name = "idx_transport_items_client_id", columnList = "client_id")
})
public class TransportItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false, referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_transport_items_product_id_product_id"))
    private Product product;
    
    @Column(name = "quantity", nullable = false)
    private Integer quantity;
    
    @Column(name = "per_bag_quantity")
    private Integer perBagQuantity;
    
    @Column(name = "remarks", length = 512)
    private String remarks;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_bag_id", nullable = false, referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_transport_items_transport_bag_id_transport_bag_id"))
    private TransportBag transportBag;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_id", nullable = false, referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_transport_items_transport_id_transport_id"))
    private Transport transport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_transport_items_client_id_client_id"))
    private Client client;
} 