package com.klasio.shared.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.klasio.shared.infrastructure.exception.ErrorResponse;
import com.klasio.tenant.domain.port.TenantCacheEviction;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.sql.DataSource;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

@Component
public class TenantStatusFilter extends OncePerRequestFilter implements TenantCacheEviction {

    private static final Logger log = LoggerFactory.getLogger(TenantStatusFilter.class);

    private static final String TENANT_STATUS_QUERY = "SELECT status FROM tenants WHERE id = ?";
    private static final String ACTIVE_STATUS = "ACTIVE";

    private final DataSource dataSource;
    private final ObjectMapper objectMapper;
    private final Cache<UUID, String> tenantStatusCache;

    public TenantStatusFilter(DataSource dataSource, ObjectMapper objectMapper) {
        this.dataSource = dataSource;
        this.objectMapper = objectMapper;
        this.tenantStatusCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(5))
                .maximumSize(1000)
                .build();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String tenantId = extractTenantId(authentication);

        if (tenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        UUID tenantUuid;
        try {
            tenantUuid = UUID.fromString(tenantId);
        } catch (IllegalArgumentException ex) {
            filterChain.doFilter(request, response);
            return;
        }

        String status = tenantStatusCache.get(tenantUuid, this::queryTenantStatus);

        if (status == null || !ACTIVE_STATUS.equals(status)) {
            writeErrorResponse(response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    public void evictTenant(UUID tenantId) {
        tenantStatusCache.invalidate(tenantId);
    }

    private String extractTenantId(Authentication authentication) {
        if (authentication instanceof UsernamePasswordAuthenticationToken token
                && token.getDetails() instanceof Map<?, ?> details) {
            Object tenantId = details.get("tenantId");
            return tenantId != null ? tenantId.toString() : null;
        }
        return null;
    }

    private String queryTenantStatus(UUID tenantId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement stmt = connection.prepareStatement(TENANT_STATUS_QUERY)) {
            stmt.setObject(1, tenantId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("status");
                }
            }
        } catch (SQLException ex) {
            log.error("Failed to query tenant status for tenant: {}", tenantId, ex);
        }
        return null;
    }

    private void writeErrorResponse(HttpServletResponse response) throws IOException {
        var error = new ErrorResponse.ErrorDetail("TENANT_INACTIVE", "Tenant is inactive");
        var errorResponse = new ErrorResponse(error);

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }
}
