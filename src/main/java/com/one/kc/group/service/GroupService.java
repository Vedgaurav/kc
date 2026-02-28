package com.one.kc.group.service;

import com.one.kc.auth.utils.JwtUtil;
import com.one.kc.common.enums.UserRole;
import com.one.kc.common.exceptions.ResourceNotFoundException;
import com.one.kc.group.dto.GroupCreateDto;
import com.one.kc.group.dto.GroupListDto;
import com.one.kc.group.entity.Group;
import com.one.kc.group.repository.GroupRepository;
import com.one.kc.user.entity.User;
import com.one.kc.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Transactional
public class GroupService {

    private final GroupRepository groupRepository;
    private final UserRepository userRepository;

    public GroupService(GroupRepository groupRepository,
                        UserRepository userRepository) {
        this.groupRepository = groupRepository;
        this.userRepository = userRepository;
    }

    public Group createGroup(GroupCreateDto dto, Long userId) {

        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Group group = new Group();
        group.setName(dto.getName());
        group.setDescription(dto.getDescription());
        group.setCreatedBy(user);

        return groupRepository.save(group);
    }

    public List<GroupListDto> getMyGroups(Long facilitatorId) {
        return groupRepository.findByCreatedBy_UserId(facilitatorId)
                .stream()
                .map(g -> new GroupListDto(g.getGroupId(), g.getName()))
                .toList();
    }
}

