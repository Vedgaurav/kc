package com.one.kc.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GoogleUser {
    private String email;
    private String firstName;
    private String lastName;
    private String googleId;
}

