package com.one.kc.auth.service;

import com.one.kc.auth.dto.AuthResponse;
import com.one.kc.auth.dto.GoogleLoginRequest;
import com.one.kc.auth.dto.GoogleUser;
import com.one.kc.auth.utils.JwtUtil;
import com.one.kc.common.exceptions.ResourceNotFoundException;
import com.one.kc.common.exceptions.UserFacingException;
import com.one.kc.user.entity.User;
import com.one.kc.user.mapper.UserMapper;
import com.one.kc.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.codec.digest.DigestUtils;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;

@Service
public class AuthService {

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String LAX = "Lax";
    private final GoogleAuthService googleAuthService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final RefreshTokenServiceImpl refreshTokenServiceImpl;

    public AuthService(GoogleAuthService googleAuthService,
                       UserService userService,
                       JwtUtil jwtUtil, UserMapper userMapper,
                       RefreshTokenServiceImpl refreshTokenServiceImpl) {
        this.googleAuthService = googleAuthService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.refreshTokenServiceImpl = refreshTokenServiceImpl;
    }

    public ResponseEntity<AuthResponse> googleLogin(GoogleLoginRequest request, HttpServletResponse response) {

        GoogleUser googleUser = googleAuthService.verify(request.getIdToken());

        Optional<User> userOptional = userService.getUserFromEmail(googleUser.getEmail());

        if (userOptional.isEmpty()) {
            return ResponseEntity.badRequest().body(AuthResponse.builder().errorMessage(googleUser.getEmail()).build());
        }

        User user = userOptional.get();

        String accessToken = jwtUtil.generateAccessToken(user);
        String refreshToken = jwtUtil.generateRefreshToken(user);

        String hashed = getHashed(refreshToken);
        refreshTokenServiceImpl.save(user.getUserId(), hashed, Duration.ofDays(JwtUtil.getRefreshTokenDays()));

        setCookiesWithTokens(response, accessToken, refreshToken);

        return ResponseEntity.ok(
                AuthResponse.builder().userDto(userMapper.toDto(user)).build()
        );
    }

    private void setCookiesWithTokens(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = getResponseCookie(ACCESS_TOKEN, accessToken, "/", JwtUtil.getAccessTokenMinutes() * 60);

        ResponseCookie refreshCookie = getResponseCookie(REFRESH_TOKEN, refreshToken, "/auth", JwtUtil.getRefreshTokenDays() * 24 * 60 * 60);

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

    }

    public static @NonNull String getHashed(String refreshToken) {
        return DigestUtils.sha256Hex(refreshToken);
    }

    public ResponseEntity<Void> refreshToken(
            String refreshToken,
            HttpServletResponse response) {

        if (jwtUtil.invalidRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        // 2. Hash incoming refresh token
        String hashedOld = getHashed(refreshToken);

        // 3. Validate against Redis
        if (!refreshTokenServiceImpl.exists(hashedOld)) {
            throw new RuntimeException("Refresh token revoked or expired");
        }

        Long userId = jwtUtil.extractUserId(refreshToken);
        User user = userService.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // 4. Rotate refresh token
        refreshTokenServiceImpl.delete(hashedOld, userId);

        String newRefreshToken = jwtUtil.generateRefreshToken(user);
        String hashedNew = getHashed(newRefreshToken);

        refreshTokenServiceImpl.save(
                user.getUserId(),
                hashedNew,
                Duration.ofDays(JwtUtil.getRefreshTokenDays())
        );


        String newAccessToken = jwtUtil.generateAccessToken(user);
        setCookiesWithTokens(response, newAccessToken, newRefreshToken);

        return ResponseEntity.ok().build();
    }

    private static @NonNull ResponseCookie getResponseCookie(String access_token, String newAccessToken, String path, long maxAgeSeconds) {
        return ResponseCookie.from(access_token, newAccessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite(LAX)
                .path(path)
                .maxAge(maxAgeSeconds)
                .build();
    }

    public ResponseEntity<Void> logout(String refreshToken, HttpServletResponse response) {

        if (refreshToken != null) {
            Long userId = jwtUtil.extractUserId(refreshToken);
            refreshTokenServiceImpl.delete(getHashed(refreshToken), userId);
        }
        deleteCookie(response, ACCESS_TOKEN);
        deleteCookie(response, REFRESH_TOKEN);

        return ResponseEntity.ok().build();
    }

    private void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // true in prod (HTTPS)
        cookie.setPath("/");
        cookie.setMaxAge(0); // DELETE
        response.addCookie(cookie);
    }

    public ResponseEntity<Void> logoutAll(String refreshToken, HttpServletResponse response) {
        if (jwtUtil.invalidRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        Long userId = jwtUtil.extractUserId(refreshToken);

        if (refreshToken != null) {
            refreshTokenServiceImpl.deleteAllForUser(userId);
        }

        deleteCookie(response, ACCESS_TOKEN);
        deleteCookie(response, REFRESH_TOKEN);

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<AuthResponse> getAuth(String refreshToken) {
        if (refreshToken != null) {
            Long userId = jwtUtil.extractUserId(refreshToken);
            Optional<User> userOptional = userService.findByUserId(userId);
            if (userOptional.isPresent()) {
                return ResponseEntity.ok(AuthResponse.builder().userDto(userMapper.toDto(userOptional.get())).build());
            }
            throw new UserFacingException("Unauthorized User");
        }
        throw new UserFacingException("Unauthorized User");
    }
}
