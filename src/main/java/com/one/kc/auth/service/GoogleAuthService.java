package com.one.kc.auth.service;

import com.one.kc.auth.config.AuthConfigProperties;
import com.one.kc.auth.dto.GoogleUser;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

    private static final String GOOGLE_JWK_SET =
            "https://www.googleapis.com/oauth2/v3/certs";

    private static final String GOOGLE_ISSUER =
            "https://accounts.google.com";

    private final JwtDecoder googleJwtDecoder;

    public GoogleAuthService(AuthConfigProperties props) {

        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withJwkSetUri(GOOGLE_JWK_SET)
                .build();

        OAuth2TokenValidator<Jwt> issuerValidator =
                JwtValidators.createDefaultWithIssuer(GOOGLE_ISSUER);

        OAuth2TokenValidator<Jwt> audienceValidator =
                jwt -> jwt.getAudience().contains(
                        props.getGoogle().getOauthAudience()
                )
                        ? OAuth2TokenValidatorResult.success()
                        : OAuth2TokenValidatorResult.failure(
                        new OAuth2Error("invalid_token", "Invalid audience", null)
                );

        OAuth2TokenValidator<Jwt> validator =
                new DelegatingOAuth2TokenValidator<>(
                        issuerValidator,
                        audienceValidator
                );

        decoder.setJwtValidator(validator);
        this.googleJwtDecoder = decoder;
    }

    public GoogleUser verify(String idToken) {

        Jwt jwt = googleJwtDecoder.decode(idToken);

        if (!Boolean.TRUE.equals(jwt.getClaim("email_verified"))) {
            throw new RuntimeException("Google email not verified");
        }

        return new GoogleUser(
                jwt.getClaim("email"),
                jwt.getClaim("given_name"),
                jwt.getClaim("family_name"),
                jwt.getSubject()
        );
    }
}
