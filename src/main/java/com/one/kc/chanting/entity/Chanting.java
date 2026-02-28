package com.one.kc.chanting.entity;

import com.one.kc.common.utils.AuditEntity;
import com.one.kc.user.entity.User;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "chanting",
        indexes = {
                @Index(
                        name = "idx_chanting_user_time",
                        columnList = "user_id, chanting_at"
                ),
                @Index(
                        name = "idx_chanting_time",
                        columnList = "chanting_at"
                )
        }
)
@Getter
@Setter
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
public class Chanting extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long chantingId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_chanting_user")
    )
    private User user;

    @Column(nullable = false)
    private Integer chantingRounds;

    @Column(nullable = false)
    private Instant chantingAt;
}