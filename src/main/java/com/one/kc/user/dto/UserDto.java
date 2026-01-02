package com.one.kc.user.dto;

import com.one.kc.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
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
}
