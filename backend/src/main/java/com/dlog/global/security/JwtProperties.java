package com.dlog.global.security;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * @param secret            HMAC-SHA256 서명 키 (32바이트 이상)
 * @param expirationMinutes 토큰 만료 시간(분)
 */
@Validated
@ConfigurationProperties("dlog.jwt")
public record JwtProperties(
        @NotBlank @Size(min = 32) String secret,
        @Min(1) long expirationMinutes
) {
}
