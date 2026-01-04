package com.one.kc.auth.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.one.kc.auth.config.AuthConfigProperties;
import com.one.kc.auth.dto.GoogleUser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
public class GoogleAuthService {

    private final AuthConfigProperties authConfigProperties;

    public GoogleAuthService(AuthConfigProperties authConfigProperties){
        this.authConfigProperties = authConfigProperties;
    }

    public GoogleUser verify(String idTokenString) {

        GoogleIdTokenVerifier verifier =
                new GoogleIdTokenVerifier.Builder(
                        new NetHttpTransport(),
                        GsonFactory.getDefaultInstance()
                )
                        .setAudience(List.of(authConfigProperties.getGoogle().getOauthAudience()))
                        .build();

        GoogleIdToken idToken = null;
        try {
            idToken = verifier.verify(idTokenString);
        } catch (GeneralSecurityException | IOException e) {
            throw new RuntimeException(e);
        }

        if (idToken == null) {
            throw new RuntimeException("Invalid Google ID Token");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();

        return new GoogleUser(
                payload.getEmail(),
                (String) payload.get("name"),
                payload.getSubject()
        );
    }
}


