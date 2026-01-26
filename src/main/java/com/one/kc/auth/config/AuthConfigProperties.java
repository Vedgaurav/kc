package com.one.kc.auth.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.annotation.Validated;

import java.util.ArrayList;
import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "auth")
@Data
@Validated
public class AuthConfigProperties {

    @Valid
    private Token token = new Token();
    @Valid
    private Google google = new Google();
    @Valid
    private Jwt jwt = new Jwt();
    @Valid
    private Cors cors = new Cors();

    @Data
    public static class Token {
        @NotNull
        private boolean secure;
        @NotNull
        private String sameSite;
    }

    @Data
    public static class Cors {
        @NotEmpty
        private List<String> allowedOrigins = new ArrayList<>();
    }

    @Data
    public static class Google {
        @NotEmpty
        private String oauthAudience;
    }

    @Data
    public static class Jwt {
        @NotEmpty
        private String activeKeyId;
        @Valid
        @NotEmpty
        private List<Rsa> keys = new ArrayList<>();
    }

    @Data
    public static class Rsa {
        @NotEmpty
        private String keyId;
        @NotEmpty
        private String publicKeyPath;
        @NotEmpty
        private String privateKeyPath;
    }
}
