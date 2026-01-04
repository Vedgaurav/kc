package com.one.kc.auth.controller;

import com.one.kc.auth.dto.AuthResponse;
import com.one.kc.auth.dto.GoogleLoginRequest;
import com.one.kc.auth.service.AuthService;
import com.one.kc.user.dto.UserDto;
import com.one.kc.user.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    public AuthController(AuthService authService, UserService userService) {
        this.authService = authService;
        this.userService = userService;
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

    @PostMapping("/logoutAll")
    public ResponseEntity<Void> logoutAll(@CookieValue(value = "refresh_token", required = false) String refreshToken, HttpServletResponse response) {
        return authService.logoutAll(refreshToken, response);
    }

    /**
     * Create a new user.
     */
    @PostMapping("/user")
    public ResponseEntity<UserDto> createUser(
            @RequestBody UserDto userDto) {
        return userService.createUser(userDto);
    }

}

