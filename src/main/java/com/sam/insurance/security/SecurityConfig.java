package com.sam.insurance.security;

import java.util.Base64;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {

        http
            .csrf(csrf ->
                    csrf.disable()
            )

            .headers(headers ->
                    headers.frameOptions(
                            frame ->
                                    frame.sameOrigin()
                    )
            )

            .sessionManagement(session ->
                    session.sessionCreationPolicy(
                            SessionCreationPolicy.STATELESS
                    )
            )

            .authorizeHttpRequests(auth ->
                    auth
                        .requestMatchers(
                                "/api/auth/**",
                                "/hello",
                                "/h2-console/**",
                                "/actuator/health",
                                "/actuator/health/liveness",
                                "/actuator/health/readiness"
                        )
                        .permitAll()

                        .requestMatchers(
                                "/api/policies/**"
                        )
                        .authenticated()

                        .anyRequest()
                        .authenticated()
            )

            .oauth2ResourceServer(oauth2 ->
                    oauth2.jwt(jwt -> {
                    })
            );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {

        return new BCryptPasswordEncoder();
    }

    @Bean
    UserDetailsService userDetailsService(
            PasswordEncoder passwordEncoder
    ) {

        UserDetails user =
                User.builder()
                        .username("sam")
                        .password(
                                passwordEncoder.encode(
                                        "password123"
                                )
                        )
                        .roles("USER")
                        .build();

        return new InMemoryUserDetailsManager(
                user
        );
    }

    @Bean
    AuthenticationManager authenticationManager(
            AuthenticationConfiguration configuration
    ) throws Exception {

        return configuration
                .getAuthenticationManager();
    }

    @Bean
    SecretKey jwtSecretKey() {

        byte[] decodedKey =
                Base64.getDecoder()
                        .decode(jwtSecret);

        return new SecretKeySpec(
                decodedKey,
                "HmacSHA256"
        );
    }

    @Bean
    JwtEncoder jwtEncoder(
            SecretKey secretKey
    ) {

        return NimbusJwtEncoder
                .withSecretKey(secretKey)
                .algorithm(
                        MacAlgorithm.HS256
                )
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            SecretKey secretKey
    ) {

        return NimbusJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(
                        MacAlgorithm.HS256
                )
                .build();
    }
}