package com.one.kc.user.controller;

import com.one.kc.auth.utils.JwtUtil;
import com.one.kc.chanting.dto.PageResponse;
import com.one.kc.user.dto.AdminUserListDto;
import com.one.kc.user.dto.AssignFacilitatorRoleRequest;
import com.one.kc.user.dto.UserRoleAuditDto;
import com.one.kc.user.service.AdminUserService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN') or hasRole('SUPER_ADMIN')")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Get all users with pagination and optional search by firstName
     * Example:
     * GET /api/admin/users?page=0&size=20&search=ram
     */
    @GetMapping
    public ResponseEntity<PageResponse<AdminUserListDto>> getUsers(
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20) Pageable pageable,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long adminId = JwtUtil.getUserId(jwt);
        return adminUserService.getUsers(adminId, search, pageable);
    }

    /**
     * Get all users with pagination and optional search by firstName
     * Example:
     * GET /api/admin/users?page=0&size=20&search=ram
     */
    @GetMapping("/audit")
    public ResponseEntity<PageResponse<UserRoleAuditDto>> getAuditHistory(
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "chantingAt",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        return adminUserService.getAuditHistory(pageable);
    }

    /**
     * Assign FACILITATOR role to one or more users
     */
    @PostMapping("/assign-facilitator")
    public ResponseEntity<Void> assignFacilitatorRole(
            @RequestBody AssignFacilitatorRoleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long adminUserId = JwtUtil.getUserId(jwt);

        adminUserService.assignFacilitatorRole(
                request.getUserIds(),
                adminUserId
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/assign-admin")
    public ResponseEntity<Void> assignAdmin(
            @RequestBody AssignFacilitatorRoleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        adminUserService.assignAdminRole(
                request.getUserIds(),
                JwtUtil.getUserId(jwt)
        );
        return ResponseEntity.ok().build();
    }

    /**
     * Remove FACILITATOR role from one or more users
     */
    @PostMapping("/remove-facilitator")
    public ResponseEntity<Void> removeFacilitatorRole(
            @RequestBody AssignFacilitatorRoleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        Long adminUserId = JwtUtil.getUserId(jwt);

        adminUserService.removeFacilitatorRole(
                request.getUserIds(),
                adminUserId
        );

        return ResponseEntity.ok().build();
    }

    @PostMapping("/remove-admin")
    public ResponseEntity<Void> removeAdmin(
            @RequestBody AssignFacilitatorRoleRequest request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        adminUserService.removeAdminRole(
                request.getUserIds(),
                JwtUtil.getUserId(jwt)
        );
        return ResponseEntity.ok().build();
    }
}
