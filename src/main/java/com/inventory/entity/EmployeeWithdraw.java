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
@Table(name = "employee_withdraw", indexes = {
    @Index(name = "idx_employee_withdraw_employee_id", columnList = "employee_id"),
    @Index(name = "idx_employee_withdraw_withdraw_date", columnList = "withdraw_date"),
    @Index(name = "idx_employee_withdraw_client_id", columnList = "client_id"),
    @Index(name = "idx_employee_withdraw_created_at", columnList = "created_at")
})
public class EmployeeWithdraw {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false, referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_employee_withdraw_employee_id_employee_id"))
    private Employee employee;

    @Column(name = "withdraw_date", nullable = false, columnDefinition = "DATE DEFAULT CURRENT_DATE")
    private LocalDate withdrawDate = LocalDate.now();

    @Column(name = "payment", precision = 10, scale = 2, nullable = false)
    private BigDecimal payment;

    @Column(name = "created_at", nullable = false, length = 29, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_employee_withdraw_created_by_user_master_id"))
    private UserMaster createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", referencedColumnName = "id",
        foreignKey = @ForeignKey(name = "fk_employee_withdraw_client_id_client_id"))
    private Client client;
}


