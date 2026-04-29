package com.today.fridge.global.filter;

import com.today.fridge.user.entity.User;
import com.today.fridge.user.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Optional;

/**
 * JWT로 인증된 사용자의 loginId를 userId(PK)로 변환하여 X-User-Id 헤더에 주입합니다.
 * JwtAuthenticationFilter 이후에 실행되어야 합니다.
 */
public class UserIdResolutionFilter extends OncePerRequestFilter {

    private final UserRepository userRepository;

    public UserIdResolutionFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        if (auth != null && auth.isAuthenticated()
                && !"anonymousUser".equals(auth.getPrincipal())) {

            String loginId = auth.getName();
            Optional<User> userOpt = userRepository.findByLoginId(loginId);

            if (userOpt.isPresent()) {
                Long userId = userOpt.get().getUserId();
                chain.doFilter(injectUserId(request, userId), response);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private HttpServletRequestWrapper injectUserId(HttpServletRequest request, Long userId) {
        String userIdStr = String.valueOf(userId);
        return new HttpServletRequestWrapper(request) {
            @Override
            public String getHeader(String name) {
                if ("X-User-Id".equalsIgnoreCase(name)) return userIdStr;
                return super.getHeader(name);
            }

            @Override
            public Enumeration<String> getHeaders(String name) {
                if ("X-User-Id".equalsIgnoreCase(name)) {
                    return Collections.enumeration(List.of(userIdStr));
                }
                return super.getHeaders(name);
            }
        };
    }
}
