package com.one.kc.chanting.entity;

import com.one.kc.common.utils.AuditEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

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
    private Integer chantingRounds;
}

