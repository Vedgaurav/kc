package com.one.kc.auth.utils;

import com.one.kc.user.entity.User;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Component
public class JwtUtil {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    private static final long ACCESS_TOKEN_MINUTES = 15;
    private static final long REFRESH_TOKEN_DAYS = 7;

    public JwtUtil(JwtEncoder jwtEncoder, JwtDecoder jwtDecoder) {
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
    }

    public static Long getRefreshTokenDays() {
        return REFRESH_TOKEN_DAYS;
    }

    public static Long getAccessTokenMinutes() {
        return ACCESS_TOKEN_MINUTES;
    }

    /* ================= ACCESS TOKEN ================= */

    public String generateAccessToken(User user, String activeKeyId) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("one-kc")
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.MINUTES))
                .subject(user.getUserId().toString())
                .claim("email", user.getEmail())
                .claim("firstName", user.getFirstName())
                .claim("lastName", user.getLastName())
                .claim("type", "access")
                .claim("aud", "one-kc-web")
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(SignatureAlgorithm.RS256)
                                .keyId(activeKeyId)
                                .build(),
                        claims
                )
        ).getTokenValue();
    }

    /* ================= REFRESH TOKEN ================= */

    public String generateRefreshToken(User user, String activeKeyId) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("one-kc")
                .issuedAt(now)
                .expiresAt(now.plus(REFRESH_TOKEN_DAYS, ChronoUnit.DAYS))
                .subject(user.getUserId().toString())
                .claim("type", "refresh")
                .claim("aud", "one-kc-web")
                .build();

        return jwtEncoder.encode(
                JwtEncoderParameters.from(
                        JwsHeader.with(() -> "RS256")
                                .keyId(activeKeyId)
                                .build(),
                        claims
                )
        ).getTokenValue();
    }

    /* ================= VALIDATION ================= */

    public Jwt validateToken(String token) {
        return jwtDecoder.decode(token);
    }

    public boolean invalidRefreshToken(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        return !"refresh".equals(jwt.getClaim("type"));
    }

    public boolean isAccessToken(String token) {
        Jwt jwt = jwtDecoder.decode(token);
        return "access".equals(jwt.getClaim("type"));
    }

    public Long extractUserId(String token) {
        return Long.parseLong(jwtDecoder.decode(token).getSubject());
    }

    public Map<String, Object> extractClaims(String token) {
        return jwtDecoder.decode(token).getClaims();
    }

    public static Long getUserId(Jwt jwt){
        return Long.parseLong(jwt.getSubject());
    }


}
