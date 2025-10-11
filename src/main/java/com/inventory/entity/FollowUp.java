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
@Table(name = "follow_up", indexes = {
        @Index(name = "idx_follow_up_enquiry_id", columnList = "enquiry_id"),
        @Index(name = "idx_follow_up_client_id", columnList = "client_id"),
        @Index(name = "idx_follow_up_created_at", columnList = "created_at"),
        @Index(name = "idx_follow_up_next_action_date", columnList = "next_action_date")
})
public class FollowUp {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "follow_up_status", length = 4, nullable = false)
    private String followUpStatus;

    @Column(name = "next_action_date", nullable = false)
    private OffsetDateTime nextActionDate;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "enquiry_id", referencedColumnName = "id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_follow_up_enquiry_id"))
    private EnquiryMaster enquiry;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_followup_client"))
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_follow_up_created_by"))
    private UserMaster createdBy;

    @Column(name = "created_at", columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by", referencedColumnName = "id",
            foreignKey = @ForeignKey(name = "fk_follow_up_updated_by"))
    private UserMaster updatedBy;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;
}
