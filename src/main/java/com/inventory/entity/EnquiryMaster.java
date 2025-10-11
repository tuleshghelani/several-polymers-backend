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
@Table(name = "enquiry_master", indexes = {
        @Index(name = "idx_enquiry_name", columnList = "name"),
        @Index(name = "idx_enquiry_client_id", columnList = "client_id"),
        @Index(name = "idx_enquiry_created_at", columnList = "created_at")
})
public class EnquiryMaster {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", length = 256)
    private String name;

    @Column(name = "mobile", length = 16)
    private String mobile;

    @Column(name = "mail", length = 256)
    private String mail;

    @Column(name = "subject", columnDefinition = "TEXT")
    private String subject;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status", length = 4)
    private String status;

    @Column(name = "type", length = 64)
    private String type;

    @Column(name = "company", length = 64)
    private String company;

    @Column(name = "city", length = 64)
    private String city;

    @Column(name = "state", length = 16)
    private String state;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_enquiry_client"))
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_enquiry_created_by"))
    private UserMaster createdBy;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_enquiry_updated_by"))
    private UserMaster updatedBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
