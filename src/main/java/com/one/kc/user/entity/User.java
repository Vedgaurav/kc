package com.one.kc.user.entity;

import com.one.kc.common.enums.Gender;
import com.one.kc.common.enums.UserRole;
import com.one.kc.common.enums.UserStatus;
import com.one.kc.common.utils.AuditEntity;
import com.one.kc.group.entity.Group;
import com.one.kc.group.entity.GroupMember;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(
        name = "users",
        indexes = {

                // Search by first name
                @Index(name = "idx_users_first_name", columnList = "first_name"),

                // Email lookups
                @Index(name = "idx_users_email", columnList = "email"),

                // Status filtering
                @Index(name = "idx_users_status", columnList = "status"),

                // Facilitator relationship
                @Index(name = "idx_users_facilitator_id", columnList = "facilitator_id"),

                // 🔥 Root group (tenant boundary)
                @Index(name = "idx_users_root_group", columnList = "root_group_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(exclude = {
        "facilitator",
        "facilitatedUsers",
        "roles",
        "groupMemberships",
        "rootGroup"
})
public class User extends AuditEntity {

    @Id
    @EqualsAndHashCode.Include
    private Long userId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(nullable = false)
    private String firstName;

    private String lastName;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.INACTIVE;

    private Integer committedRounds;

    /**
     * 🔥 Root group = tenant boundary (MALE_ROOT / FEMALE_ROOT / OTHER_ROOT)
     * This replaces heavy EXISTS-based filtering.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "root_group_id")
    private Group rootGroup;

    /**
     * Facilitator relationship
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facilitator_id")
    private User facilitator;

    @OneToMany(mappedBy = "facilitator")
    private Set<User> facilitatedUsers = new HashSet<>();

    /**
     * Roles
     */
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<UserRoleEntity> roles = new HashSet<>();

    /**
     * Group memberships (sub-groups under root)
     */
    @OneToMany(
            mappedBy = "user",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<GroupMember> groupMemberships = new HashSet<>();

    // -------------------------------------------------------
    // Role helpers
    // -------------------------------------------------------

    public void addRole(UserRole role) {
        if (hasRole(role)) return;

        UserRoleEntity userRole = new UserRoleEntity();
        userRole.setRole(role);
        userRole.setUser(this);

        this.roles.add(userRole);
    }

    public boolean hasRole(UserRole role) {
        return roles.stream()
                .anyMatch(r -> r.getRole() == role);
    }

    public boolean hasRoleWithEqualOrGreaterPriorityThan(UserRole requiredRole) {
        return roles.stream()
                .anyMatch(r -> r.getRole().hasAtLeast(requiredRole));
    }

    // -------------------------------------------------------
    // Lifecycle hooks
    // -------------------------------------------------------

    @PrePersist
    @PreUpdate
    public void prePersist() {
        this.lastName = Objects.requireNonNullElse(this.lastName, "");
        this.committedRounds = Objects.requireNonNullElse(this.committedRounds, 0);
        this.phoneNumber = Objects.requireNonNullElse(this.phoneNumber, "");
    }
}