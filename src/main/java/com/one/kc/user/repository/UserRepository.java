package com.one.kc.user.repository;

import com.one.kc.common.enums.UserRole;
import com.one.kc.common.enums.UserStatus;
import com.one.kc.user.dto.AdminUserList;
import com.one.kc.user.dto.AdminUserListDto;
import com.one.kc.user.dto.FacilitatorListDto;
import com.one.kc.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmail(String email);
    Optional<User> findByEmail(String email);
    Optional<User> findByUserId(Long userId);
    Optional<User> findByEmailAndStatus(String email, UserStatus userStatus);

    @Query("""
    SELECT u FROM User u
    LEFT JOIN FETCH u.facilitator
    WHERE u.userId = :userId
    """)
    Optional<User> findByUserIdWithFacilitator(Long userId);

    @EntityGraph(attributePaths = {"roles"})
    @Query("""
    SELECT u
    FROM User u
    ORDER BY u.firstName
    """)
    Page<User> findUsersWithRolesWithoutSearch(Pageable pageable);


    @EntityGraph(attributePaths = {"roles"})
    @Query("""
    SELECT u
    FROM User u
    WHERE u.firstName LIKE CONCAT('%', :query, '%')
    ORDER BY
        CASE
            WHEN u.firstName = :query THEN 1
            WHEN u.firstName LIKE CONCAT(:query, '%') THEN 2
            ELSE 3
        END,
        u.firstName
    """)
    Page<User> findUsersWithRolesWithSearch(
            @Param("query") String query,
            Pageable pageable
    );


    @Query("""
    SELECT DISTINCT u
    FROM User u
    JOIN u.roles r
    WHERE r.role = :role
      AND u.status = 'ACTIVE'
    """)
    List<User> findActiveUsersByRole(UserRole role);

    @Query("""
        SELECT u
        FROM User u
        WHERE u.facilitator.userId = :facilitatorId
    """)
    Page<User> findUsersByFacilitator(
            Long facilitatorId,
            Pageable pageable
    );


}
