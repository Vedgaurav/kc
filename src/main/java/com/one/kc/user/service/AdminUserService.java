package com.one.kc.user.service;

import com.one.kc.chanting.dto.PageResponse;
import com.one.kc.common.enums.RoleAuditAction;
import com.one.kc.common.enums.UserRole;
import com.one.kc.common.exceptions.ResourceNotFoundException;
import com.one.kc.common.exceptions.UserFacingException;
import com.one.kc.common.utils.ResponseEntityUtils;
import com.one.kc.user.dto.AdminUserListDto;
import com.one.kc.user.dto.UserRoleAuditDto;
import com.one.kc.user.entity.User;
import com.one.kc.user.entity.UserRoleAudit;
import com.one.kc.user.repository.UserRepository;
import com.one.kc.user.repository.UserRoleAuditRepository;
import com.one.kc.user.repository.UserRoleRepository;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRoleAuditRepository userRoleAuditRepository;


    public AdminUserService(
            UserRepository userRepository,
            UserRoleRepository userRoleRepository,
            UserRoleAuditRepository userRoleAuditRepository
    ) {
        this.userRepository = userRepository;
        this.userRoleRepository = userRoleRepository;
        this.userRoleAuditRepository = userRoleAuditRepository;
    }

    @Transactional(readOnly = true)
    public ResponseEntity<PageResponse<AdminUserListDto>> getUsers(
            String search,
            Pageable pageable
    ) {
        Page<User> userPage = null;
        if (StringUtils.isBlank(search)) {
            userPage = userRepository.findUsersWithRolesWithoutSearch(
                    pageable
            );
        } else {
            userPage = userRepository.findUsersWithRolesWithSearch(
                    search.trim(),
                    pageable
            );
        }

        List<AdminUserListDto> adminUserListDtoList =
                userPage.get()
                        .map(user ->
                                AdminUserListDto.builder()
                                        .userId(String.valueOf(user.getUserId()))
                                        .email(user.getEmail())
                                        .firstName(user.getFirstName())
                                        .lastName(user.getLastName())
                                        .status(user.getStatus())
                                        .isAdmin(isAdmin(user))
                                        .isFacilitator(isFacilitator(user))
                                        .build()
                        ).toList();

        return ResponseEntityUtils.getPaginatedResponse(userPage, adminUserListDtoList);
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getRole().equals(UserRole.ADMIN));
    }

    private boolean isFacilitator(User user) {
        return user.getRoles().stream().anyMatch(role -> role.getRole().equals(UserRole.FACILITATOR));
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void assignFacilitatorRole(
            List<String> userIdsString,
            Long adminUserId
    ) {

        List<Long> userIds = userIdsString.stream().map(Long::parseLong).toList();

        //Prevent admin from modifying self
        preventSelfModification(userIds, adminUserId);

        // Users who already have role
        List<Long> alreadyHaveRole = findUserIdsWithRole(userIds, UserRole.FACILITATOR);

        Set<Long> existing = new HashSet<>(alreadyHaveRole);

        List<User> userRoleAssignedList = new ArrayList<>();
        List<UserRoleAudit> userRoleAuditList = new ArrayList<>();
        for (Long userId : userIds) {

            if (existing.contains(userId)) {
                continue;
            }

            User user = findUser(userId);

            // Ensure base role
            user.addRole(UserRole.USER);

            // Add FACILITATOR role
            user.addRole(UserRole.FACILITATOR);
            userRoleAssignedList.add(user);

            userRoleAuditList.add(
                    UserRoleAudit.builder()
                            .targetUserId(userId)
                            .actorUserId(adminUserId)
                            .role(UserRole.FACILITATOR)
                            .action(RoleAuditAction.ASSIGNED)
                            .build()
            );

        }
        userRepository.saveAll(userRoleAssignedList);
        userRoleAuditRepository.saveAll(userRoleAuditList);
    }

    private List<Long> findUserIdsWithRole(
            List<Long> userIds,
            UserRole role
    ) {
        return userRoleRepository.findUserIdsWithRole(
                role,
                userIds
        );
    }

    @Transactional
    @PreAuthorize("hasAnyRole('ADMIN','SUPER_ADMIN')")
    public void removeFacilitatorRole(
            List<String> userIdsString,
            Long adminUserId
    ) {
        List<Long> userIds = userIdsString.stream()
                .map(Long::parseLong)
                .toList();

        preventSelfModification(userIds, adminUserId);

        List<Long> userIdsWithRole = findUserIdsWithRole(userIds, UserRole.FACILITATOR);

        userRoleRepository.deleteByRoleAndUser_UserIdIn(
                UserRole.FACILITATOR,
                userIdsWithRole
        );
        List<UserRoleAudit> userRoleAuditList = userIdsWithRole.stream()
                .map(userId ->
                        UserRoleAudit.builder()
                                .targetUserId(userId)
                                .actorUserId(adminUserId)
                                .role(UserRole.FACILITATOR)
                                .action(RoleAuditAction.REMOVED)
                                .build()
                )
                .toList();
        userRoleAuditRepository.saveAll(userRoleAuditList);

    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void assignAdminRole(
            List<String> userIdsString,
            Long superAdminUserId
    ) {
        List<Long> userIds = userIdsString.stream()
                .map(Long::parseLong)
                .toList();

        preventSelfModification(userIds, superAdminUserId);

        List<Long> alreadyAdmins =
                userRoleRepository.findUserIdsWithRole(
                        UserRole.ADMIN,
                        userIds
                );

        Set<Long> existing = new HashSet<>(alreadyAdmins);

        List<User> userRoleAssignedList = new ArrayList<>();
        List<UserRoleAudit> userRoleAuditList = new ArrayList<>();
        for (Long userId : userIds) {

            if (existing.contains(userId)) continue;

            User user = findUser(userId);

            user.addRole(UserRole.USER);
            user.addRole(UserRole.ADMIN);

            userRoleAssignedList.add(user);
            userRoleAuditList.add(
                    UserRoleAudit.builder()
                            .targetUserId(userId)
                            .actorUserId(superAdminUserId)
                            .role(UserRole.ADMIN)
                            .action(RoleAuditAction.ASSIGNED)
                            .build()
            );

        }
        userRepository.saveAll(userRoleAssignedList);
        userRoleAuditRepository.saveAll(userRoleAuditList);
    }

    @Transactional
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public void removeAdminRole(
            List<String> userIdsString,
            Long superAdminUserId
    ) {
        List<Long> userIds = userIdsString.stream()
                .map(Long::parseLong)
                .toList();

        preventSelfModification(userIds, superAdminUserId);
        List<Long> userIdsWithRole = findUserIdsWithRole(userIds, UserRole.FACILITATOR);

        userRoleRepository.deleteByRoleAndUser_UserIdIn(
                UserRole.ADMIN,
                userIds
        );
        List<UserRoleAudit> userRoleAuditList = userIdsWithRole.stream()
                .map(userId ->
                        UserRoleAudit.builder()
                                .targetUserId(userId)
                                .actorUserId(superAdminUserId)
                                .role(UserRole.FACILITATOR)
                                .action(RoleAuditAction.REMOVED)
                                .build()
                )
                .toList();
        userRoleAuditRepository.saveAll(userRoleAuditList);
    }

    private void preventSelfModification(
            List<Long> userIds,
            Long actorId
    ) {
        if (userIds.contains(actorId)) {
            throw new UserFacingException(
                    "You cannot modify your own roles"
            );
        }
    }

    private User findUser(Long userId) {
        return userRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "User not found: " + userId
                        )
                );
    }

    public ResponseEntity<PageResponse<UserRoleAuditDto>> getAuditHistory(Pageable pageable) {
       Page<UserRoleAuditDto> userRoleAuditListPage =  userRoleAuditRepository.findAllUserRoleAuditHistory(pageable);

       return ResponseEntityUtils.getPaginatedResponse(userRoleAuditListPage, userRoleAuditListPage.getContent());
    }
}
