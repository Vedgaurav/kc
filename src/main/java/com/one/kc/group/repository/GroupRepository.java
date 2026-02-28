package com.one.kc.group.repository;

import com.one.kc.group.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GroupRepository extends JpaRepository<Group, Long> {
    List<Group> findByCreatedBy_UserId(Long userId);
    Optional<Group> findByName(String name);
}

