package com.today.fridge.auth.security;

import com.today.fridge.auth.service.RedisTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT 인증 필터 (Redis 블랙리스트 대응 버전).
 *
 * 기존 JwtAuthenticationFilter와 동일한 기능을 수행하되,
 * 로그아웃된 Access Token을 Redis 블랙리스트에서 확인하여 차단합니다.
 *
 * Redis 장애 시에는 블랙리스트 체크를 건너뛰어 기존과 동일하게 동작합니다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter2.class);

    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final RedisTokenService redisTokenService;

    public JwtAuthenticationFilter2(JwtProvider jwtProvider,
                                    UserDetailsService userDetailsService,
                                    RedisTokenService redisTokenService) {
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
        this.redisTokenService = redisTokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String accessToken = jwtProvider.resolveTokenFromCookie(request, "accessToken");

        if (accessToken != null && jwtProvider.validateToken(accessToken)) {
            // Redis 블랙리스트 확인 (graceful degradation)
            try {
                if (redisTokenService.isBlacklisted(accessToken)) {
                    log.debug("[JwtFilter2] 블랙리스트된 Access Token 감지 — 인증 거부");
                    filterChain.doFilter(request, response);
                    return;
                }
            } catch (Exception e) {
                // Redis 장애 시 블랙리스트 체크 생략 (기존 동작 유지)
                log.warn("[JwtFilter2] Redis 블랙리스트 확인 실패, 건너뜀: {}", e.getMessage());
            }

            String loginId = jwtProvider.getLoginIdFromToken(accessToken);
            UserDetails userDetails = userDetailsService.loadUserByUsername(loginId);
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        filterChain.doFilter(request, response);
    }
}
