package com.one.kc.user.controller;

import com.one.kc.user.dto.UserDto;
import com.one.kc.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Create a new user.
     */
    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @RequestBody UserDto userDto) {
        return userService.createUser(userDto);
    }

    /**
     * Update an existing user.
     */
    @PutMapping("/{email}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable String email,
            @RequestBody UserDto userDto) {
        return userService.updateUser(email, userDto);
    }

    /**
     * Get user by email.
     */
    @GetMapping("/{email}")
    public ResponseEntity<UserDto> getUserByEmail(
            @PathVariable String email) {
        return userService.getUserResponseByEmail(email);
    }

    /**
     * Get all users.
     */
    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * Soft delete user (mark inactive).
     */
    @PatchMapping("/{email}/deactivate")
    public ResponseEntity<Void> softDeleteUser(
            @PathVariable String email) {
        return userService.softDeleteUser(email);
    }

    /**
     * Hard delete user (permanent).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> hardDeleteUser(
            @PathVariable Long id) {
        return userService.hardDeleteUser(id);
    }
}

