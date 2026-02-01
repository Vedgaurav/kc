package com.one.kc.user.mapper;

import com.one.kc.user.dto.UserDto;
import com.one.kc.user.entity.User;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserMapper {

    /* ================= DTO → ENTITY ================= */

    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "facilitator", ignore = true)
    @Mapping(target = "facilitatedUsers", ignore = true)
    User toEntity(UserDto request);


    /* ================= ENTITY → DTO ================= */

    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "countryCode", ignore = true)
    @Mapping(target = "facilitatorId", ignore = true)
    @Mapping(target = "facilitatorName", ignore = true)
    UserDto toDto(User user);


    /* ================= UPDATE ================= */

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "roles", ignore = true)
    @Mapping(target = "facilitator", ignore = true)
    @Mapping(target = "facilitatedUsers", ignore = true)
    void updateEntityFromDto(UserDto dto, @MappingTarget User entity);
}
