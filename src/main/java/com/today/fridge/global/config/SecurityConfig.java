package com.today.fridge.global.config;

import com.today.fridge.global.filter.MDCLoggingFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    // MDC 필터를 사용하여 로그를 추적
    private final MDCLoggingFilter MDCLoggingFilter;

    // 생성자
    public SecurityConfig(MDCLoggingFilter MDCLoggingFilter) {
        this.MDCLoggingFilter = MDCLoggingFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().permitAll())
                // MDC 필터를 Security 필터 체인에 추가
                .addFilterBefore(MDCLoggingFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}