package com.one.kc.chanting.repository;

import com.one.kc.chanting.entity.Chanting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChantingRepository extends JpaRepository<Chanting, Long> {
    List<Chanting> findByUserId(Long userId);
}
