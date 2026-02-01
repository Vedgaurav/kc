package com.one.kc.user.dto;

import com.one.kc.common.enums.RoleAuditAction;
import com.one.kc.common.enums.UserRole;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class UserRoleAuditDto {
    private Long id;
    private String targetUserEmail;
    private String actorUserEmail;
    private UserRole role;
    private RoleAuditAction action;
    private Instant createdAt;
}
