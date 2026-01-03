package com.one.kc.auth.service;

import com.one.kc.common.exceptions.UserFacingException;
import org.apache.commons.codec.binary.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.Duration;
import java.util.Set;

@Service
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private static final String REFRESH_PREFIX = "refresh:";
    private static final String USER_TOKENS_PREFIX = "user:";
    public static final String TOKENS = ":tokens";

    private final RedisTemplate<String, String> redisTemplate;

    public RefreshTokenServiceImpl(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(Long userId, String hashedRefreshToken, Duration TTL) {
        String userTokensKey = getUserTokenKey(userId);

        if (redisTemplate.opsForSet().size(userTokensKey) >= 5) {
            throw new UserFacingException("Login max devices reached: 5");
        }

        redisTemplate.opsForValue()
                .set(refreshTokenKey(hashedRefreshToken), userId.toString(), TTL);

        // add token hash to user's set
        redisTemplate.opsForSet().add(
                getUserTokenKey(userId),
                hashedRefreshToken
        );
    }

    public boolean exists(String hashedRefreshToken) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(refreshTokenKey(hashedRefreshToken)));
    }

    public void delete(String hashedRefreshToken, Long userId) {
        // remove token from user's active token set
        redisTemplate.opsForSet().remove(
                getUserTokenKey(userId),
                hashedRefreshToken
        );
        redisTemplate.delete(refreshTokenKey(hashedRefreshToken));
    }

    @Override
    public void deleteAllForUser(Long userId) {
        String userTokensKey = getUserTokenKey(userId);

        Set<String> tokens = redisTemplate.opsForSet().members(userTokensKey);

        if (!CollectionUtils.isEmpty(tokens)) {
            tokens.forEach(token ->
                    redisTemplate.delete(REFRESH_PREFIX + token)
            );
        }

        redisTemplate.delete(userTokensKey);

    }

    private String refreshTokenKey(String hashedKey) {
        return REFRESH_PREFIX + hashedKey;
    }

    private String getUserTokenKey(Long userId) {
        return USER_TOKENS_PREFIX + userId + TOKENS;
    }
}

