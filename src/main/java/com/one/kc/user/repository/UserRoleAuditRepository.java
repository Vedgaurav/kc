package com.one.kc.user.repository;

import com.one.kc.user.dto.UserRoleAuditDto;
import com.one.kc.user.entity.UserRoleAudit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserRoleAuditRepository
        extends JpaRepository<UserRoleAudit, Long> {

    List<UserRoleAudit> findByTargetUserIdOrderByCreatedAtDesc(Long userId);

    List<UserRoleAudit> findByActorUserIdOrderByCreatedAtDesc(Long actorId);

    @Query("""
        SELECT new com.one.kc.user.dto.UserRoleAuditDto(
            audit.id,
            target.email,
            actor.email,
            audit.role,
            audit.action,
            audit.createdAt
        )
        FROM UserRoleAudit audit
        JOIN User target ON target.userId = audit.targetUserId
        JOIN User actor ON actor.userId = audit.actorUserId
        """)
    Page<UserRoleAuditDto> findAllUserRoleAuditHistory(Pageable pageable);
}
