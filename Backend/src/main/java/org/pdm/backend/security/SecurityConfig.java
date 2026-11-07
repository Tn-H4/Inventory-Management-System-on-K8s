package org.pdm.backend.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.pdm.backend.exception.CustomAccessDenialHandler;
import org.pdm.backend.exception.CustomAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final AuthFilter authFilter;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDenialHandler customAccessDenialHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        httpSecurity.csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .headers(headers -> headers
                        .contentSecurityPolicy(policy -> policy
                                .policyDirectives("default-src 'self'; img-src 'self' data: https:; style-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; script-src 'self' 'unsafe-inline' https://cdn.jsdelivr.net; connect-src 'self' https://cdn.jsdelivr.net; font-src 'self' https://cdn.jsdelivr.net data:;")
                        )
                )
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(customAccessDenialHandler)
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                )
                .authorizeHttpRequests(request -> request
                        .requestMatchers("/api/auth/login", "/api/auth/register").permitAll()
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        // STAFF can read products, categories, warehouses, and view transactions
                        .requestMatchers(HttpMethod.GET,
                                "/api/products/all",
                                "/api/products/{id}",
                                "/api/products/search",
                                "/api/categories/all",
                                "/api/categories/{id}",
                                "/api/warehouses/all",
                                "/api/warehouses/{id}",
                                "/api/transactions/all",
                                "/api/transactions/{id}",
                                "/api/transactions/by-month-year",
                                "/api/dashboard/stats"
                        ).hasAnyAuthority("STAFF", "MANAGER", "ADMIN")
                        // Only MANAGER/ADMIN can change transaction status
                        .requestMatchers(HttpMethod.PUT, "/api/transactions/{transactionId}").hasAnyAuthority("MANAGER", "ADMIN")
                        // MANAGER/ADMIN can view proposals
                        .requestMatchers(HttpMethod.GET,
                                "/api/proposals/all",
                                "/api/proposals/{id}",
                                "/api/proposals/pending-count"
                        ).hasAnyAuthority("MANAGER", "ADMIN")
                        // Only MANAGER can approve or reject proposals
                        .requestMatchers(HttpMethod.POST,
                                "/api/proposals/{id}/approve",
                                "/api/proposals/{id}/reject"
                        ).hasAuthority("MANAGER")
                        // ADMIN + MANAGER for transaction, product, dashboard exports
                        .requestMatchers(
                                "/api/transactions/export",
                                "/api/products/export",
                                "/api/dashboard/export"
                        ).hasAnyAuthority("ADMIN", "MANAGER")
                        .requestMatchers("/api/proposals/export").hasAuthority("MANAGER")
                        // ADMIN only for audit logs and remaining CSV exports
                        .requestMatchers(
                                "/api/audit-logs/all",
                                "/api/audit-logs/{id}",
                                "/api/categories/export",
                                "/api/suppliers/export",
                                "/api/warehouses/export",
                                "/api/users/export",
                                "/api/audit-logs/export"
                        ).hasAuthority("ADMIN")
                        .anyRequest().authenticated()
                )
                .sessionManagement(manager -> manager.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterBefore(authFilter, UsernamePasswordAuthenticationFilter.class);
        return httpSecurity.build();

    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
