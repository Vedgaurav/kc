package com.one.kc.auth.service;

import com.one.kc.auth.config.AuthConfigProperties;
import com.one.kc.auth.dto.GoogleUser;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

    private final JwtDecoder googleJwtDecoder;
    private final AuthConfigProperties props;

    public GoogleAuthService(
            @Qualifier("googleJwtDecoder") JwtDecoder googleJwtDecoder,
            AuthConfigProperties props
    ) {
        this.googleJwtDecoder = googleJwtDecoder;
        this.props = props;
    }

    public GoogleUser verify(String idToken) {

        Jwt jwt = googleJwtDecoder.decode(idToken);

        // audience check
        if (!jwt.getAudience().contains(
                props.getGoogle().getOauthAudience())) {
            throw new RuntimeException("Invalid Google audience");
        }

        if (!Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
            throw new RuntimeException("Google email not verified");
        }

        return new GoogleUser(
                jwt.getClaimAsString("email"),
                jwt.getClaimAsString("given_name"),
                jwt.getClaimAsString("family_name"),
                jwt.getSubject()
        );
    }
}
