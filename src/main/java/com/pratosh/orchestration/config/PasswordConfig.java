package com.pratosh.orchestration.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Provides password hashing functionality to the application.
 */
@Configuration
public class PasswordConfig {

    /**
     * Registers BCryptPasswordEncoder as a Spring-managed bean.
     *
     * The same encoder must be used when creating passwords
     * and when verifying passwords during login.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}