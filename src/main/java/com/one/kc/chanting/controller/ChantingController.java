package com.one.kc.chanting.controller;

import com.one.kc.auth.utils.JwtUtil;
import com.one.kc.chanting.dto.ChantingDashboardResponseDto;
import com.one.kc.chanting.dto.ChantingDto;
import com.one.kc.chanting.dto.PageResponse;
import com.one.kc.chanting.service.ChantingService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * REST controller for managing Chanting records.
 *
 * <p>
 * Provides APIs to create, update, retrieve and delete
 * chanting information.
 * </p>
 */
@RestController
@RequestMapping("/api/chanting")
public class ChantingController {

    private final ChantingService chantingService;

    public ChantingController(ChantingService chantingService) {
        this.chantingService = chantingService;
    }

    /**
     * Creates a new chanting record.
     *
     * <p>
     * Generates a unique chanting ID and persists the record.
     * </p>
     *
     * @param chantingDto chanting details
     * @return {@link ResponseEntity} containing created {@link ChantingDto}
     */
    @PostMapping
    public ResponseEntity<ChantingDto> createChanting(
            @RequestBody ChantingDto chantingDto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return chantingService.createChanting(chantingDto, jwt);
    }

    /**
     * Updates an existing chanting record.
     *
     * <p>
     * Performs a partial update on the chanting record.
     * </p>
     *
     * @param jwt  user auth token
     * @param chantingDto updated chanting details
     * @return updated {@link ChantingDto}
     */
    @PutMapping
    public ResponseEntity<ChantingDto> updateChanting(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody ChantingDto chantingDto
    ) {
        return chantingService.updateChanting(jwt, chantingDto);
    }

    /**
     * Retrieves a chanting record by ID.
     *
     * @param chantingId unique identifier of the chanting record
     * @return {@link ChantingDto} for the requested chanting record
     */
    @GetMapping("/{chantingId}")
    public ResponseEntity<ChantingDto> getChantingById(@PathVariable Long chantingId) {
        return chantingService.getChantingById(chantingId);
    }

    /**
     * Retrieves all chanting records by ID.
     *
     * @return {@link ChantingDto} for the requested chanting record
     */
    @GetMapping
    public ResponseEntity<PageResponse<ChantingDto>> getChantingListByUserId(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(
                    page = 0,
                    size = 10,
                    sort = "chantingDate",
                    direction = Sort.Direction.DESC
            )
            Pageable pageable
    ) {
        Long userId = JwtUtil.getUserId(jwt);
        return chantingService.getChantingListByUserId(userId, pageable);
    }

    /**
     * Dashboard data for chanting chart & stats
     */
    @GetMapping("/dashboard")
    public ResponseEntity<ChantingDashboardResponseDto> getDashboard(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        Long userId = JwtUtil.getUserId(jwt);
        return ResponseEntity.ok(
                chantingService.getDashboard(userId, fromDate, toDate)
        );
    }


    /**
     * Permanently deletes a chanting record.
     *
     * <p>
     * ⚠️ This operation is irreversible.
     * </p>
     *
     * @param chantingId unique identifier of the chanting record
     * @return HTTP 204 No Content on successful deletion
     */
    @DeleteMapping("/{chantingId}")
    public ResponseEntity<Void> deleteChanting(@PathVariable Long chantingId) {
        return chantingService.deleteChanting(chantingId);
    }


}

