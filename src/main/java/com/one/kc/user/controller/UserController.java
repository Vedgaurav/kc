package com.one.kc.user.controller;

import com.one.kc.user.dto.UserDto;
import com.one.kc.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Update an existing user.
     */
    @PutMapping
    public ResponseEntity<UserDto> updateUser(
            @RequestBody UserDto userDto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return userService.updateUser(userDto, jwt);
    }

    /**
     * Get user.
     */
    @GetMapping
    public ResponseEntity<UserDto> getUser(
            @AuthenticationPrincipal Jwt jwt
    ) {
        return userService.getUser(jwt);
    }

    @GetMapping("/auth")
    public ResponseEntity<UserDto> getAuth(Authentication authentication) {
        return userService.getUserFromAuth(authentication);
    }
}

