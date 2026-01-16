package com.one.kc.chanting.repository;

import com.one.kc.chanting.entity.Chanting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ChantingRepository extends JpaRepository<Chanting, Long> {
    Page<Chanting> findByUserId(Long userId, Pageable pageable);

    Optional<Chanting> findByUserIdAndChantingDate(
            Long userId,
            LocalDate chantingDate
    );

    List<Chanting> findByUserIdAndChantingDateBetweenOrderByChantingDateAsc(
            Long userId,
            LocalDate fromDate,
            LocalDate toDate
    );
}
