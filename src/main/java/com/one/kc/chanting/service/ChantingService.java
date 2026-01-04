package com.one.kc.chanting.service;

import com.one.kc.chanting.dto.ChantingDto;
import com.one.kc.chanting.dto.PageResponse;
import com.one.kc.chanting.entity.Chanting;
import com.one.kc.chanting.mapper.ChantingMapper;
import com.one.kc.chanting.repository.ChantingRepository;
import com.one.kc.common.exceptions.ResourceNotFoundException;
import com.one.kc.common.utils.LoggerUtils;
import com.one.kc.common.utils.ResponseEntityUtils;
import com.one.kc.common.utils.SnowflakeIdGenerator;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class ChantingService {

    private static final Logger logger =
            LoggerFactory.getLogger(ChantingService.class);

    private final ChantingRepository chantingRepository;
    private final ChantingMapper chantingMapper;
    private final SnowflakeIdGenerator idGenerator;

    public ChantingService(ChantingRepository chantingRepository,
                           ChantingMapper chantingMapper,
                           SnowflakeIdGenerator idGenerator) {
        this.chantingRepository = chantingRepository;
        this.chantingMapper = chantingMapper;
        this.idGenerator = idGenerator;
    }

    /**
     * Creates a new chanting record.
     *
     * <p>
     * - Generates a unique chanting ID using Snowflake
     * - Maps DTO to entity
     * - Persists the record
     * </p>
     *
     * @param chantingDto chanting details
     * @return created {@link ChantingDto}
     */
    public ResponseEntity<ChantingDto> createChanting(ChantingDto chantingDto, Jwt jwt) {

        Chanting chanting = chantingMapper.toEntity(chantingDto);
        chanting.setChantingId(idGenerator.nextId());
        chanting.setUserId(Long.parseLong(jwt.getSubject()));

        Chanting saved = chantingRepository.save(chanting);

        LoggerUtils.info(logger,
                "Chanting record created with id: {}", saved.getChantingId());

        return ResponseEntityUtils.getCreatedResponse(
                chantingMapper.toDto(saved));
    }

    /**
     * Updates an existing chanting record.
     *
     * <p>
     * Performs partial update using MapStruct.
     * </p>
     *
     * @param chantingId chanting identifier
     * @param chantingDto updated details
     * @return updated {@link ChantingDto}
     */
    @Transactional
    public ResponseEntity<ChantingDto> updateChanting(
            Long chantingId, ChantingDto chantingDto) {

        Chanting existing = chantingRepository.findById(chantingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chanting not found with id: " + chantingId));

        chantingMapper.updateEntityFromDto(chantingDto, existing);

        Chanting updated = chantingRepository.save(existing);

        LoggerUtils.info(logger,
                "Chanting record updated with id: {}", chantingId);

        return ResponseEntity.ok(chantingMapper.toDto(updated));
    }

    /**
     * Retrieves a chanting record by ID.
     *
     * @param chantingId chanting identifier
     * @return {@link ChantingDto}
     */
    public ResponseEntity<ChantingDto> getChantingById(Long chantingId) {

        Chanting chanting = chantingRepository.findById(chantingId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Chanting not found with id: " + chantingId));

        return ResponseEntity.ok(chantingMapper.toDto(chanting));
    }

    /**
     * Retrieves  chanting records by user email.
     *
     * @param userId chanting identifier
     * @return {@link ChantingDto}
     */
    public ResponseEntity<PageResponse<ChantingDto>> getChantingListByUserId(Long userId, Pageable pageable) {

        Page<Chanting> chantingListPage = chantingRepository.findByUserId(userId, pageable);

        List<ChantingDto> chantingDtoList = chantingListPage.getContent().stream().map(chantingMapper::toDto).toList();

        return ResponseEntityUtils.getPaginatedResponse(chantingListPage, chantingDtoList);
    }

    /**
     * Permanently deletes a chanting record.
     *
     * <p>
     * ⚠️ This operation is irreversible.
     * </p>
     *
     * @param chantingId chanting identifier
     * @return HTTP 204 No Content
     */
    public ResponseEntity<Void> deleteChanting(Long chantingId) {

        if (!chantingRepository.existsById(chantingId)) {
            throw new ResourceNotFoundException(
                    "Chanting not found with id: " + chantingId);
        }

        chantingRepository.deleteById(chantingId);

        LoggerUtils.info(logger,
                "Chanting record deleted with id: {}", chantingId);

        return ResponseEntity.noContent().build();
    }
}

