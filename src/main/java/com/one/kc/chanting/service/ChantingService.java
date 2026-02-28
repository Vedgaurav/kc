package com.one.kc.chanting.service;

import com.one.kc.chanting.dto.ChantingDashboardResponseDto;
import com.one.kc.chanting.dto.ChantingDto;
import com.one.kc.chanting.dto.DashboardDto;
import com.one.kc.chanting.dto.FacilitatorTodayDto;
import com.one.kc.chanting.dto.PageResponse;
import com.one.kc.chanting.entity.Chanting;
import com.one.kc.chanting.mapper.ChantingMapper;
import com.one.kc.chanting.repository.ChantingRepository;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
     * - Generates a unique chanting ID
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

        if (chantingDto.getChantingAt().isAfter(Instant.now())) {
            throw new UserFacingException("Future time not allowed");
        }
        Long userId = Long.parseLong(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserFacingException("User not found"));

        Chanting chanting = new Chanting();
        chanting.setUser(user);
        chanting.setChantingRounds(chantingDto.getChantingRounds());
        chanting.setChantingAt(chantingDto.getChantingAt());

        Chanting saved = chantingRepository.save(chanting);

        LoggerUtils.info(logger,
                "Chanting record created at {}", chantingDto.getChantingAt());

        return ResponseEntityUtils.getCreatedResponse(
                chantingMapper.toDto(saved)
        );
    }

    @Transactional
    public ResponseEntity<ChantingDto> updateChanting(
            ChantingDto chantingDto,
            Jwt jwt
    ) {

        if (chantingDto.getChantingAt().isAfter(Instant.now())) {
            throw new UserFacingException("Future time not allowed");
        }
        Long loggedInUserId = Long.parseLong(jwt.getSubject());
        Long chantingId = Long.parseLong(chantingDto.getChantingId());
        Chanting existing = chantingRepository.findById(chantingId)
                .orElseThrow(() -> new UserFacingException("Chanting not found"));

        // 🔒 SECURITY CHECK
        if (!existing.getUser().getUserId().equals(loggedInUserId)) {
            throw new UserFacingException("You are not allowed to update this record");
        }

        Instant cutoff = Instant.now().minus(5, ChronoUnit.DAYS);

        if (existing.getChantingAt().isBefore(cutoff)) {
            throw new UserFacingException("Update not allowed for old record");
        }

        existing.setChantingRounds(chantingDto.getChantingRounds());
        existing.setChantingAt(chantingDto.getChantingAt());

        Chanting updated = chantingRepository.save(existing);

        return ResponseEntity.ok(chantingMapper.toDto(updated));
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

        Page<Chanting> chantingListPage = chantingRepository.findByUser_UserId(userId, pageable);

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

        Instant date = chanting.getChantingAt();

        Instant cutoff = Instant.now().minus(5, ChronoUnit.DAYS);

        if (date.isBefore(cutoff)) {
            throw new UserFacingException(
                    "Update not allowed for date " + date
            );
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
                .orElseThrow(() -> new UserFacingException("User not found"));

        ZoneId zone = ZoneId.systemDefault(); // Ideally user-specific timezone

        // Resolve end date
        LocalDate to = Objects.requireNonNullElseGet(toDate, LocalDate::now);

        // Resolve start date
        LocalDate from = fromDate;

        if (from == null) {
            Optional<Instant> minInstantOpt =
                    chantingRepository.findMinChantingAtByUserId(userId);

            from = minInstantOpt
                    .map(instant -> instant.atZone(zone).toLocalDate())
                    .orElse(to);
        }

        // Safety guard
        if (from.isAfter(to)) {
            from = to;
        }

        // Convert LocalDate range → Instant range
        Instant fromInstant = from.atStartOfDay(zone).toInstant();
        Instant toInstant = to.plusDays(1).atStartOfDay(zone).toInstant();

        // Fetch chanting records
        List<Chanting> chantingList =
                chantingRepository
                        .findByUser_UserIdAndChantingAtBetweenOrderByChantingAtAsc(
                                userId,
                                fromInstant,
                                toInstant
                        );

        int totalRounds = chantingList.stream()
                .mapToInt(Chanting::getChantingRounds)
                .sum();

        // Build full time series
        List<DashboardDto> chantingRecords =
                buildTimeSeries(chantingList, from, to, zone);

        return ChantingDashboardResponseDto.builder()
                .committedRounds(user.getCommittedRounds())
                .idealRounds(IDEAL_ROUNDS)
                .currentStreak(
                        calculateStreak(chantingRecords, user.getCommittedRounds(), zone)
                )
                .averageRounds(calculateAverage(chantingRecords))
                .chantingDtoList(chantingRecords)
                .totalRounds(totalRounds)
                .totalMahamantras(totalRounds * BEADS_IN_ONE_ROUND)
                .build();
    }

    private List<DashboardDto> buildTimeSeries(
            List<Chanting> chantingList,
            LocalDate from,
            LocalDate to,
            ZoneId zone
    ) {

        Map<LocalDate, Integer> dailyTotals =
                chantingList.stream()
                        .collect(Collectors.groupingBy(
                                chanting -> chanting.getChantingAt()
                                        .atZone(zone)
                                        .toLocalDate(),
                                Collectors.summingInt(Chanting::getChantingRounds)
                        ));

        List<DashboardDto> result = new ArrayList<>();

        LocalDate current = from;

        while (!current.isAfter(to)) {

            DashboardDto dto = new DashboardDto();
            dto.setChantingDate(current);
            dto.setChantingRounds(
                    dailyTotals.getOrDefault(current, 0)
            );

            result.add(dto);
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
            Integer committedRounds,
            ZoneId zone
    ) {

        if (CollectionUtils.isEmpty(records)) {
            return 0;
        }

        int streak = 0;
        LocalDate expectedDate = LocalDate.now(zone);

        for (int i = records.size() - 1; i >= 0; i--) {

            DashboardDto dto = records.get(i);

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

    public ResponseEntity<Page<FacilitatorTodayDto>>getFacilitatorGroupChantingToday(Jwt jwt, Pageable pageable) {

        Long userId = Long.parseLong(jwt.getSubject());

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserFacingException("User not found"));

        if (user.getFacilitator() == null) {
            throw new UserFacingException("User has no facilitator");
        }

        Long facilitatorId = user.getFacilitator().getUserId();

        ZoneId zone = ZoneId.systemDefault();
        LocalDate today = LocalDate.now(zone);

        Instant start = today.atStartOfDay(zone).toInstant();
        Instant end = today.plusDays(1).atStartOfDay(zone).toInstant();

        Page<FacilitatorTodayDto> result =
                chantingRepository.findFacilitatorUsersTodayChanting(
                        facilitatorId,
                        start,
                        end,
                        pageable
                );

        return ResponseEntity.ok(result);
    }
}

