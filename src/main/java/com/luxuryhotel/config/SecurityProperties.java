package com.luxuryhotel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app.security.jwt")
public record SecurityProperties(
        String secret,
        long accessTokenExpirationMinutes,
        String issuer,
        List<String> allowedOrigins
) {
}
