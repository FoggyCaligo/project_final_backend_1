package com.today.fridge.shopping.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

/**
 * application2.yml 의 naver/coupang API 키 설정을 Spring 환경으로 로드한다.
 * 실제 키는 환경변수 NAVER_CLIENT_ID, NAVER_CLIENT_SECRET, COUPANG_ACCESS_KEY, COUPANG_SECRET_KEY 로 주입.
 */
@Configuration
@PropertySource(
        value = "classpath:application2.yml",
        factory = YamlPropertySourceFactory.class,
        ignoreResourceNotFound = true
)
public class ShoppingApiConfig {
}
