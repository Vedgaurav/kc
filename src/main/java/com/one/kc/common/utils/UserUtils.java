package com.one.kc.common.utils;

import com.one.kc.user.entity.User;

import java.util.List;

public class UserUtils {
    public static  List<String> extractRoles(User user) {
        return user.getRoles().stream()
                .map(r -> r.getRole().name())
                .toList();
    }
}
