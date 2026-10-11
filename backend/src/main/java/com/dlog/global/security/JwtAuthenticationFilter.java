package com.dlog.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final AdminProperties adminProperties;

    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider, AdminProperties adminProperties) {
        this.tokenProvider = tokenProvider;
        this.adminProperties = adminProperties;
        if (adminProperties.autoLogin()) {
            log.warn("dlog.admin.auto-login=true: 모든 요청을 관리자로 인증합니다. 로컬 개발 전용입니다.");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            tokenProvider.parseUsername(header.substring(BEARER_PREFIX.length()))
                    // 관리자 아이디가 바뀐 경우 이전에 발급된 토큰은 무효 처리
                    .filter(adminProperties.username()::equals)
                    .ifPresent(this::authenticateAsAdmin);
        }

        if (adminProperties.autoLogin() && SecurityContextHolder.getContext().getAuthentication() == null) {
            authenticateAsAdmin(adminProperties.username());
        }
        chain.doFilter(request, response);
    }

    private void authenticateAsAdmin(String username) {
        var auth = new UsernamePasswordAuthenticationToken(
                username, null, AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
