package com.one.kc.common.utils;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuditEntity {
    @CreatedDate
    @NotNull
    @Column(updatable = false)
    private LocalDateTime addTime;
    @LastModifiedDate
    @NotNull
    private LocalDateTime chgTime;
    @CreatedBy
    @NotNull
    @Column(updatable = false)
    private Long addBy;
    @LastModifiedBy
    @NotNull
    private Long chgBy;
}
