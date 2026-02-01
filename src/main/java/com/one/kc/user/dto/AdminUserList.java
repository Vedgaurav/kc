package com.one.kc.user.dto;

import com.one.kc.common.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminUserList {
    private Long userId;
    private String email;
    private String firstName;
    private String lastName;
    private UserStatus status;
    private String role;
}

