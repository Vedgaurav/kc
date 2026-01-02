package com.one.kc.auth.dto;

import com.one.kc.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String tokenType = "Bearer";
    private long expiresIn;
    private UserDto userDto;

    public AuthResponse(String accessToken, Long expiresIn, UserDto userDto){
        this.accessToken = accessToken;
        this.expiresIn = expiresIn;
        this.userDto = userDto;
    }

    public AuthResponse(String accessToken){
        this.accessToken = accessToken;
    }
}
