package com.klasio.shared.infrastructure.config;

import com.klasio.auth.infrastructure.security.BCryptPasswordEncoderAdapter;
import com.klasio.shared.infrastructure.security.JwtAuthenticationFilter;
import com.klasio.shared.infrastructure.security.TenantStatusFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final TenantStatusFilter tenantStatusFilter;
    private final BCryptPasswordEncoderAdapter bCryptPasswordEncoderAdapter;

    @Value("${klasio.cors.allowed-origins:http://localhost:3000}")
    private String corsAllowedOrigins;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          TenantStatusFilter tenantStatusFilter,
                          BCryptPasswordEncoderAdapter bCryptPasswordEncoderAdapter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.tenantStatusFilter = tenantStatusFilter;
        this.bCryptPasswordEncoderAdapter = bCryptPasswordEncoderAdapter;
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return bCryptPasswordEncoderAdapter.getSpringEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/health/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/favicon.ico",
                                "/error",
                                "/dev/**",
                                "/api/v1/auth/**",
                                "/api/v1/tenants/*/register"
                        ).permitAll()
                        .requestMatchers("/api/v1/tenants/**").hasRole("SUPERADMIN")
                        .requestMatchers("/api/v1/programs/**").authenticated()
                        .requestMatchers("/api/v1/professors/**").authenticated()
                        .requestMatchers("/api/**").authenticated()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(tenantStatusFilter, JwtAuthenticationFilter.class)
                .build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Use AllowedOriginPatterns (not AllowedOrigins) so wildcard subdomains
        // like https://*.klasio.club are accepted. Required because every tenant
        // is served from its own subdomain and the list cannot be enumerated.
        List<String> patterns = Arrays.stream(corsAllowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOriginPatterns(patterns);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
