package com.one.kc.auth.service;

import com.one.kc.auth.dto.AuthResponse;
import com.one.kc.auth.dto.GoogleLoginRequest;
import com.one.kc.auth.dto.GoogleUser;
import com.one.kc.auth.utils.JwtUtil;
import com.one.kc.common.exceptions.ResourceNotFoundException;
import com.one.kc.user.entity.User;
import com.one.kc.user.mapper.UserMapper;
import com.one.kc.user.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final GoogleAuthService googleAuthService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final UserMapper userMapper;

    public AuthService(GoogleAuthService googleAuthService,
                       UserService userService,
                       JwtUtil jwtUtil, UserMapper userMapper) {
        this.googleAuthService = googleAuthService;
        this.userService = userService;
        this.jwtUtil = jwtUtil;
        this.userMapper = userMapper;
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

        //TODO Store refresh token in DB / Redis
        //refreshTokenService.save(user.getUserId(), refreshToken);


        ResponseCookie accessCookie = ResponseCookie.from("access_token", accessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(15 * 60)
                .build();

        ResponseCookie refreshCookie = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
//                .sameSite("Strict")
                .path("/auth/refresh")
                .maxAge(7 * 24 * 60 * 60)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());
        response.addHeader(HttpHeaders.SET_COOKIE, refreshCookie.toString());

        return ResponseEntity.ok(
                AuthResponse.builder().userDto(userMapper.toDto(user)).build()
        );
    }

    public ResponseEntity<Void> refreshToken(
            String refreshToken,
            HttpServletResponse response) {

        if (!jwtUtil.isRefreshToken(refreshToken)) {
            throw new RuntimeException("Invalid refresh token");
        }

        Long userId = jwtUtil.extractUserId(refreshToken);
        User user = userService.getUserById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        String newAccessToken = jwtUtil.generateAccessToken(user);

        ResponseCookie accessCookie = ResponseCookie.from("access_token", newAccessToken)
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(15 * 60)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, accessCookie.toString());

        return ResponseEntity.ok().build();
    }

    public ResponseEntity<Void> logout(String refreshToken, HttpServletResponse response) {

        //        if (refreshToken != null) {
//            refreshTokenService.delete(refreshToken);
//        }
        deleteCookie(response, "access_token");
        deleteCookie(response, "refresh_token");

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
}
