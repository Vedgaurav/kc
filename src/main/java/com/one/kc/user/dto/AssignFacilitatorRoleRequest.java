package com.one.kc.user.dto;

import com.one.kc.common.enums.UserRole;
import lombok.Data;

import java.util.List;

@Data
public class AssignFacilitatorRoleRequest {
    private List<String> userIds;
}
