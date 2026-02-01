package com.one.kc.auth.service;

import com.one.kc.auth.config.AuthConfigProperties;
import com.one.kc.auth.dto.AuthResponse;
import com.one.kc.auth.dto.GoogleLoginRequest;
import com.one.kc.auth.dto.GoogleUser;
import com.one.kc.auth.utils.JwtUtil;
import com.one.kc.auth.utils.RsaKeyProvider;
import com.one.kc.common.constants.ErrorCodeConstants;
import com.one.kc.common.enums.UserStatus;
import com.one.kc.common.exceptions.ResourceNotFoundException;
import com.one.kc.common.exceptions.UserFacingException;
import com.one.kc.common.utils.LoggerUtils;
import com.one.kc.user.dto.UserDto;
import com.one.kc.user.entity.User;
import com.one.kc.user.mapper.UserMapper;
import com.one.kc.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    public static final String ACCESS_TOKEN = "access_token";
    public static final String REFRESH_TOKEN = "refresh_token";
    public static final String COOKIE_PATH = "/";
    private final GoogleAuthService googleAuthService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;
    private final RefreshTokenServiceImpl refreshTokenServiceImpl;
    private final AuthConfigProperties authConfigProperties;
    private final RsaKeyProvider rsaKeyProvider;

    public AuthService(GoogleAuthService googleAuthService,
                       UserService userService,
                       JwtUtil jwtUtil, UserMapper userMapper,
                       RefreshTokenServiceImpl refreshTokenServiceImpl,
                       AuthConfigProperties authConfigProperties,
                       RsaKeyProvider rsaKeyProvider) {
        this.googleAuthService = googleAuthService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
        this.refreshTokenServiceImpl = refreshTokenServiceImpl;
        this.authConfigProperties = authConfigProperties;
        this.rsaKeyProvider = rsaKeyProvider;
    }

    public ResponseEntity<AuthResponse> googleLogin(GoogleLoginRequest request, HttpServletResponse response) {

        try {
            GoogleUser googleUser = googleAuthService.verify(request.getIdToken());

            Optional<User> userOpt =
                    userService.findByGoogleSub(googleUser.getEmail());

            User user;

            user = userOpt.orElseGet(() -> userService.createUser(
                    UserDto.builder()
                            .email(googleUser.getEmail())
                            .firstName(googleUser.getFirstName())
                            .lastName(googleUser.getLastName())
                            .status(UserStatus.INACTIVE)
                            .build()
            ));

            String rsaActiveKeyId = rsaKeyProvider.getActiveKeyId();
            String accessToken = jwtUtil.generateAccessToken(user, rsaActiveKeyId);
            String refreshToken = jwtUtil.generateRefreshToken(user, rsaActiveKeyId);

            String hashed = getHashed(refreshToken);
            refreshTokenServiceImpl.save(user.getUserId(), hashed, Duration.ofDays(JwtUtil.getRefreshTokenDays()));

            setCookiesWithTokens(response, accessToken, refreshToken);
            return ResponseEntity.ok(
                    AuthResponse.builder().userDto(userMapper.toDto(user)).build());
        }catch (Exception e) {
            LoggerUtils.error(logger, "Login failed {}", e);
            throw new UserFacingException("Login failed", ErrorCodeConstants.GOOGLE_LOGIN_ERROR);
        }
    }

    private void setCookiesWithTokens(HttpServletResponse response, String accessToken, String refreshToken) {
        ResponseCookie accessCookie = getResponseCookie(ACCESS_TOKEN, accessToken, JwtUtil.getAccessTokenMinutes() * 60);

        ResponseCookie refreshCookie = getResponseCookie(REFRESH_TOKEN, refreshToken, JwtUtil.getRefreshTokenDays() * 24 * 60 * 60);

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

    }

    public static @NonNull String getHashed(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));

            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }

    public ResponseEntity<Void> refreshToken(
            String refreshToken,
            HttpServletResponse response) {

       try{
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

           String activeKeyId = rsaKeyProvider.getActiveKeyId();
           String newRefreshToken = jwtUtil.generateRefreshToken(user, activeKeyId);
           String hashedNew = getHashed(newRefreshToken);

           refreshTokenServiceImpl.save(
                   user.getUserId(),
                   hashedNew,
                   Duration.ofDays(JwtUtil.getRefreshTokenDays())
           );


           String newAccessToken = jwtUtil.generateAccessToken(user, activeKeyId);
           setCookiesWithTokens(response, newAccessToken, newRefreshToken);

           return ResponseEntity.ok().build();
       }catch (Exception e) {
           LoggerUtils.error(logger, "Refresh token failed {}", e);
           throw new UserFacingException("Login failed");
       }
    }

    private @NonNull ResponseCookie getResponseCookie(String access_token, String newAccessToken,
                                                      long maxAgeSeconds) {
        return ResponseCookie.from(access_token, newAccessToken)
                .httpOnly(true)
                .secure(authConfigProperties.getToken().isSecure())
                .sameSite(authConfigProperties.getToken().getSameSite())
                .path(AuthService.COOKIE_PATH)
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
        cookie.setSecure(authConfigProperties.getToken().isSecure()); // true in prod (HTTPS)
        cookie.setPath(COOKIE_PATH);
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
}
