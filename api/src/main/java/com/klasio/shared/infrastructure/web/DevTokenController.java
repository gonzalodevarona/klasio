package com.klasio.shared.infrastructure.web;

import com.klasio.shared.infrastructure.config.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Development-only controller to generate JWT tokens for manual testing.
 * Only active with the "local" Spring profile.
 */
@RestController
@RequestMapping("/dev")
@Profile("local")
public class DevTokenController {

    private final SecretKey secretKey;

    public DevTokenController(JwtProperties jwtProperties) {
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/token")
    public ResponseEntity<Map<String, String>> generateSuperadminToken() {
        String userId = UUID.randomUUID().toString();
        Instant now = Instant.now();

        String token = Jwts.builder()
                .subject(userId)
                .claim("roles", List.of("SUPERADMIN"))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(24, ChronoUnit.HOURS)))
                .signWith(secretKey)
                .compact();

        return ResponseEntity.ok(Map.of(
                "token", token,
                "userId", userId,
                "role", "SUPERADMIN",
                "expiresIn", "24 hours"
        ));
    }
}
