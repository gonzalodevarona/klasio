package com.klasio.shared.infrastructure.persistence;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantContextInterceptor.class);
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
            }
        }

        return true;
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
            try (var stmt = connection.prepareStatement("SET LOCAL app.current_tenant = ?")) {
                stmt.setString(1, tenantId);
                stmt.execute();
            }
        } catch (SQLException ex) {
            log.error("Failed to set tenant context for tenant {}", tenantId, ex);
        }
    }
}
