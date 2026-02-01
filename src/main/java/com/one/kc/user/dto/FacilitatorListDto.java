package com.one.kc.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class FacilitatorListDto {
    private String id;
    private String name;
    private String email;
}

