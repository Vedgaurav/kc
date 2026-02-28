package com.one.kc.group.entity;

import com.one.kc.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "user_group_members",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"group_id", "user_id"})
        },
        indexes = {
                @Index(name = "idx_group_member_user", columnList = "user_id"),
                @Index(name = "idx_group_member_group", columnList = "group_id")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"group", "user"})
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class GroupMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 🔥 Include group in equality
    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_id", nullable = false)
    private Group group;

    // 🔥 Include user in equality
    @EqualsAndHashCode.Include
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public GroupMember(Group group, User user) {
        this.group = group;
        this.user = user;
    }
}