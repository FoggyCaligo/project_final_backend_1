package com.today.fridge.global.config;

import com.today.fridge.auth.security.JwtAuthenticationFilter;
import com.today.fridge.auth.security.JwtProvider;
import com.today.fridge.auth.service.RedisTokenService;
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
    // MDC는 로그에 찍을 정보를 담아두는 보관함이다 서버에서 요청이 들어올때마다 아이디를 스레드에 넣는다. 이후 로깅을 남길때마다 아이디를 확인한다. 이렇게 하면 로그에 일일이 번호표를 적지 않아도 바로 알 수 있다 , MDC는 스레드 로컬 방식을 사용한다. 각 요청(스레드)마다 자신만의 보관함을 가지기 때문에 수천명의 사용자가 동시에 접근해도 로그가 서로 뒤섞이지 않는다 
    private final MDCLoggingFilter MDCLoggingFilter;
    private final JwtProvider jwtProvider;
    private final UserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final RedisTokenService redisTokenService;

    public SecurityConfig(MDCLoggingFilter MDCLoggingFilter,
                          JwtProvider jwtProvider,
                          UserDetailsService userDetailsService,
                          UserRepository userRepository,
                          RedisTokenService redisTokenService) {
        this.MDCLoggingFilter = MDCLoggingFilter;
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
        this.userRepository = userRepository;
        this.redisTokenService = redisTokenService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JwtAuthenticationFilter JwtAuthenticationFilter() {
        return new JwtAuthenticationFilter(jwtProvider, userDetailsService, redisTokenService);
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
                        // Redis 기반 auth 경로
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
                        .requestMatchers("/api/v1/meal", "/api/v1/meal/**").authenticated()
                        .requestMatchers("/api/v1/files/**").permitAll()
                        .requestMatchers("/api/fastapi-test").permitAll()
                        // v3 쇼핑 검색 (키워드 검색은 인증 없이 허용)
                        .requestMatchers("/api/v1/shopping/search").permitAll()
                        // 인증 필요한 API
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/auth/me").authenticated()
                        .requestMatchers("/api/v1/auth/logout").authenticated()
                        .requestMatchers("/api/v1/users/me/**").authenticated()
                        .requestMatchers("/api/v1/shopping/**").authenticated()
                        .anyRequest().permitAll())
                .addFilterBefore(JwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(MDCLoggingFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(new UserIdResolutionFilter(userRepository), JwtAuthenticationFilter.class);
        return http.build();
    }
}
