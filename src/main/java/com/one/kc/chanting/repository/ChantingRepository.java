package com.one.kc.chanting.repository;

import com.one.kc.chanting.dto.FacilitatorTodayDto;
import com.one.kc.chanting.entity.Chanting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChantingRepository extends JpaRepository<Chanting, Long> {
    Page<Chanting> findByUser_UserId(Long userId, Pageable pageable);


    List<Chanting> findByUser_UserIdAndChantingAtBetweenOrderByChantingAtAsc(
            Long userId,
            Instant fromDate,
            Instant toDate
    );


    @Query("""
        select min(c.chantingAt)
        from Chanting c
        where c.user.userId = :userId
    """)
    Optional<Instant> findMinChantingAtByUserId(@Param("userId") Long userId);

    @Query(
            value = """
       SELECT new com.one.kc.chanting.dto.FacilitatorTodayDto(
            u.firstName,
            u.lastName,
            COALESCE(SUM(c.chantingRounds), 0)
       )
       FROM User u
       LEFT JOIN Chanting c
            ON c.user = u
            AND c.chantingAt BETWEEN :start AND :end
       WHERE u.facilitator.userId = :facilitatorId
       GROUP BY u.userId, u.firstName, u.lastName
       ORDER BY COALESCE(SUM(c.chantingRounds), 0) DESC
       """,
            countQuery = """
       SELECT COUNT(u)
       FROM User u
       WHERE u.facilitator.userId = :facilitatorId
       """
    )
    Page<FacilitatorTodayDto> findFacilitatorUsersTodayChanting(
            @Param("facilitatorId") Long facilitatorId,
            @Param("start") Instant start,
            @Param("end") Instant end,
            Pageable pageable
    );

}
