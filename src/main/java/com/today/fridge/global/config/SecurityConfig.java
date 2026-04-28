package com.today.fridge.global.config;

import com.today.fridge.auth.security.JwtAuthenticationFilter;
import com.today.fridge.auth.security.JwtProvider;
import com.today.fridge.global.filter.MDCLoggingFilter;
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

    public SecurityConfig(MDCLoggingFilter MDCLoggingFilter,
                          JwtProvider jwtProvider,
                          UserDetailsService userDetailsService) {
        this.MDCLoggingFilter = MDCLoggingFilter;
        this.jwtProvider = jwtProvider;
        this.userDetailsService = userDetailsService;
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
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .anyRequest().permitAll())
                .addFilterBefore(jwtAuthenticationFilter(), UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(MDCLoggingFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
