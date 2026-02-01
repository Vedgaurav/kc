package com.one.kc.user.dto;

import com.one.kc.common.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AdminUserListDto {
    private String userId;
    private String email;
    private String firstName;
    private String lastName;
    private UserStatus status;
    private boolean isAdmin;
    private boolean isFacilitator;
}
