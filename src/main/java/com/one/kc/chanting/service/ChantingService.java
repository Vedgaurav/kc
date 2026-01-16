package com.one.kc.chanting.service;

import com.one.kc.chanting.dto.ChantingDashboardResponseDto;
import com.one.kc.chanting.dto.ChantingDto;
import com.one.kc.chanting.dto.PageResponse;
import com.one.kc.chanting.entity.Chanting;
import com.one.kc.chanting.mapper.ChantingMapper;
import com.one.kc.chanting.repository.ChantingRepository;
import com.one.kc.common.exceptions.ResourceNotFoundException;
import com.one.kc.common.exceptions.UserFacingException;
import com.one.kc.common.utils.LoggerUtils;
import com.one.kc.common.utils.ResponseEntityUtils;
import com.one.kc.common.utils.SnowflakeIdGenerator;
import com.one.kc.user.entity.User;
import com.one.kc.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.toList;

@Service
public class ChantingService {

    private static final Logger logger =
            LoggerFactory.getLogger(ChantingService.class);

    private static final int IDEAL_ROUNDS = 16;

    private final ChantingRepository chantingRepository;
    private final ChantingMapper chantingMapper;
    private final SnowflakeIdGenerator idGenerator;
    private final UserRepository userRepository;

    public ChantingService(
            ChantingRepository chantingRepository,
            ChantingMapper chantingMapper,
            SnowflakeIdGenerator idGenerator,
            UserRepository userRepository
    ) {
        this.chantingRepository = chantingRepository;
        this.chantingMapper = chantingMapper;
        this.idGenerator = idGenerator;
        this.userRepository = userRepository;
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
    public ResponseEntity<ChantingDto> createChanting(
            ChantingDto chantingDto,
            Jwt jwt
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        LocalDate date = chantingDto.getChantingDate();
        int newRounds = chantingDto.getChantingRounds();

        Chanting chanting = chantingRepository.findByUserIdAndChantingDate(userId, date)
                .map(existing -> {
                    existing.setChantingRounds(existing.getChantingRounds() + newRounds);
                    return existing;
                })
                .orElseGet(() -> {
                    Chanting newRecord = chantingMapper.toEntity(chantingDto);
                    newRecord.setChantingId(idGenerator.nextId());
                    newRecord.setUserId(userId);
                    return newRecord;
                });

        Chanting saved = chantingRepository.save(chanting);

        LoggerUtils.info(logger, "Chanting record updated/created for on date: {}", date);

        return ResponseEntityUtils.getCreatedResponse(chantingMapper.toDto(saved));
    }

    /**
     * Updates an existing chanting record.
     *
     * <p>
     * Performs partial update using MapStruct.
     * </p>
     *
     * @param jwt  auth filter
     * @param chantingDto updated details
     * @return updated {@link ChantingDto}
     */
    @Transactional
    public ResponseEntity<ChantingDto> updateChanting(
           Jwt jwt,
            ChantingDto chantingDto
    ) {
        Long userId = Long.parseLong(jwt.getSubject());
        LocalDate date = chantingDto.getChantingDate();
        LocalDate currentDate = LocalDate.now();
        if(currentDate.minusDays(5).isAfter(date)) {
            throw new UserFacingException("Update Not allowed for date " + date);
        }
        int newRounds = chantingDto.getChantingRounds();


        Chanting existing = chantingRepository.findByUserIdAndChantingDate(userId, date)
                .map(existingChanting -> {
                    existingChanting.setChantingRounds(newRounds);
                    return  existingChanting;
                })
                .orElseThrow(() ->  new UserFacingException("Chanting not found"));

        Chanting updated = chantingRepository.save(existing);

        LoggerUtils.info(logger,
                "Chanting record updated with id: {}", updated.getChantingRounds());

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
    public ResponseEntity<PageResponse<ChantingDto>> getChantingListByUserId(
            Long userId,
            Pageable pageable
    ) {

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

        Chanting chanting = chantingRepository.findById(chantingId).orElseThrow(
                () -> new UserFacingException("Chanting not found ")
        );
        LocalDate currentDate = LocalDate.now();
        if(currentDate.minusDays(5).isAfter(chanting.getChantingDate())) {
            throw new UserFacingException("Update Not allowed for date " + chanting.getChantingDate());
        }

        chantingRepository.deleteById(chantingId);

        LoggerUtils.info(logger,
                "Chanting record deleted with id: {}", chantingId);

        return ResponseEntity.noContent().build();
    }

    public ChantingDashboardResponseDto getDashboard(
            Long userId,
            LocalDate fromDate,
            LocalDate toDate
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Default range → last 30 days
        LocalDate to = toDate != null ? toDate : LocalDate.now();
        LocalDate from = fromDate != null ? fromDate : to.minusDays(30);

        List<Chanting> chantingList =
                chantingRepository.findByUserIdAndChantingDateBetweenOrderByChantingDateAsc(
                        userId, from, to
                );

        List<ChantingDto> records = chantingList.stream()
                .map(chantingMapper::toDto
                )
                .toList();

        return new ChantingDashboardResponseDto(
                user.getCommittedRounds(),
                IDEAL_ROUNDS,
                calculateStreak(chantingList, user.getCommittedRounds()),
                calculateAverage(chantingList),
                records
        );
    }


    private BigDecimal calculateAverage(List<Chanting> list) {
        if (list == null || list.isEmpty()) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        
        BigDecimal sum = list.stream()
                .map(Chanting::getChantingRounds)
                .map(BigDecimal::valueOf)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(
                BigDecimal.valueOf(list.size()),
                2,
                RoundingMode.HALF_UP
        );
    }

    /**
     * Streak = consecutive days (ending today or yesterday)
     * where chanting >= committedRounds
     */
    private Integer calculateStreak(
            List<Chanting> list,
            Integer committed
    ) {

        if (CollectionUtils.isEmpty(list)) return 0;

        int streak = 0;
        LocalDate expectedDate = LocalDate.now();

        for (int i = list.size() - 1; i >= 0; i--) {
            Chanting c = list.get(i);

            if (!c.getChantingDate().equals(expectedDate)
                    && !c.getChantingDate().equals(expectedDate.minusDays(1))) {
                break;
            }

            if (c.getChantingRounds() < committed) {
                break;
            }

            streak++;
            expectedDate = c.getChantingDate().minusDays(1);
        }

        return streak;
    }
}

