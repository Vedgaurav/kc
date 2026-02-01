package com.one.kc.common.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    USER(1),
    FACILITATOR(2),
    ADMIN(3),
    SUPER_ADMIN(4);

    private final int priority;

    UserRole(int priority) {
        this.priority = priority;
    }

    public boolean hasAtLeast(UserRole other) {
        return this.priority >= other.priority;
    }

}

