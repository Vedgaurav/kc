package com.one.kc.chanting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ChantingDashboardResponseDto {
    private Integer committedRounds;
    private Integer idealRounds;
    private Integer currentStreak;
    private BigDecimal averageRounds;
    private List<DashboardDto> chantingDtoList;
    private Integer totalRounds;
    private Integer totalMahamantras;
}
