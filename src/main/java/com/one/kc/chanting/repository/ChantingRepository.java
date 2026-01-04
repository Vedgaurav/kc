package com.one.kc.chanting.repository;

import com.one.kc.chanting.entity.Chanting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChantingRepository extends JpaRepository<Chanting, Long> {
    Page<Chanting> findByUserId(Long userId, Pageable pageable);
}
