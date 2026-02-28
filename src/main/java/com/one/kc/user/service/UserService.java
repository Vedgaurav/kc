package com.one.kc.user.service;

import com.one.kc.auth.utils.JwtUtil;
import com.one.kc.common.constants.GroupConstants;
import com.one.kc.common.enums.Gender;
import com.one.kc.common.enums.UserRole;
import com.one.kc.common.enums.UserStatus;
import com.one.kc.common.exceptions.ResourceAlreadyExistsException;
import com.one.kc.common.exceptions.ResourceNotFoundException;
import com.one.kc.common.exceptions.UserFacingException;
import com.one.kc.common.utils.LoggerUtils;
import com.one.kc.common.utils.PhoneNumberUtils;
import com.one.kc.common.utils.SnowflakeIdGenerator;
import com.one.kc.group.entity.Group;
import com.one.kc.group.entity.GroupMember;
import com.one.kc.group.repository.GroupRepository;
import com.one.kc.user.dto.FacilitatorListDto;
import com.one.kc.user.dto.UserDto;
import com.one.kc.user.entity.User;
import com.one.kc.user.mapper.UserMapper;
import com.one.kc.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final GroupRepository groupRepository;


    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            SnowflakeIdGenerator idGenerator,
            GroupRepository groupRepository
    ) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.idGenerator = idGenerator;
        this.groupRepository = groupRepository;
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

        // ensureGenderGroupMembership(user, userDto.getGender());
        User saved = userRepository.save(user);

        LoggerUtils.info(logger, "User created: {}", saved.getEmail());

        return saved;
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
        user.addRole(UserRole.USER);
        user.setStatus(UserStatus.INACTIVE);

        if (StringUtils.isNotBlank(userDto.getPhoneNumber())) {
            user.setPhoneNumber(
                    PhoneNumberUtils.toE164(
                            userDto.getCountryCode(),
                            userDto.getPhoneNumber()
                    )
            );
        }

        if (StringUtils.isNotBlank(userDto.getFacilitatorId())) {
            Long facilitatorId = Long.parseLong(userDto.getFacilitatorId());
            User facilitator = userRepository.findByUserId(facilitatorId)
                    .orElseThrow(() ->
                            new ResourceNotFoundException("Facilitator not found")
                    );

            user.setFacilitator(facilitator);
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

        if (CollectionUtils.isEmpty(existingUser.getRoles())) {
            existingUser.addRole(UserRole.USER);
        }

        // ✅ 1️⃣ Ensure gender + root group FIRST
        if (Objects.nonNull(userDto.getGender())) {
            ensureGenderGroupMembership(existingUser, userDto.getGender());
        }

        if (StringUtils.isNotBlank(userDto.getPhoneNumber()) && StringUtils.isNotBlank(userDto.getCountryCode())) {
            String e164PhoneNumber = PhoneNumberUtils.toE164(userDto.getCountryCode(), userDto.getPhoneNumber());
            existingUser.setPhoneNumber(e164PhoneNumber);
            if (existingUser.getStatus() == UserStatus.INACTIVE) {
                existingUser.setStatus(UserStatus.ACTIVE);
            }
        }


        if (StringUtils.isNotBlank(userDto.getFacilitatorId())) {
            Long facilitatorId = Long.parseLong(userDto.getFacilitatorId());
            // Skip if same facilitator
            if ((Objects.isNull(existingUser.getFacilitator())
                    || !existingUser.getFacilitator()
                    .getUserId()
                    .equals(facilitatorId)) && !facilitatorId.equals(existingUser.getUserId())) {

                User facilitator = userRepository.findByUserId(facilitatorId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Facilitator not found")
                        );

                Group userRoot = getRootGroup(existingUser);
                Group facilitatorRoot = getRootGroup(facilitator);

                if (!userRoot.equals(facilitatorRoot)) {
                    throw new UserFacingException(
                            "Facilitator must belong to same gender group"
                    );
                }


                existingUser.setFacilitator(facilitator);
            }
        } else {
            existingUser.setFacilitator(null);
        }

        User updatedUser = userRepository.save(existingUser);

        UserDto userDtoResponse = userMapper.toDto(updatedUser);
        setPhoneParts(updatedUser, userDtoResponse);

        LoggerUtils.info(logger, "User Updated ");

        return ResponseEntity.ok(userDtoResponse);
    }

    private Group getRootGroup(User user) {

        return user.getGroupMemberships().stream()
                .map(GroupMember::getGroup)
                .filter(group -> group.getParent() == null)
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("User does not belong to any root group")
                );
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

        User user = userRepository.findByUserIdWithFacilitator(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found")
                );

        UserDto dto = userMapper.toDto(user);
        enrichFacilitator(dto, user);
        setPhoneParts(user, dto);

        return ResponseEntity.ok(dto);
    }

    private void enrichFacilitator(
            UserDto dto,
            User user
    ) {
        if (user.getFacilitator() != null) {
            User f = user.getFacilitator();
            dto.setFacilitatorId(String.valueOf(f.getUserId()));
            dto.setFacilitatorName(
                    f.getFirstName() + " " + f.getLastName()
            );
        }
    }


    private static void setPhoneParts(
            User user,
            UserDto userDto
    ) {
        if (StringUtils.isNotBlank(userDto.getPhoneNumber())) {
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
            userDto.setRoles(jwt.getClaimAsStringList("roles"));
            userDto.setStatus(UserStatus.valueOf(jwt.getClaimAsString("status")));

            return ResponseEntity.ok(userDto);
        }
        throw new UserFacingException("Unauthorized User");
    }

    private Group getRootGroupForGender(Gender gender) {
        String groupName = switch (gender) {
            case MALE -> GroupConstants.MALE_ROOT;
            case FEMALE -> GroupConstants.FEMALE_ROOT;
            case OTHER -> GroupConstants.OTHER_ROOT;
        };

        return groupRepository.findByName(groupName)
                .orElseThrow(() ->
                        new IllegalStateException("Root group missing: " + groupName)
                );
    }
    private void ensureGenderGroupMembership(User user, Gender gender) {

        if (gender == null) {
            throw new UserFacingException("Gender is required");
        }

        // 🚫 If gender already set → block change
        if (user.getGender() != null && !user.getGender().equals(gender)) {
            throw new UserFacingException(
                    "Gender cannot be changed once set."
            );
        }

        Group targetRootGroup = groupRepository
                .findByName(getRootGroupName(gender))
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Root group missing for gender: " + gender
                        )
                );

        // ✅ First-time setup only
        if (user.getRootGroup() == null) {
            user.setGender(gender);
            user.setRootGroup(targetRootGroup);
            // Ensure exactly ONE root membership exists

            boolean exists = user.getGroupMemberships().stream()
                    .anyMatch(m -> m.getGroup().equals(targetRootGroup));

            if (!exists) {
                GroupMember membership = GroupMember.builder()
                        .group(targetRootGroup)
                        .user(user)
                        .build();

                user.getGroupMemberships().add(membership);
            }
        }
    }

    private String getRootGroupName(Gender gender) {
        return switch (gender) {
            case MALE -> GroupConstants.MALE_ROOT;
            case FEMALE -> GroupConstants.FEMALE_ROOT;
            case OTHER -> GroupConstants.OTHER_ROOT;
        };
    }
}
