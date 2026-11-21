package com.pratosh.orchestration.repository;

import com.pratosh.orchestration.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository for database operations related to User entities.
 *
 * Spring Data JPA automatically provides the implementation
 * of this interface when the application starts.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Finds a user using their unique username.
     *
     * Spring Data JPA generates the database query automatically.
     *
     * @param username the username to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByUsername(String username);

    /**
     * Finds a user using their unique email address.
     *
     * @param email the email address to search for
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Checks whether a username already exists.
     *
     * Useful when creating an account to prevent duplicate usernames.
     *
     * @param username the username to check
     * @return true if the username exists, otherwise false
     */
    boolean existsByUsername(String username);

    /**
     * Checks whether an email address already exists.
     *
     * Useful when creating an account to prevent duplicate email addresses.
     *
     * @param email the email address to check
     * @return true if the email exists, otherwise false
     */
    boolean existsByEmail(String email);
}