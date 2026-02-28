package com.one.kc.chanting.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FacilitatorTodayDto {

    private String firstName;
    private String lastName;
    private Long totalRounds;
}