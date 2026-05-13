package com.today.fridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

import com.today.fridge.llm.config.LlmClientProperties;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(LlmClientProperties.class)
public class TodayFridgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(TodayFridgeApplication.class, args);
    }
}