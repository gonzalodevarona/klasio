package com.klasio.shared.infrastructure.config;

import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class TenantContextConfig implements WebMvcConfigurer {

    private final TenantContextInterceptor tenantContextInterceptor;

    public TenantContextConfig(TenantContextInterceptor tenantContextInterceptor) {
        this.tenantContextInterceptor = tenantContextInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantContextInterceptor)
                .addPathPatterns("/api/**");
    }
}
