package com.healthcare.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "healthcare.security")
public record AuthProperties(
        String jwtSecret,
        String fieldEncryptionKey,
        String issuer,
        long accessTokenMinutes,
        long refreshTokenDays) {
}
