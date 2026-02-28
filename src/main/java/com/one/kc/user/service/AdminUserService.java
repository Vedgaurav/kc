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
            Long adminId,
            String search,
            Pageable pageable
    ) {

        User admin = userRepository.findByUserIdWithFacilitator(adminId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        boolean isSuperAdmin = admin.hasRole(UserRole.SUPER_ADMIN);

        Page<User> userPage;

        if (isSuperAdmin) {

            // 🔥 SUPER_ADMIN → No tenant restriction
            if (StringUtils.isBlank(search)) {
                userPage = userRepository.findAllWithRoles(pageable);
            } else {
                userPage = userRepository.findAllWithRolesAndSearch(
                        search.trim(),
                        pageable
                );
            }

        } else {

            // 🔒 ADMIN → Restrict to same rootGroup
            if (StringUtils.isBlank(search)) {
                userPage = userRepository.findUsersInSameRootGroup(
                        adminId,
                        pageable
                );
            } else {
                userPage = userRepository.findUsersInSameRootGroupWithSearch(
                        adminId,
                        search.trim(),
                        pageable
                );
            }
        }

        List<AdminUserListDto> adminUserListDtoList =
                userPage.stream()
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
                        )
                        .toList();

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

        List<Long> userIds = userIdsString.stream()
                .map(Long::parseLong)
                .toList();

        // Prevent admin from modifying self
        preventSelfModification(userIds, adminUserId);

        // 🔥 Fetch admin with rootGroup
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        Long adminRootGroupId = admin.getRootGroup().getGroupId();

        // 🔥 Fetch all target users in one query
        List<User> users = userRepository.findAllById(userIds);

        if (users.size() != userIds.size()) {
            throw new ResourceNotFoundException("One or more users not found");
        }

        // 🔥 ROOT GROUP VALIDATION
        for (User user : users) {
            if (!user.getRootGroup().getGroupId().equals(adminRootGroupId)) {
                throw new IllegalStateException(
                        "Cannot assign facilitator role to user outside your root group"
                );
            }
        }

        // Users who already have role
        List<Long> alreadyHaveRole =
                findUserIdsWithRole(userIds);

        Set<Long> existing = new HashSet<>(alreadyHaveRole);

        List<User> userRoleAssignedList = new ArrayList<>();
        List<UserRoleAudit> userRoleAuditList = new ArrayList<>();

        for (User user : users) {

            if (existing.contains(user.getUserId())) {
                continue;
            }

            // Ensure base role
            user.addRole(UserRole.USER);

            // Add FACILITATOR role
            user.addRole(UserRole.FACILITATOR);

            userRoleAssignedList.add(user);

            userRoleAuditList.add(
                    UserRoleAudit.builder()
                            .targetUserId(user.getUserId())
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
            List<Long> userIds
    ) {
        return userRoleRepository.findUserIdsWithRole(
                UserRole.FACILITATOR,
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

        // 🔥 Fetch admin
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Admin not found"));

        Long adminRootGroupId = admin.getRootGroup().getGroupId();

        // 🔥 Validate users belong to same root group (DB level)
        List<User> validUsers =
                userRepository.findAllByIdsAndRootGroup(userIds, adminRootGroupId);

        if (validUsers.size() != userIds.size()) {
            throw new IllegalStateException(
                    "Cannot remove facilitator role from users outside your root group"
            );
        }

        // 🔥 Get only those users who actually have FACILITATOR role
        List<Long> userIdsWithRole =
                findUserIdsWithRole(userIds);

        if (userIdsWithRole.isEmpty()) {
            return; // nothing to remove
        }

        // 🔥 Remove role
        userRoleRepository.deleteByRoleAndUser_UserIdIn(
                UserRole.FACILITATOR,
                userIdsWithRole
        );

        // 🔥 Audit entries
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
        List<Long> userIdsWithRole = findUserIdsWithRole(userIds);

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
