package com.one.kc.user.repository;

import com.one.kc.common.enums.UserRole;
import com.one.kc.user.entity.UserRoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRoleEntity, Long> {

    boolean existsByUser_UserIdAndRole(Long userId, UserRole role);

    @Query("""
        SELECT r.user.userId
        FROM UserRoleEntity r
        WHERE r.role = :role
          AND r.user.userId IN :userIds
    """)
    List<Long> findUserIdsWithRole(
            @Param("role") UserRole role,
            @Param("userIds") List<Long> userIds
    );

    void deleteByRoleAndUser_UserIdIn(UserRole role, List<Long> userIds);

}

