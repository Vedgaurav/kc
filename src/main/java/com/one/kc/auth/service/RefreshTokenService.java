package com.one.kc.auth.service;

import java.time.Duration;

public interface RefreshTokenService {

    void save(Long userId, String hashedToken, Duration ttl);

    boolean exists(String hashedToken);

    void delete(String hashedToken, Long userId);

    void deleteAllForUser(Long userId);
}

