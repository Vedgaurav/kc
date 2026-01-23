package com.one.kc.user.service;

import com.one.kc.auth.utils.JwtUtil;
import com.one.kc.common.enums.UserStatus;
import com.one.kc.common.exceptions.ResourceAlreadyExistsException;
import com.one.kc.common.exceptions.ResourceNotFoundException;
import com.one.kc.common.exceptions.UserFacingException;
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
import org.apache.commons.lang3.Strings;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SnowflakeIdGenerator idGenerator;


    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            SnowflakeIdGenerator idGenerator
    ) {
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
    public User createUser(UserDto userDto) {
        validateUserDto(userDto);

        User user = prepareUser(userDto);

        User saved = userRepository.save(user);

        LoggerUtils.info(logger, "User created: {}", saved.getEmail());

        return saved;
    }

    public Optional<User> createUserWithGoogleLogin(UserDto userDto) {
        User userSaved = validateAndSaveUser(userDto);
        if(Strings.CS.equals(userDto.getEmail(), userSaved.getEmail())) {
            LoggerUtils.info(logger, "User Created: {}", userSaved.getEmail());
            return Optional.of(userSaved);
        }
        return Optional.empty();
    }

    private @NonNull User validateAndSaveUser(UserDto userDto) {
        validateUserDto(userDto);
        User user = prepareUser(userDto);
        return userRepository.save(user);
    }

    private void validateUserDto(UserDto userDto) {
        if (userDto == null || StringUtils.isBlank(userDto.getEmail())) {
            throw new UserFacingException("Invalid user");
        }

        if (userRepository.existsByEmail(userDto.getEmail())) {
            throw new ResourceAlreadyExistsException(
                    "User already exists with email"
            );
        }
    }


    private @NonNull User prepareUser(UserDto userDto) {
        User user = userMapper.toEntity(userDto);

        Long userId = idGenerator.nextId();
        user.setUserId(userId);
        user.setAddBy(userId);
        user.setChgBy(userId);

        if (StringUtils.isNotBlank(userDto.getPhoneNumber())) {
            user.setPhoneNumber(
                    PhoneNumberUtils.toE164(
                            userDto.getCountryCode(),
                            userDto.getPhoneNumber()
                    )
            );
        }

        return user;
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
     * @param jwt     authentication token
     * @param userDto updated user details
     * @return updated {@link UserDto}
     * @throws ResourceNotFoundException if user does not exist
     */
    @Transactional
    public ResponseEntity<UserDto> updateUser(
            UserDto userDto,
            Jwt jwt
    ) {

        Long userId = JwtUtil.getUserId(jwt);
        User existingUser = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found "));

        userMapper.updateEntityFromDto(userDto, existingUser);

        if(StringUtils.isNotBlank(userDto.getPhoneNumber()) && StringUtils.isNotBlank(userDto.getCountryCode())) {
            String e164PhoneNumber = PhoneNumberUtils.toE164(userDto.getCountryCode(), userDto.getPhoneNumber());
            existingUser.setPhoneNumber(e164PhoneNumber);
            if(existingUser.getStatus() ==  UserStatus.INACTIVE) {
                existingUser.setStatus(UserStatus.ACTIVE);
            }
        }

        User updatedUser = userRepository.save(existingUser);

        UserDto userDtoResponse = userMapper.toDto(updatedUser);
        setPhoneParts(updatedUser, userDtoResponse);

        LoggerUtils.info(logger, "User Updated ");

        return ResponseEntity.ok(userDtoResponse);
    }

    /**
     * Retrieves a user by email.
     *
     * @param jwt authentication token
     * @return {@link UserDto} for the requested user
     * @throws ResourceNotFoundException if user is not found
     */
    public ResponseEntity<UserDto> getUser(Jwt jwt) {

        Long userId = JwtUtil.getUserId(jwt);
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with "));

        UserDto userDto = userMapper.toDto(user);

        setPhoneParts(user, userDto);

        return ResponseEntity.ok(userDto);
    }

    private static void setPhoneParts(
            User user,
            UserDto userDto
    ) {
        if(StringUtils.isNotBlank(userDto.getPhoneNumber())) {
            PhoneNumberUtils.PhoneParts phoneParts = PhoneNumberUtils.fromE164(user.getPhoneNumber());
            userDto.setCountryCode(phoneParts.countryCode());
            userDto.setPhoneNumber(phoneParts.phoneNumber());
        }
    }

    public Optional<User> getUserFromEmail(String email) {
        if (StringUtils.isBlank(email)) return Optional.empty();
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByGoogleSub(String email) {
        if (StringUtils.isBlank(email)) return Optional.empty();
        return userRepository.findByEmail(email);
    }

    public Optional<User> findByUserId(Long userId) {
        if (userId == null) return Optional.empty();
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
     *
     * @return List<UserDto>
     */
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> userList = userRepository.findAll();

        List<UserDto> userDtoList = userList.stream().map(userMapper::toDto).toList();
        return ResponseEntity.ok(userDtoList);
    }

    public ResponseEntity<UserDto> getUserFromAuth(Authentication authentication) {

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {

            Jwt jwt = jwtAuth.getToken();

            UserDto userDto = new UserDto();
            userDto.setEmail(jwt.getClaimAsString("email"));
            userDto.setFirstName(jwt.getClaimAsString("firstName"));
            userDto.setLastName(jwt.getClaimAsString("lastName"));

            return ResponseEntity.ok(userDto);
        }
        throw new UserFacingException("Unauthorized User");
    }

}
