package com.one.kc.auth.config;

import com.one.kc.common.enums.UserRole;
import com.one.kc.common.enums.UserStatus;
import com.one.kc.common.utils.SnowflakeIdGenerator;
import com.one.kc.user.entity.User;
import com.one.kc.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
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
public class SuperAdminInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final AuthConfigProperties authConfigProperties;
    private final SnowflakeIdGenerator idGenerator;

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

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> createSuperAdminUser(email));

        if (!user.hasRole(UserRole.SUPER_ADMIN)) {
            user.addRole(UserRole.SUPER_ADMIN);
            log.info("Assigned SUPER_ADMIN role to Super User");
        }else{
            log.info("Super Admin ready");
            return;
        }

        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
    }

    private User createSuperAdminUser(String email) {

        User user = new User();
        user.setUserId(idGenerator.nextId());
        user.setEmail(email);
        user.setStatus(UserStatus.ACTIVE);

        user.addRole(UserRole.SUPER_ADMIN);

        log.info("Creating SuperAdmin user ");
        return user;
    }
}

