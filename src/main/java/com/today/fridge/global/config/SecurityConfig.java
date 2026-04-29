package com.today.fridge.global.config;

import com.today.fridge.auth.security.JwtAuthenticationFilter;
import com.today.fridge.auth.security.JwtProvider;
import com.today.fridge.global.filter.MDCLoggingFilter;
import com.today.fridge.global.filter.UserIdResolutionFilter;
import com.today.fridge.user.repository.UserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final MDCLoggingFilter MDCLoggingFilter;
    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;

    public SecurityConfig(MDCLoggingFilter MDCLoggingFilter,
                          JwtProvider jwtProvider,
                          UserDetailsService userDetailsService,
                          UserRepository userRepository) {
        this.MDCLoggingFilter = MDCLoggingFilter;
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, userDetailsService);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 공개 API
                        .requestMatchers("/api/v1/auth/login").permitAll()
                        .requestMatchers("/api/v1/auth/signup").permitAll()
                        .requestMatchers("/api/v1/auth/check-login-id").permitAll()
                        .requestMatchers("/api/v1/auth/refresh").permitAll()
                        .requestMatchers("/api/v1/auth/verify-email").permitAll()
                        .requestMatchers("/api/v1/auth/resend-verification").permitAll()
                        .requestMatchers("/api/v1/auth/kakao/**").permitAll()
                        .requestMatchers("/api/v1/users/find-loginid").permitAll()
                        // 다른 팀원 API 도 일단 permitAll (추후 개별 설정)
                        .requestMatchers("/api/v1/recipes/**").permitAll()
                        .requestMatchers("/api/v1/ingredients/**").permitAll()
                        .requestMatchers("/api/v1/fridge/**").permitAll()
                        .requestMatchers("/api/v1/posts/**").permitAll()
                        .requestMatchers("/api/v1/chatbot/**").permitAll()
                        .requestMatchers("/api/v1/recommendation/**").permitAll()
                        .requestMatchers("/api/v1/dashboard/**").permitAll()
                        .requestMatchers("/api/v1/bookmarks/**").permitAll()
                        .requestMatchers("/api/v1/files/**").permitAll()
                        .requestMatchers("/api/fastapi-test").permitAll()
                        // 인증 필요한 API
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/users/me/**").authenticated()
                        .requestMatchers("/api/v1/shopping/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(MDCLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new UserIdResolutionFilter(userRepository), JwtAuthenticationFilter.class);
        return http.build();
    }
}
