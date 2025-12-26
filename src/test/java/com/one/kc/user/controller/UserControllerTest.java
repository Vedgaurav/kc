package com.one.kc.user.controller;


import com.one.kc.common.exceptions.ResourceNotFoundException;
import com.one.kc.user.dto.UserDto;
import com.one.kc.user.service.UserService;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;


import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private org.springframework.data.jpa.mapping.JpaMetamodelMappingContext jpaMappingContext;


    @MockitoBean
    private UserService userService;

    @Autowired
    private ObjectMapper objectMapper;

    // -------------------------
    // Helper Method
    // -------------------------
    private UserDto getUserDto() {
        UserDto dto = new UserDto();
        dto.setEmail("test.user@one.com");
        dto.setFirstName("Test");
        dto.setLastName("User");
        dto.setPhoneNumber("9876543210");
        return dto;
    }

    // -------------------------
    // Create User
    // -------------------------
    @Test
    void shouldCreateUser() throws Exception {

        UserDto userDto = getUserDto();

        when(userService.createUser(any(UserDto.class)))
                .thenReturn(ResponseEntity.status(201).body(userDto));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("test.user@one.com"))
                .andExpect(jsonPath("$.firstName").value("Test"));
    }

    // -------------------------
    // Update User
    // -------------------------
    @Test
    void shouldUpdateUser() throws Exception {

        UserDto userDto = getUserDto();

        when(userService.updateUser(eq("test.user@one.com"), any(UserDto.class)))
                .thenReturn(ResponseEntity.ok(userDto));

        mockMvc.perform(put("/api/users/{email}", "test.user@one.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(userDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test.user@one.com"));
    }

    // -------------------------
    // Get User By Email
    // -------------------------
    @Test
    void shouldGetUserByEmail() throws Exception {

        UserDto userDto = getUserDto();

        when(userService.getUserByEmail("test.user@one.com"))
                .thenReturn(ResponseEntity.ok(userDto));

        mockMvc.perform(get("/api/users/{email}", "test.user@one.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("test.user@one.com"));
    }

    // -------------------------
    // Soft Delete User
    // -------------------------
    @Test
    void shouldSoftDeleteUser() throws Exception {

        when(userService.softDeleteUser("test.user@one.com"))
                .thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(patch("/api/users/{email}/deactivate", "test.user@one.com"))
                .andExpect(status().isNoContent());
    }

    // -------------------------
    // Hard Delete User
    // -------------------------
    @Test
    void shouldHardDeleteUser() throws Exception {

        when(userService.hardDeleteUser(1L))
                .thenReturn(ResponseEntity.noContent().build());

        mockMvc.perform(delete("/api/users/{id}", 1L))
                .andExpect(status().isNoContent());
    }

    // -------------------------
    // Negative Case - User Not Found
    // -------------------------
    @Test
    void shouldReturn404WhenUserNotFound() throws Exception {

        when(userService.getUserByEmail("missing@one.com"))
                .thenThrow(new ResourceNotFoundException("User not found"));

        mockMvc.perform(get("/api/users/{email}", "missing@one.com"))
                .andExpect(status().isNotFound());
    }
}

