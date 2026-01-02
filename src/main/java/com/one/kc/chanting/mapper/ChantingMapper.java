package com.one.kc.chanting.mapper;

import com.one.kc.chanting.dto.ChantingDto;
import com.one.kc.chanting.entity.Chanting;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

@Mapper(componentModel = "spring")
public interface ChantingMapper {
    Chanting toEntity(ChantingDto chantingDto);
    ChantingDto toDto(Chanting chanting);
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntityFromDto(ChantingDto dto, @MappingTarget Chanting entity);
}