package com.one.kc.chanting.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChantingDto {
    private String chantingId;
    private Integer chantingRounds;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Instant chantingAt;
}
