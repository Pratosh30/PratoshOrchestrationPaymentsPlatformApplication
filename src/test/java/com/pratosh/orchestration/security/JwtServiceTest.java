package com.pratosh.orchestration.security;

import com.pratosh.orchestration.entity.Role;
import com.pratosh.orchestration.entity.User;

import io.jsonwebtoken.JwtException;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static String secret(int seed) {
        byte[] key = new byte[32];
        for (int i = 0; i < key.length; i++) {
            key[i] = (byte) (i + seed);
        }
        return Base64.getEncoder().encodeToString(key);
    }

    private JwtService service(String secret, long expirationMs) {
        JwtService service = new JwtService();
        ReflectionTestUtils.setField(service, "jwtSecret", secret);
        ReflectionTestUtils.setField(service, "jwtExpirationMs", expirationMs);
        return service;
    }

    private User admin() {
        User user = new User();
        user.setUsername("admin");
        user.setRole(Role.ADMIN);
        ReflectionTestUtils.setField(user, "id", 1L);
        return user;
    }

    @Test
    void validTokenIsAcceptedForItsUser() {
        JwtService service = service(secret(1), 60_000);
        String token = service.generateToken(admin());

        assertEquals("admin", service.extractUsername(token));
        assertTrue(service.isTokenValid(token, "admin"));
        assertFalse(service.isTokenValid(token, "someone-else"));
    }

    @Test
    void expiredTokenIsRejected() throws Exception {
        JwtService service = service(secret(1), 1);
        String token = service.generateToken(admin());
        Thread.sleep(50);

        assertThrows(JwtException.class,
                () -> service.extractUsername(token));
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String token = service(secret(1), 60_000).generateToken(admin());

        assertThrows(JwtException.class,
                () -> service(secret(2), 60_000).extractUsername(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService service = service(secret(1), 60_000);
        String token = service.generateToken(admin());
        String tampered = token.substring(0, token.length() - 2) + "xx";

        assertThrows(JwtException.class,
                () -> service.extractUsername(tampered));
    }

    @Test
    void missingSecretFailsFast() {
        JwtService service = service("", 60_000);

        assertThrows(IllegalStateException.class,
                () -> service.generateToken(admin()));
    }
}
