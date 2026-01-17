package com.one.kc.user.entity;

import com.one.kc.common.enums.UserStatus;
import com.one.kc.common.utils.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.Objects;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "USER")
public class User extends AuditEntity {

    @Id
    private Long userId;
    @Column(nullable = false, unique = true)
    private String email;
    @Column(nullable = false)
    private String phoneNumber;
    @Column(nullable = false)
    private String firstName;
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status = UserStatus.ACTIVE;
    private Integer committedRounds;

    @PrePersist
    @PreUpdate
    public void prePersist() {
        this.lastName = Objects.requireNonNullElse(lastName, "");
        this.committedRounds = Objects.requireNonNullElse(committedRounds, 0);
    }
}
