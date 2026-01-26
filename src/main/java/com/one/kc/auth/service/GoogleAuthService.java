package com.one.kc.auth.service;

import com.one.kc.auth.config.AuthConfigProperties;
import com.one.kc.auth.dto.GoogleUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;


@Service
public class GoogleAuthService {

    private final JwtDecoder googleJwtDecoder;
    private final AuthConfigProperties props;

    public GoogleAuthService(JwtDecoder jwtDecoder,
                             AuthConfigProperties props) {
        this.googleJwtDecoder = NimbusJwtDecoder
                .withJwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .build();
        this.props = props;
    }

    public GoogleUser verify(String idTokenString) {

        Jwt jwt = googleJwtDecoder.decode(idTokenString);

        // audience validation
        if (!jwt.getAudience().contains(
                props.getGoogle().getOauthAudience())) {
            throw new RuntimeException("Invalid Google audience");
        }

        Boolean emailVerified = jwt.getClaim("email_verified");
        if (!Boolean.TRUE.equals(emailVerified)) {
            throw new RuntimeException("Email not verified");
        }

        return new GoogleUser(
                jwt.getClaim("email"),
                jwt.getClaim("given_name"),
                jwt.getClaim("family_name"),
                jwt.getSubject()
        );
    }
}


