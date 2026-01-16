package com.one.kc.chanting.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChantingDashboardResponseDto {
    private Integer committedRounds;
    private Integer idealRounds;
    private Integer currentStreak;
    private BigDecimal averageRounds;
    private List<ChantingDto> chantingDtoList;
}
