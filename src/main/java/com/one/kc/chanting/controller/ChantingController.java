package com.one.kc.chanting.controller;

import com.one.kc.auth.utils.JwtUtil;
import com.one.kc.chanting.dto.ChantingDto;
import com.one.kc.chanting.dto.PageResponse;
import com.one.kc.chanting.service.ChantingService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * @param chantingId  unique identifier of the chanting record
     * @param chantingDto updated chanting details
     * @return updated {@link ChantingDto}
     */
    @PutMapping("/{chantingId}")
    public ResponseEntity<ChantingDto> updateChanting(
            @PathVariable Long chantingId,
            @RequestBody ChantingDto chantingDto
    ) {
        return chantingService.updateChanting(chantingId, chantingDto);
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

