package com.one.kc.auth.service;

import com.one.kc.auth.config.AuthConfigProperties;
import com.one.kc.auth.dto.GoogleUser;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class GoogleAuthService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final AuthConfigProperties props;

    public GoogleAuthService(AuthConfigProperties props) {
        this.props = props;
    }

    public GoogleUser verify(String idToken) {

        String url =
                "https://oauth2.googleapis.com/tokeninfo?id_token=" + idToken;

        ResponseEntity<Map<String, Object>> response =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        null,
                        new ParameterizedTypeReference<>() {}
                );

        Map<String, Object> claims = response.getBody();

        if (claims == null) {
            throw new RuntimeException("Invalid Google ID token");
        }

        if (!props.getGoogle().getOauthAudience()
                .equals(claims.get("aud"))) {
            throw new RuntimeException("Invalid Google audience");
        }

        if (!Boolean.parseBoolean(
                String.valueOf(claims.get("email_verified")))) {
            throw new RuntimeException("Google email not verified");
        }

        return new GoogleUser(
                (String) claims.get("email"),
                (String) claims.get("given_name"),
                (String) claims.get("family_name"),
                (String) claims.get("sub")
        );
    }
}
