package com.klasio.shared.infrastructure.persistence;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@Slf4j
@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private final DataSource dataSource;

    public TenantContextInterceptor(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof UsernamePasswordAuthenticationToken token
                && token.getDetails() instanceof Map<?, ?> details) {
            Object tenantId = details.get("tenantId");

            if (tenantId != null) {
                String tenantIdStr = tenantId.toString();
                CURRENT_TENANT.set(tenantIdStr);
                setTenantContext(tenantIdStr);
            } else if (isSuperadmin(authentication)) {
                String headerTenantId = request.getHeader("X-Tenant-Id");
                if (headerTenantId != null && !headerTenantId.isBlank()) {
                    CURRENT_TENANT.set(headerTenantId);
                    setTenantContext(headerTenantId);
                }
            }
        }

        return true;
    }

    private boolean isSuperadmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch("ROLE_SUPERADMIN"::equals);
    }

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        CURRENT_TENANT.remove();
    }

    public static String getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    private void setTenantContext(String tenantId) {
        try (Connection connection = dataSource.getConnection()) {
            try (var stmt = connection.prepareStatement("SELECT set_config('app.current_tenant', ?, false)")) {
                stmt.setString(1, tenantId);
                stmt.execute();
            }
        } catch (SQLException ex) {
            log.error("Failed to set tenant context for tenant {}", tenantId, ex);
        }
    }
}
