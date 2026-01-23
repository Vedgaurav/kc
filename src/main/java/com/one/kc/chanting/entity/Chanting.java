package com.one.kc.chanting.entity;

import com.one.kc.common.utils.AuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(
        name = "CHANTING",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_user_date",
                        columnNames = {"userId", "chantingDate"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_chanting_user_date",
                        columnList = "userId, chantingDate"
                )
        }
)
public class Chanting extends AuditEntity {

    @Id
    private Long chantingId;
    private Long userId;
    @Column(nullable = false)
    private Integer chantingRounds;
    @Column(nullable = false)
    private LocalDate chantingDate;
}

