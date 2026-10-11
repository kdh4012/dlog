package com.dlog.auth;

import com.dlog.auth.dto.LoginRequest;
import com.dlog.auth.dto.TokenResponse;
import com.dlog.global.error.BusinessException;
import com.dlog.global.error.ErrorCode;
import com.dlog.global.security.AdminProperties;
import com.dlog.global.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminProperties adminProperties;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    public TokenResponse login(LoginRequest request) {
        // 아이디 비교도 상수 시간으로 처리하고, 아이디가 틀려도 BCrypt 검증을 수행해 응답 시간 차이를 줄인다
        boolean usernameMatches = MessageDigest.isEqual(
                adminProperties.username().getBytes(StandardCharsets.UTF_8),
                request.username().getBytes(StandardCharsets.UTF_8));
        boolean passwordMatches = passwordEncoder.matches(request.password(), adminProperties.passwordHash());

        if (!usernameMatches || !passwordMatches) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        JwtTokenProvider.IssuedToken issued = tokenProvider.issue(adminProperties.username());
        return new TokenResponse(issued.token(), issued.expiresAt());
    }
}
