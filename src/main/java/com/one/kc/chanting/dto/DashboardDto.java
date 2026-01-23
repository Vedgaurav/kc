package com.one.kc.chanting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DashboardDto {
    private Integer chantingRounds;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate chantingDate;
}
