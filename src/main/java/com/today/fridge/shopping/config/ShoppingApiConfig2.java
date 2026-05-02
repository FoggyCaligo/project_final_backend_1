package com.today.fridge.shopping.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource(
        value = "classpath:application3.yml",
        factory = YamlPropertySourceFactory.class,
        ignoreResourceNotFound = true
)
public class ShoppingApiConfig2 {
}
