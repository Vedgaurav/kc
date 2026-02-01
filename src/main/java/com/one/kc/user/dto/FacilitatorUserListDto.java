package com.one.kc.user.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FacilitatorUserListDto {
    private String userId;
    private String name;
    private String email;
    private String phone;
}
