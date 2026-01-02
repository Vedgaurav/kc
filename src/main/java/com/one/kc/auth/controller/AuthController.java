package com.one.kc.auth.controller;

import com.one.kc.auth.dto.AuthResponse;
import com.one.kc.auth.dto.GoogleLoginRequest;
import com.one.kc.auth.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleLogin(@RequestBody GoogleLoginRequest request, HttpServletResponse response) {

        return authService.googleLogin(request, response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<Void> refreshToken(@CookieValue("refresh_token") String refreshToken, HttpServletResponse response) {
        return authService.refreshToken(refreshToken, response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(value = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        return authService.logout(refreshToken, response);
    }
}

