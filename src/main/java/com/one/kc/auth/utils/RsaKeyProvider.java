package com.one.kc.auth.utils;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.one.kc.auth.config.AuthConfigProperties;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Getter
public class RsaKeyProvider {

    private final Map<String, RSAKey> rsaKeys;
    private final String activeKeyId;

    public RsaKeyProvider(AuthConfigProperties authConfig) {

        this.activeKeyId = authConfig.getJwt().getActiveKeyId();

        this.rsaKeys = authConfig.getJwt().getKeys().stream()
                .collect(Collectors.toUnmodifiableMap(
                        AuthConfigProperties.Rsa::getKeyId,
                        this::loadRsaKey
                ));

        if (!rsaKeys.containsKey(activeKeyId)) {
            throw new IllegalStateException("Active RSA key not found: " + activeKeyId);
        }
    }

    private RSAKey loadRsaKey(AuthConfigProperties.Rsa rsa) {

        RSAPublicKey publicKey =
                PemUtils.loadPublicKey(Path.of(rsa.getPublicKeyPath()));

        RSAPrivateKey privateKey =
                PemUtils.loadPrivateKey(Path.of(rsa.getPrivateKeyPath()));

        return new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(rsa.getKeyId())
                .algorithm(JWSAlgorithm.RS256)
                .keyUse(KeyUse.SIGNATURE)
                .build();
    }

    /** Used for signing new tokens */
    public RSAKey activeKey() {
        return rsaKeys.get(activeKeyId);
    }

    /** Used for validation */
    public Collection<RSAKey> allKeys() {
        return rsaKeys.values();
    }
}


