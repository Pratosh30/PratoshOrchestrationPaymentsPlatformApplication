package com.pratosh.orchestration.config;

import com.pratosh.orchestration.security.JwtAuthenticationFilter;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .exceptionHandling(exceptions -> exceptions

                        // The request has no valid authentication.
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                            response.getWriter().write(
                                    "{\"status\":401,\"error\":\"Unauthorized\","
                                            + "\"message\":\"Authentication is required. "
                                            + "Check your Bearer token.\"}"
                            );
                        })

                        // The user is authenticated but lacks the required authority.
                        .accessDeniedHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                            response.getWriter().write(
                                    "{\"status\":403,\"error\":\"Forbidden\","
                                            + "\"message\":\"You do not have permission "
                                            + "to access this resource.\"}"
                            );
                        })
                )

                .authorizeHttpRequests(auth -> auth

                        // Container error dispatches (404, 405, 500 ...) are
                        // forwarded to /error. The JWT filter does not run on
                        // that dispatch, so without this rule every real error
                        // is rewritten into a misleading 401 Unauthorized.
                        // The original request was already authorized.
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        // Allow users to log in without an existing JWT.
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/login"
                        ).permitAll()

                        // Only administrators can access admin endpoints.
                        .requestMatchers("/api/v1/admin/**")
                        .hasAuthority("ROLE_ADMIN")

                        // All other endpoints require authentication.
                        .anyRequest().authenticated()
                )

                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                .addFilterBefore(
                        jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}