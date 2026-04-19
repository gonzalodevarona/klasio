package com.klasio.auth.infrastructure.security;

import com.klasio.auth.domain.model.Role;
import com.klasio.shared.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtTokenService {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = jwtProperties.accessTokenExpiration();
    }

    /**
     * Generates a signed JWT access token.
     * Roles are sorted by hierarchy (highest privilege first) so that {@code roles[0]}
     * is always the primary role, allowing controllers to read it as a convenience.
     */
    public String generateAccessToken(UUID userId, UUID tenantId, Set<Role> roles) {
        Instant now = Instant.now();

        List<String> sortedRoles = roles.stream()
                .sorted(Comparator.comparingInt(r -> r.hierarchy))
                .map(Role::name)
                .toList();

        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("roles", sortedRoles)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(accessTokenExpiration)))
                .signWith(secretKey);

        if (tenantId != null) {
            builder.claim("tenant_id", tenantId.toString());
        }

        return builder.compact();
    }
}
