package com.pratosh.orchestration.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.LocalDateTime;

// IMPORTANT:
// Use our custom Role enum, not javax.management.relation.Role.
 import com.pratosh.orchestration.entity.Role;
/**
 * Represents a user account stored in the database.
 * Initially, we will create administrator accounts only.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_users_username",
                        columnNames = "username"
                ),
                @UniqueConstraint(
                        name = "uk_users_email",
                        columnNames = "email"
                )
        }
)
public class User {

    // Automatically generated primary key.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique username used for login.
    @Column(nullable = false, length = 50)
    private String username;

    // Unique email address associated with the account.
    @Column(nullable = false, length = 150)
    private String email;

    // Stores a BCrypt password hash, never the raw password.
    @Column(nullable = false, length = 100)
    private String password;

    // Stores the user's role as text in the database.
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    // Disabled accounts must not be allowed to log in.
    @Column(nullable = false)
    private boolean enabled = true;

    // Records when the account was created.
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Required by JPA when creating entity objects.
    public User() {
    }

    // Initializes the creation timestamp before inserting the record.
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
