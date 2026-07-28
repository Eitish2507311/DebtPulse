package com.debtpulse.legal.config;

import com.debtpulse.common.security.RoleBasedHeaderFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Stateless security for legal-service.
 *
 * <p>The API Gateway validates the JWT and forwards identity headers; the shared
 * {@link RoleBasedHeaderFilter} rebuilds the authenticated principal so method-level
 * {@code @PreAuthorize} rules are enforced here. Only docs/actuator/error are public;
 * {@code /api/internal/**} stays authenticated (reached service-to-service via Feign).</p>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**",
                                "/webjars/**", "/actuator/**", "/error"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new RoleBasedHeaderFilter(), UsernamePasswordAuthenticationFilter.class)
                .build();
    }
}
