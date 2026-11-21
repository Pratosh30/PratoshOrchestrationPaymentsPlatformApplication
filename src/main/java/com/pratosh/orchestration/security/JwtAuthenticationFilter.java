package com.pratosh.orchestration.security;

import com.pratosh.orchestration.entity.User;
import com.pratosh.orchestration.repository.UserRepository;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger logger =
            LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        String authorizationHeader =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        logger.debug(
                "JWT filter invoked: method={}, path={}, authorizationHeaderPresent={}",
                request.getMethod(),
                path,
                authorizationHeader != null
        );

        // Allow requests without a token to continue.
        // Protected endpoints will be rejected by Spring Security.
        if (authorizationHeader == null
                || !authorizationHeader.startsWith("Bearer ")) {

            logger.debug(
                    "No valid Bearer Authorization header found for path={}",
                    path
            );

            filterChain.doFilter(request, response);
            return;
        }

        String token = authorizationHeader.substring(7).trim();

        if (token.isEmpty()) {
            writeUnauthorizedResponse(
                    response,
                    "Bearer token is empty"
            );
            return;
        }

        try {
            // Parse and verify the JWT signature, then extract its subject.
            String username = jwtService.extractUsername(token);

            if (username == null || username.isBlank()) {
                logger.warn("JWT subject is missing for path={}", path);

                writeUnauthorizedResponse(
                        response,
                        "Invalid access token"
                );
                return;
            }

            // Load the current user from the database.
            Optional<User> optionalUser =
                    userRepository.findByUsername(username);

            if (optionalUser.isEmpty()) {
                logger.warn("JWT subject does not match an existing user");

                writeUnauthorizedResponse(
                        response,
                        "Invalid access token"
                );
                return;
            }

            User user = optionalUser.get();

            if (!user.isEnabled()) {
                logger.warn("JWT authentication rejected: account disabled");

                writeUnauthorizedResponse(
                        response,
                        "User account is disabled"
                );
                return;
            }

            if (!jwtService.isTokenValid(token, user.getUsername())) {
                logger.warn("JWT validation returned false for path={}", path);

                writeUnauthorizedResponse(
                        response,
                        "Invalid or expired access token"
                );
                return;
            }

            // Establish authentication only if it has not already been set.
            if (SecurityContextHolder.getContext().getAuthentication() == null) {

                String authority = "ROLE_" + user.getRole().name();

                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                user.getUsername(),
                                null,
                                List.of(new SimpleGrantedAuthority(authority))
                        );

                authentication.setDetails(
                        new WebAuthenticationDetailsSource()
                                .buildDetails(request)
                );

                SecurityContextHolder.getContext()
                        .setAuthentication(authentication);

                logger.info(
                        "JWT authentication successful: username={}, authority={}",
                        user.getUsername(),
                        authority
                );
            }

        } catch (JwtException | IllegalArgumentException exception) {

            SecurityContextHolder.clearContext();

            logger.warn(
                    "JWT rejected: exceptionType={}",
                    exception.getClass().getSimpleName()
            );

            writeUnauthorizedResponse(
                    response,
                    "Invalid or expired access token"
            );
            return;

        } catch (IllegalStateException exception) {

            SecurityContextHolder.clearContext();

            logger.error(
                    "JWT configuration or validation error: {}",
                    exception.getMessage()
            );

            writeUnauthorizedResponse(
                    response,
                    "JWT configuration error"
            );
            return;
        }

        logger.debug(
                "JWT filter continuing: method={}, path={}, authenticated={}",
                request.getMethod(),
                path,
                SecurityContextHolder.getContext().getAuthentication() != null
                        && SecurityContextHolder.getContext()
                        .getAuthentication().isAuthenticated()
        );

        filterChain.doFilter(request, response);
    }

    private void writeUnauthorizedResponse(
            HttpServletResponse response,
            String message) throws IOException {

        SecurityContextHolder.clearContext();

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        response.getWriter().write(
                "{\"status\":401,\"error\":\"Unauthorized\","
                        + "\"message\":\"" + message + "\"}"
        );
    }
}
