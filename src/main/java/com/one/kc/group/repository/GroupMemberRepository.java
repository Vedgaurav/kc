package com.one.kc.group.repository;

import com.one.kc.group.entity.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {

    @Query("""
        SELECT gm.user.userId
        FROM GroupMember gm
        WHERE gm.group.groupId = :groupId
    """)
    List<Long> findUserIdsByGroupId(Long groupId);

    boolean existsByGroup_GroupIdAndUser_UserId(Long groupId, Long userId);
}

