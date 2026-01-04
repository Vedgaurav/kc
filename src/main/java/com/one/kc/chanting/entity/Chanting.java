package com.one.kc.chanting.entity;

import com.one.kc.common.utils.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
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
@Table(name = "CHANTING")
public class Chanting extends AuditEntity {

    @Id
    private Long chantingId;
    private Long userId;
    @Column(nullable = false)
    private Integer chantingRounds;
    @Column(nullable = false)
    private LocalDate chantingDate;
}

