package com.luxuryhotel.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.admin")
public record AdminProperties(
        String fullName,
        String email,
        String phone,
        String password
) {
}
