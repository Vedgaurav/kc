package com.one.kc.chanting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChantingDto {
    private String chantingId;
    private Integer chantingRounds;
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate chantingDate;
}
