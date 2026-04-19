package com.todayfridge.backend1;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class Backend1Application {
    public static void main(String[] args) {
        SpringApplication.run(Backend1Application.class, args);
    }
}
