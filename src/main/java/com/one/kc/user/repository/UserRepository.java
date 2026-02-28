package com.one.kc.user.repository;

import com.one.kc.common.enums.UserRole;
import com.one.kc.common.enums.UserStatus;
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

    // =========================================================
    // 🔥 ADMIN - Fetch users in same root group
    // =========================================================

    @EntityGraph(attributePaths = {"roles"})
    @Query("""
        SELECT u
        FROM User u
        WHERE u.rootGroup.groupId = (
            SELECT admin.rootGroup.groupId
            FROM User admin
            WHERE admin.userId = :adminId
        )
        ORDER BY u.firstName
    """)
    Page<User> findUsersInSameRootGroup(
            @Param("adminId") Long adminId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"roles"})
    @Query("""
        SELECT u
        FROM User u
        WHERE u.rootGroup.groupId = (
            SELECT admin.rootGroup.groupId
            FROM User admin
            WHERE admin.userId = :adminId
        )
        AND u.firstName LIKE CONCAT('%', :query, '%')
        ORDER BY
            CASE
                WHEN u.firstName = :query THEN 1
                WHEN u.firstName LIKE CONCAT(:query, '%') THEN 2
                ELSE 3
            END,
            u.firstName
    """)
    Page<User> findUsersInSameRootGroupWithSearch(
            @Param("adminId") Long adminId,
            @Param("query") String query,
            Pageable pageable
    );

    // =========================================================
    // 🔥 FACILITATOR LIST (same root group)
    // =========================================================

    @Query("""
        SELECT u
        FROM User u
        JOIN u.roles r
        WHERE r.role = :role
          AND u.status = 'ACTIVE'
          AND u.userId <> :requesterId
          AND u.rootGroup.groupId = (
                SELECT requester.rootGroup.groupId
                FROM User requester
                WHERE requester.userId = :requesterId
          )
    """)
    List<User> findActiveFacilitatorsInSameRootGroup(
            @Param("role") UserRole role,
            @Param("requesterId") Long requesterId
    );

    // =========================================================
    // Facilitator → Assigned Users
    // =========================================================

    @Query("""
        SELECT u
        FROM User u
        WHERE u.facilitator.userId = :facilitatorId
    """)
    Page<User> findUsersByFacilitator(
            @Param("facilitatorId") Long facilitatorId,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "roles",
            "facilitator",
            "rootGroup"
    })
        @Query("""
        SELECT u
        FROM User u
        WHERE u.userId = :userId
    """)
    Optional<User> findByUserIdWithFacilitator(
            @Param("userId") Long userId
    );

    @Query("""
        SELECT u
        FROM User u
        JOIN u.roles r
        WHERE r.role = :role
          AND u.status = UserStatus.ACTIVE
          AND u.userId <> :requesterId
          AND u.rootGroup.groupId = (
                SELECT requester.rootGroup.groupId
                FROM User requester
                WHERE requester.userId = :requesterId
          )
    """)
    List<User> findActiveFacilitatorsInSameGroup(
            @Param("role") UserRole role,
            @Param("requesterId") Long requesterId
    );


    @Query("""
        SELECT u
        FROM User u
        WHERE u.userId IN :userIds
          AND u.rootGroup.groupId = :rootGroupId
    """)
    List<User> findAllByIdsAndRootGroup(
            @Param("userIds") List<Long> userIds,
            @Param("rootGroupId") Long rootGroupId
    );

    @EntityGraph(attributePaths = {"roles"})
        @Query("""
        SELECT u
        FROM User u
        ORDER BY u.firstName
    """)
    Page<User> findAllWithRoles(Pageable pageable);

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
    Page<User> findAllWithRolesAndSearch(
            @Param("query") String query,
            Pageable pageable
    );
}