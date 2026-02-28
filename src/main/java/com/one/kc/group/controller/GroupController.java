package com.one.kc.group.controller;

import com.one.kc.auth.utils.JwtUtil;
import com.one.kc.group.dto.GroupCreateDto;
import com.one.kc.group.dto.GroupListDto;
import com.one.kc.group.entity.Group;
import com.one.kc.group.service.GroupService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class GroupController {

    private final GroupService groupService;

    public GroupController(GroupService groupService) {
        this.groupService = groupService;
    }

    @PostMapping
    public ResponseEntity<Group> createGroup(
            @RequestBody GroupCreateDto dto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long userId = JwtUtil.getUserId(jwt);
        return ResponseEntity.ok(groupService.createGroup(dto, userId));
    }

    @GetMapping
    public ResponseEntity<List<GroupListDto>> myGroups(
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long facilitatorId = JwtUtil.getUserId(jwt);
        return ResponseEntity.ok(groupService.getMyGroups(facilitatorId));
    }
}

