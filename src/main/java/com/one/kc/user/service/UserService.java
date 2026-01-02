package com.one.kc.user.service;

import com.one.kc.common.enums.UserStatus;
import com.one.kc.common.exceptions.ResourceAlreadyExistsException;
import com.one.kc.common.exceptions.ResourceNotFoundException;
import com.one.kc.common.utils.LoggerUtils;
import com.one.kc.common.utils.PhoneNumberUtils;
import com.one.kc.common.utils.ResponseEntityUtils;
import com.one.kc.common.utils.SnowflakeIdGenerator;
import com.one.kc.user.dto.UserDto;
import com.one.kc.user.entity.User;
import com.one.kc.user.mapper.UserMapper;
import com.one.kc.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SnowflakeIdGenerator idGenerator;



    public UserService(UserRepository userRepository,
                       UserMapper userMapper,
                       SnowflakeIdGenerator idGenerator) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * Creates a new user in the system.
     *
     * <p>
     * - Validates that the request contains a non-empty email
     * - Checks if a user already exists with the given email
     * - Maps DTO to entity and persists it
     * - Sets user status to {@link UserStatus#ACTIVE}
     * </p>
     *
     * @param userDto user details for creation
     * @return {@link ResponseEntity} containing created {@link UserDto}
     * @throws ResourceAlreadyExistsException if user already exists with the same email
     */
    public ResponseEntity<UserDto> createUser(UserDto userDto) {

        if (userDto != null && StringUtils.isNotBlank(userDto.getEmail())) {

            if (userRepository.existsByEmail(userDto.getEmail())) {
                throw new ResourceAlreadyExistsException(
                        "User already exists with email: " + userDto.getEmail());
            }

            String e164PhoneNumber = PhoneNumberUtils.toE164(userDto.getCountryCode(), userDto.getPhoneNumber());
            User user = userMapper.toEntity(userDto);
            user.setUserId(idGenerator.nextId());
            user.setStatus(UserStatus.ACTIVE);
            user.setPhoneNumber(e164PhoneNumber);

            User userSaved = userRepository.save(user);

            LoggerUtils.info(logger, "User Created: {}", userSaved.getEmail());

            return ResponseEntityUtils.getCreatedResponse(
                    userMapper.toDto(userSaved));
        }

        return ResponseEntityUtils.badResquest();
    }

    /**
     * Updates an existing user using partial update.
     *
     * <p>
     * - Fetches user by ID
     * - Updates only non-null fields from DTO
     * - Persists changes in a transactional context
     * </p>
     *
     * @param email user identifier
     * @param userDto updated user details
     * @return updated {@link UserDto}
     * @throws ResourceNotFoundException if user does not exist
     */
    @Transactional
    public ResponseEntity<UserDto> updateUser(String email, UserDto userDto) {

        User existingUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));

        userMapper.updateEntityFromDto(userDto, existingUser);

        User updatedUser = userRepository.save(existingUser);

        LoggerUtils.info(logger, "User Updated: {}", updatedUser.getEmail());

        return ResponseEntity.ok(userMapper.toDto(updatedUser));
    }

    /**
     * Retrieves a user by email.
     *
     * @param email unique email address of the user
     * @return {@link UserDto} for the requested user
     * @throws ResourceNotFoundException if user is not found
     */
    public ResponseEntity<UserDto> getUserResponseByEmail(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));

        return ResponseEntity.ok(userMapper.toDto(user));
    }

    public Optional<User> getUserFromEmail(String email){
        if(StringUtils.isBlank(email)) return Optional.empty();
        return userRepository.findByEmail(email);
    }

    public Optional<User> getUserById(Long userId){
        if(userId == null) return Optional.empty();
        return userRepository.findByUserId(userId);
    }

    /**
     * Soft deletes a user by marking the status as {@link UserStatus#INACTIVE}.
     *
     * <p>
     * This keeps the record in the database for audit and historical purposes.
     * </p>
     *
     * @param email user identifier
     * @return HTTP 204 No Content on success
     * @throws ResourceNotFoundException if user does not exist
     */
    @Transactional
    public ResponseEntity<Void> softDeleteUser(String email) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with email: " + email));

        user.setStatus(UserStatus.INACTIVE);
        userRepository.save(user);

        LoggerUtils.info(logger, "User Deactivated: {}", user.getEmail());

        return ResponseEntity.noContent().build();
    }

    /**
     * Permanently deletes a user from the database.
     *
     * <p>
     * ⚠️ This operation is irreversible.
     * </p>
     *
     * @param id user identifier
     * @return HTTP 204 No Content on success
     * @throws ResourceNotFoundException if user does not exist
     */
    public ResponseEntity<Void> hardDeleteUser(Long id) {

        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "User not found with id: " + id);
        }

        userRepository.deleteById(id);

        LoggerUtils.info(logger, "User Deleted with id: {}", id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Find all users
     * @return List<UserDto>
     */
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> userList = userRepository.findAll();

        List<UserDto> userDtoList =  userList.stream().map(userMapper::toDto).toList();
        return ResponseEntity.ok(userDtoList);
    }
}
