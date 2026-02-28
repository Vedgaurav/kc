package com.one.kc.group.entity;

import com.one.kc.common.enums.Gender;
import com.one.kc.common.utils.AuditEntity;
import com.one.kc.user.entity.User;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
        name = "user_groups",
        indexes = {
                @Index(name = "idx_group_gender", columnList = "gender"),
                @Index(name = "idx_group_parent", columnList = "parent_group_id")
        }
)
@Data
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
@ToString(exclude = {"parent", "createdBy", "members"})
public class Group extends AuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long groupId;

    @Column(nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    /** Tenant boundary */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private Gender gender;

    /** Parent group (hierarchy like MALE → Pune) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_group_id")
    private Group parent;

    /**
     * Creator (nullable for system/root groups)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")  // 🔥 nullable allowed
    private User createdBy;

    /** Group members */
    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<GroupMember> members = new HashSet<>();

    public void addMember(User user) {
        members.add(GroupMember.builder().group(this).user(user).build());
    }

    public void removeMember(User user) {
        members.removeIf(m -> m.getUser().equals(user));
    }
}