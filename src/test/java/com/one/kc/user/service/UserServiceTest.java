//package com.one.kc.user.service;
//
//import com.one.kc.common.enums.UserStatus;
//import com.one.kc.common.exceptions.ResourceAlreadyExistsException;
//import com.one.kc.common.exceptions.ResourceNotFoundException;
//import com.one.kc.common.utils.SnowflakeIdGenerator;
//import com.one.kc.user.dto.UserDto;
//import com.one.kc.user.entity.User;
//import com.one.kc.user.mapper.UserMapper;
//import com.one.kc.user.repository.UserRepository;
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.extension.ExtendWith;
//import org.mockito.InjectMocks;
//import org.mockito.Mock;
//import org.mockito.junit.jupiter.MockitoExtension;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//
//import java.util.Optional;
//
//import static org.junit.jupiter.api.Assertions.*;
//import static org.mockito.ArgumentMatchers.any;
//import static org.mockito.Mockito.*;
//
//@ExtendWith(MockitoExtension.class)
//class UserServiceTest {
//
//    @Mock
//    private UserRepository userRepository;
//
//    @Mock
//    private UserMapper userMapper;
//
//    @Mock
//    private SnowflakeIdGenerator idGenerator;
//
//    @InjectMocks
//    private UserService userService;
//
//    private UserDto userDto;
//    private User user;
//
//    @BeforeEach
//    void setUp() {
//        userDto = new UserDto();
//        userDto.setEmail("test@one.com");
//        userDto.setFirstName("Test");
//        userDto.setLastName("User");
//
//        user = new User();
//        user.setId(1L);
//        user.setEmail("test@one.com");
//        user.setStatus(UserStatus.ACTIVE);
//    }
//
//    // -------------------------
//    // Create User
//    // -------------------------
//
//    @Test
//    void createUser_success() {
//        when(userRepository.existsByEmail(userDto.getEmail())).thenReturn(false);
//        when(userMapper.toEntity(userDto)).thenReturn(user);
//        when(idGenerator.nextId()).thenReturn(1L);
//        when(userRepository.save(any(User.class))).thenReturn(user);
//        when(userMapper.toDto(user)).thenReturn(userDto);
//
//        ResponseEntity<UserDto> response = userService.createUser(userDto);
//
//        assertEquals(HttpStatus.CREATED, response.getStatusCode());
//        assertNotNull(response.getBody());
//        assertEquals("test@one.com", response.getBody().getEmail());
//    }
//
//    @Test
//    void createUser_duplicateEmail_shouldThrowException() {
//        when(userRepository.existsByEmail(userDto.getEmail())).thenReturn(true);
//
//        assertThrows(ResourceAlreadyExistsException.class,
//                () -> userService.createUser(userDto));
//    }
//
//    @Test
//    void createUser_invalidRequest_shouldReturnBadRequest() {
//        ResponseEntity<UserDto> response = userService.createUser(new UserDto());
//
//        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
//    }
//
//    // -------------------------
//    // Update User
//    // -------------------------
//
//    @Test
//    void updateUser_success() {
//        when(userRepository.findByEmail("test@one.com"))
//                .thenReturn(Optional.of(user));
//        when(userRepository.save(user)).thenReturn(user);
//        when(userMapper.toDto(user)).thenReturn(userDto);
//
//        ResponseEntity<UserDto> response =
//                userService.updateUser("test@one.com", userDto);
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        verify(userMapper).updateEntityFromDto(userDto, user);
//    }
//
//    @Test
//    void updateUser_userNotFound() {
//        when(userRepository.findByEmail("test@one.com"))
//                .thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class,
//                () -> userService.updateUser( eq(userDto), any()));
//    }
//
//    // -------------------------
//    // Get User
//    // -------------------------
//
//    @Test
//    void getUserResponseByEmail_success() {
//        when(userRepository.findByEmail("test@one.com"))
//                .thenReturn(Optional.of(user));
//        when(userMapper.toDto(user)).thenReturn(userDto);
//
//        ResponseEntity<UserDto> response =
//                userService.getUserResponseByEmail("test@one.com");
//
//        assertEquals(HttpStatus.OK, response.getStatusCode());
//        assertEquals("test@one.com", response.getBody().getEmail());
//    }
//
//    @Test
//    void getUserResponseByEmail_notFound() {
//        when(userRepository.findByEmail("test@one.com"))
//                .thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class,
//                () -> userService.getUserResponseByEmail("test@one.com"));
//    }
//
//    // -------------------------
//    // Soft Delete
//    // -------------------------
//
//    @Test
//    void softDeleteUser_success() {
//        when(userRepository.findByEmail("test@one.com"))
//                .thenReturn(Optional.of(user));
//
//        ResponseEntity<Void> response =
//                userService.softDeleteUser("test@one.com");
//
//        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
//        assertEquals(UserStatus.INACTIVE, user.getStatus());
//    }
//
//    @Test
//    void softDeleteUser_notFound() {
//        when(userRepository.findByEmail("test@one.com"))
//                .thenReturn(Optional.empty());
//
//        assertThrows(ResourceNotFoundException.class,
//                () -> userService.softDeleteUser("test@one.com"));
//    }
//
//    // -------------------------
//    // Hard Delete
//    // -------------------------
//
//    @Test
//    void hardDeleteUser_success() {
//        when(userRepository.existsById(1L)).thenReturn(true);
//
//        ResponseEntity<Void> response = userService.hardDeleteUser(1L);
//
//        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
//        verify(userRepository).deleteById(1L);
//    }
//
//    @Test
//    void hardDeleteUser_notFound() {
//        when(userRepository.existsById(1L)).thenReturn(false);
//
//        assertThrows(ResourceNotFoundException.class,
//                () -> userService.hardDeleteUser(1L));
//    }
//}
//
