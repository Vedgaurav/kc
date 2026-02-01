package com.one.kc.user.dto;

import com.one.kc.common.enums.UserStatus;
import com.one.kc.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
    private String email;
    private String countryCode;
    private String phoneNumber;
    private String firstName;
    private String lastName;
    private LocalDateTime addTime;
    private LocalDateTime chgTime;
    private String addBy;
    private String chgBy;
    private UserStatus status;
    private Integer committedRounds;
    private String facilitatorId;
    private String facilitatorName;
    private List<String> roles;
}
