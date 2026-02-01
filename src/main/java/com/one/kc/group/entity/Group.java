package com.one.kc.group.entity;

import com.one.kc.common.utils.AuditEntity;
import com.one.kc.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "user_groups")
@Data
public class Group extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @Column(nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    // Owner (facilitator/admin)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @OneToMany(
            mappedBy = "group",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    private Set<GroupMember> members = new HashSet<>();

    public void addMember(User user) {
        GroupMember member = new GroupMember(this, user);
        members.add(member);
    }

    public void removeMember(User user) {
        members.removeIf(m -> m.getUser().equals(user));
    }
}

