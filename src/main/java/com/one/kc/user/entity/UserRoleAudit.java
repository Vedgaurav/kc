package com.one.kc.user.entity;

import com.one.kc.common.enums.RoleAuditAction;
import com.one.kc.common.enums.UserRole;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


import java.time.Instant;

@Entity
@Table(
        name = "user_role_audit",
        indexes = {
                @Index(name = "idx_role_audit_user", columnList = "target_user_id"),
                @Index(name = "idx_role_audit_actor", columnList = "actor_user_id"),
                @Index(name = "idx_role_audit_role", columnList = "role")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRoleAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** User whose role was modified */
    @Column(name = "target_user_id", nullable = false)
    private Long targetUserId;

    /** Admin / Super Admin who performed the action */
    @Column(name = "actor_user_id", nullable = false)
    private Long actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RoleAuditAction action;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        this.createdAt = Instant.now();
    }
}
