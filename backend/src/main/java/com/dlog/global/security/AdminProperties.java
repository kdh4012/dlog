package com.dlog.global.security;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * 1인 블로그이므로 관리자 계정은 DB 가 아닌 환경 변수로 관리한다.
 *
 * @param username     관리자 아이디
 * @param passwordHash BCrypt 해시된 비밀번호
 */
@Validated
@ConfigurationProperties("dlog.admin")
public record AdminProperties(
        @NotBlank String username,
        @NotBlank String passwordHash
) {
}
