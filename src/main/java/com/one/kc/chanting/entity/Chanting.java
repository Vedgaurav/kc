package com.one.kc.chanting.entity;

import com.one.kc.common.enums.UserStatus;
import com.one.kc.common.utils.AuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "CHANTING")
public class Chanting extends AuditEntity {

    @Id
    @GeneratedValue(generator = "UUID")
    private UUID id;

}

