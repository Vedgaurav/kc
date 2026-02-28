package com.one.kc.auth.config;

import com.one.kc.common.constants.GroupConstants;
import com.one.kc.common.enums.Gender;
import com.one.kc.common.enums.UserRole;
import com.one.kc.common.enums.UserStatus;
import com.one.kc.common.utils.SnowflakeIdGenerator;
import com.one.kc.group.entity.Group;
import com.one.kc.group.entity.GroupMember;
import com.one.kc.group.repository.GroupMemberRepository;
import com.one.kc.group.repository.GroupRepository;
import com.one.kc.user.entity.User;
import com.one.kc.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AppBootstrapInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final GroupRepository groupRepository;
    private final AuthConfigProperties authConfigProperties;
    private final SnowflakeIdGenerator idGenerator;
    private final GroupMemberRepository groupMemberRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {

        String email = authConfigProperties
                .getBootstrap()
                .getSuperAdminEmail();

        if (StringUtils.isBlank(email)) {
            log.warn("SUPER_ADMIN_EMAIL not configured. Skipping bootstrap.");
            return;
        }

        User superAdmin = userRepository.findByEmail(email).orElse(null);
        Group maleRoot = createRootGroup(GroupConstants.MALE_ROOT);
        createRootGroup(GroupConstants.FEMALE_ROOT);
        createRootGroup(GroupConstants.OTHER_ROOT);

        if (superAdmin == null) {

            // 1️⃣ Create super admin WITHOUT saving
            superAdmin = buildSuperAdmin(email);

            // 2️⃣ Create root group with superAdmin as creator


            // 3️⃣ Set root group BEFORE saving user
            superAdmin.setRootGroup(maleRoot);

            attachRootMembership(superAdmin, maleRoot);

            ensureAllRoles(superAdmin);

            // 4️⃣ Now save user safely (root_group_id is set)
            userRepository.save(superAdmin);

            log.info("Bootstrap super admin and root group created successfully");

        } else {

            ensureAllRoles(superAdmin);
            userRepository.save(superAdmin);

            log.info("Super admin already exists, roles ensured");
        }
    }

    private void attachRootMembership(User user, Group rootGroup) {

        boolean alreadyExists =
                groupMemberRepository.existsByGroup_GroupIdAndUser_UserId(
                        rootGroup.getGroupId(),
                        user.getUserId()
                );

        if (!alreadyExists) {
            GroupMember membership = GroupMember.builder()
                    .group(rootGroup)
                    .user(user)
                    .build();

            user.getGroupMemberships().add(membership);

        }
    }

    // ------------------------------------------------------

    private User buildSuperAdmin(String email) {

        User user = new User();
        user.setUserId(idGenerator.nextId());
        user.setEmail(email);
        user.setFirstName("SuperAdmin");
        user.setLastName("");
        user.setStatus(UserStatus.ACTIVE);
        user.setGender(Gender.MALE);

        user.addRole(UserRole.SUPER_ADMIN);

        user.setAddBy(1L);
        user.setChgBy(1L);

        return user;
    }

    private void ensureAllRoles(User user) {

        if (!user.hasRole(UserRole.SUPER_ADMIN)) {
            user.addRole(UserRole.SUPER_ADMIN);
        }
        if (!user.hasRole(UserRole.ADMIN)) {
            user.addRole(UserRole.ADMIN);
        }
        if (!user.hasRole(UserRole.FACILITATOR)) {
            user.addRole(UserRole.FACILITATOR);
        }
        if (!user.hasRole(UserRole.USER)) {
            user.addRole(UserRole.USER);
        }

        user.setStatus(UserStatus.ACTIVE);
    }

    private Group createRootGroup(String groupName) {

        return groupRepository.findByName(groupName)
                .orElseGet(() -> {

                    Group group = new Group();
                    group.setName(groupName);
                    group.setGender(getGender(groupName));
                    group.setDescription("System root group for " + getGender(groupName));
                    group.setAddBy(1L);
                    group.setChgBy(1L);

                    return groupRepository.save(group);
                });
    }

    private Gender getGender(String groupName) {
        return switch (groupName) {
            case GroupConstants.FEMALE_ROOT -> Gender.FEMALE;
            case GroupConstants.OTHER_ROOT -> Gender.OTHER;
            default -> Gender.MALE;
        };
    }
}