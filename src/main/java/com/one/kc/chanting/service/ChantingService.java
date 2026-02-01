package com.one.kc.chanting.service;

import com.one.kc.chanting.dto.ChantingDashboardResponseDto;
import com.one.kc.chanting.dto.ChantingDto;
import com.one.kc.chanting.dto.DashboardDto;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ChantingService {

    private static final Logger logger =
            LoggerFactory.getLogger(ChantingService.class);

    private static final int IDEAL_ROUNDS = 16;
    public static final int BEADS_IN_ONE_ROUND = 108;

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

//    /**
//     * Retrieves a chanting record by ID.
//     *
//     * @param chantingId chanting identifier
//     * @return {@link ChantingDto}
//     */
//    public ResponseEntity<ChantingDto> getChantingById(Long chantingId) {
//
//        Chanting chanting = chantingRepository.findById(chantingId)
//                .orElseThrow(() -> new ResourceNotFoundException(
//                        "Chanting not found with id: " + chantingId));
//
//        return ResponseEntity.ok(chantingMapper.toDto(chanting));
//    }

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

        // Resolve end date
        LocalDate to = Objects.requireNonNullElseGet(toDate, LocalDate::now);

        // Resolve start date
        LocalDate from;
            // "All" → start from earliest real chanting date
        // user has no data
        from = Objects.requireNonNullElseGet(fromDate, () -> chantingRepository
                .findMinChantingDateByUserId(userId)
                .orElse(to));

        // Safety guard
        if (from.isAfter(to)) {
            from = to;
        }

        // Fetch chanting records (uses idx_chanting_user_date)
        List<Chanting> chantingList =
                chantingRepository.findByUserIdAndChantingDateBetweenOrderByChantingDateAsc(
                        userId, from, to
                );

      int totalRounds = chantingList.stream()
                .mapToInt(Chanting::getChantingRounds)
                .sum();

        // Build full time series
        List<DashboardDto> chantingRecords =
                buildTimeSeries(chantingList, from, to);



        return  ChantingDashboardResponseDto.builder()
                        .committedRounds( user.getCommittedRounds())
                .idealRounds(IDEAL_ROUNDS)
                .currentStreak(calculateStreak(chantingRecords, user.getCommittedRounds()))
                .averageRounds(calculateAverage(chantingRecords))
                .chantingDtoList(chantingRecords)
                .totalRounds(totalRounds)
                .totalMahamantras(totalRounds * BEADS_IN_ONE_ROUND)
                .build();

    }


    private List<DashboardDto> buildTimeSeries(
            List<Chanting> chantingList,
            LocalDate from,
            LocalDate to
    ) {
        // Map existing DB records by date
        Map<LocalDate, Chanting> chantingMap = chantingList.stream()
                .collect(Collectors.toMap(
                        Chanting::getChantingDate,
                        Function.identity()
                ));

        List<DashboardDto> result = new ArrayList<>();

        LocalDate current = from;
        while (!current.isAfter(to)) {

            Chanting chanting = chantingMap.get(current);

            DashboardDto dashboardDto = new DashboardDto();
            dashboardDto.setChantingDate(current);
            dashboardDto.setChantingRounds(
                    chanting != null ? chanting.getChantingRounds() : 0
            );

            result.add(dashboardDto);
            current = current.plusDays(1);
        }

        return result;
    }



    private BigDecimal calculateAverage(List<DashboardDto> records) {
        if (CollectionUtils.isEmpty(records)) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal sum = records.stream()
                .map(r -> BigDecimal.valueOf(r.getChantingRounds()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return sum.divide(
                BigDecimal.valueOf(records.size()),
                2,
                RoundingMode.HALF_UP
        );
    }
    private Integer calculateStreak(
            List<DashboardDto> records,
            Integer committedRounds
    ) {
        if (CollectionUtils.isEmpty(records)) {
            return 0;
        }

        int streak = 0;
        LocalDate expectedDate = LocalDate.now();

        // Walk backwards through the time series
        for (int i = records.size() - 1; i >= 0; i--) {
            DashboardDto dto = records.get(i);

            // This condition is safe guard if data is not continuous
            if (streak == 0 &&
                    !dto.getChantingDate().equals(expectedDate) &&
                    !dto.getChantingDate().equals(expectedDate.minusDays(1))) {
                continue;
            }

            if (!dto.getChantingDate().equals(expectedDate)
                    && !dto.getChantingDate().equals(expectedDate.minusDays(1))) {
                break;
            }

            if (dto.getChantingRounds() < committedRounds) {
                break;
            }

            streak++;
            expectedDate = dto.getChantingDate().minusDays(1);
        }

        return streak;
    }


}

