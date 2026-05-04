package com.today.fridge.global.config;

import com.today.fridge.auth.security.JwtAuthenticationFilter;
import com.today.fridge.global.filter.MDCLoggingFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JwtAuthenticationFilter와 MDCLoggingFilter는 Spring Security 필터 체인에 직접 등록되기 때문에
 * Spring Boot의 서블릿 필터 자동 등록을 비활성화합니다.
 * 두 필터가 동일 요청에 대해 이중으로 실행되는 것을 방지합니다.
 */
@Configuration
public class FilterRegistrationConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthenticationFilter> disableJwtFilterAutoRegistration(
            JwtAuthenticationFilter jwtAuthenticationFilter2) {
        FilterRegistrationBean<JwtAuthenticationFilter> registration =
                new FilterRegistrationBean<>(jwtAuthenticationFilter2);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<MDCLoggingFilter> disableMdcFilterAutoRegistration(
            MDCLoggingFilter mdcLoggingFilter) {
        FilterRegistrationBean<MDCLoggingFilter> registration =
                new FilterRegistrationBean<>(mdcLoggingFilter);
        registration.setEnabled(false);
        return registration;
    }
}
