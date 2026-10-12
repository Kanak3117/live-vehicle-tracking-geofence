package com.kanak.tracking.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableCaching
public class AppConfig {
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}