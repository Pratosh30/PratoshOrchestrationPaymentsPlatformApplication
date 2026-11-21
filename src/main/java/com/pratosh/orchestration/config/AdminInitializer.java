package com.pratosh.orchestration.config;

import com.pratosh.orchestration.entity.Role;
import com.pratosh.orchestration.entity.User;
import com.pratosh.orchestration.repository.UserRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Initializes the first administrator account when the application starts.
 *
 * Important:
 * 1. Administrator credentials are read from environment variables.
 * 2. The password is hashed using BCrypt before being stored.
 * 3. The administrator account is not created if the username already exists.
 * 4. Plain-text passwords and other secrets are never logged.
 */
@Configuration
public class AdminInitializer {

    /**
     * Creates the initial administrator account after the Spring application
     * context has been initialized.
     */
    @Bean
    public CommandLineRunner initializeAdmin(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,

            // Read the administrator username from the environment.
            @Value("${admin.username}") String adminUsername,

            // Use the configured administrator email address.
            @Value("${admin.email}") String adminEmail,

            // Read the administrator password without hardcoding it here.
            @Value("${admin.password}") String adminPassword) {

        return args -> {

            // Validate the configuration before attempting account creation.
            if (adminUsername.isBlank()
                    || adminEmail.isBlank()
                    || adminPassword.isBlank()) {

                throw new IllegalStateException(
                        "Administrator environment variables must not be blank."
                );
            }

            // Avoid creating a duplicate administrator username.
            if (userRepository.existsByUsername(adminUsername)) {
                System.out.println(
                        "Administrator initialization skipped: username already exists."
                );
                return;
            }

            // Avoid creating an account with an email already assigned to another user.
            if (userRepository.existsByEmail(adminEmail)) {
                throw new IllegalStateException(
                        "Administrator initialization failed: email already exists."
                );
            }

            // Create a new administrator entity.
            User admin = new User();

            admin.setUsername(adminUsername);
            admin.setEmail(adminEmail);

            // Never store the administrator password as plain text.
            admin.setPassword(passwordEncoder.encode(adminPassword));

            // Assign the administrator role.
            admin.setRole(Role.ADMIN);

            // Enable the account so it can authenticate.
            admin.setEnabled(true);

            // Save the administrator account in the database.
            userRepository.save(admin);

            // Log the outcome without exposing credentials.
            System.out.println(
                    "Initial administrator account created successfully."
            );
        };
    }
}
