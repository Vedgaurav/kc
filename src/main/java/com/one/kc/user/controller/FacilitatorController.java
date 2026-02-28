package com.one.kc.user.controller;


import com.one.kc.auth.utils.JwtUtil;
import com.one.kc.chanting.dto.ChantingDashboardResponseDto;
import com.one.kc.chanting.dto.ChantingDto;
import com.one.kc.chanting.dto.PageResponse;
import com.one.kc.user.dto.FacilitatorListDto;
import com.one.kc.user.dto.FacilitatorUserListDto;
import com.one.kc.user.service.FacilitatorService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/facilitator")
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN', 'FACILITATOR', 'USER')")
public class FacilitatorController {

    private final FacilitatorService facilitatorService;

    public FacilitatorController(FacilitatorService facilitatorService) {
        this.facilitatorService = facilitatorService;
    }

    @GetMapping
    public ResponseEntity<List<FacilitatorListDto>> getFacilitators(@AuthenticationPrincipal Jwt jwt) {
        Long userId = JwtUtil.getUserId(jwt);
        return ResponseEntity.ok(facilitatorService.getFacilitators(userId));
    }

    @GetMapping("/users")
    public ResponseEntity<PageResponse<FacilitatorUserListDto>> getFacilitatorsUsers(@AuthenticationPrincipal Jwt jwt, @PageableDefault(
            sort = "chantingAt",
            direction = Sort.Direction.DESC
    )
    Pageable pageable) {
        Long userId = JwtUtil.getUserId(jwt);
        return facilitatorService.getFacilitatorUsers(userId, pageable);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<PageResponse<ChantingDto>>  getFacilitatorsUserDetails(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(
                    sort = "chantingDate",
            direction = Sort.Direction.DESC
    )
    Pageable pageable) {
        Long facilitatorId = JwtUtil.getUserId(jwt);
        return facilitatorService.getFacilitatorUserChantingListByUserId(facilitatorId, userId, pageable);
    }

    @GetMapping("/user/{userId}/dashboard")
    public ResponseEntity<ChantingDashboardResponseDto>  getFacilitatorsUserDashboardDetails(
            @PathVariable Long userId,
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate) {
        Long facilitatorId = JwtUtil.getUserId(jwt);
          return ResponseEntity.ok(facilitatorService.getFacilitatorsUserDashboardDetails(facilitatorId, userId, fromDate, toDate));
    }
}