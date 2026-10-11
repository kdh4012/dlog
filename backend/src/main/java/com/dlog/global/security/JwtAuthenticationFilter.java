package com.dlog.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider tokenProvider;
    private final AdminProperties adminProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (header != null && header.startsWith(BEARER_PREFIX)) {
            tokenProvider.parseUsername(header.substring(BEARER_PREFIX.length()))
                    // 관리자 아이디가 바뀐 경우 이전에 발급된 토큰은 무효 처리
                    .filter(adminProperties.username()::equals)
                    .ifPresent(username -> {
                        var auth = new UsernamePasswordAuthenticationToken(
                                username, null, AuthorityUtils.createAuthorityList("ROLE_ADMIN"));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    });
        }
        chain.doFilter(request, response);
    }
}
