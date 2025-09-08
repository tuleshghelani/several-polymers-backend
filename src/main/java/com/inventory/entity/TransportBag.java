package com.inventory.entity;

import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.OffsetDateTime;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "transport_bag", indexes = {
    @Index(name = "idx_transport_bag_transport_id", columnList = "transport_id"),
    @Index(name = "idx_transport_bag_client_id", columnList = "client_id")
})
public class TransportBag {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "weight", nullable = false, precision = 10, scale = 3)
    private BigDecimal weight; //per bag weight

    @Column(name = "number_of_bags", columnDefinition = "int4 default 1")
    private Integer numberOfBags = 1;
    
    @Column(name = "total_bag_weight", precision = 10, scale = 3)
    private BigDecimal totalBagWeight;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transport_id", nullable = false, referencedColumnName = "id", 
        foreignKey = @ForeignKey(name = "fk_transport_bag_transport_id_transport_id"))
    private Transport transport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id", foreignKey = @ForeignKey(name = "fk_transport_bag_client_id_client_id"))
    private Client client;
} 