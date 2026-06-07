package com.nila.chores.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * Provides the application-wide {@link Clock} bean.
 * {@link Clock#systemDefaultZone()} is used at runtime.
 * Tests can override this bean with a {@link Clock#fixed(java.time.Instant, java.time.ZoneId)} instance
 * or pass a Clock directly to service constructors.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
