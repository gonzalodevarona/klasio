package com.klasio.email.infrastructure.tenant;

import com.klasio.email.domain.model.TenantContext;
import com.klasio.tenant.domain.port.LogoStorage;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JpaTenantContextAdapterTest {

    private final EntityManager em = mock(EntityManager.class);
    private final LogoStorage logoStorage = mock(LogoStorage.class);
    private final JpaTenantContextAdapter adapter =
            new JpaTenantContextAdapter(em, logoStorage);

    @Test
    void findById_withLogoKey_returnsLogoUrl() {
        UUID id = UUID.randomUUID();
        Query query = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(new Object[]{
                id, "test-slug", "Test League", "en",
                "America/Bogota", "logos/abc/img.png"}));
        when(logoStorage.getPublicUrl("logos/abc/img.png"))
                .thenReturn("https://klasio.s3.us-east-1.amazonaws.com/logos/abc/img.png");

        TenantContext ctx = adapter.findById(id);

        assertThat(ctx.id()).isEqualTo(id);
        assertThat(ctx.slug()).isEqualTo("test-slug");
        assertThat(ctx.name()).isEqualTo("Test League");
        assertThat(ctx.language()).isEqualTo("en");
        assertThat(ctx.timezone()).isEqualTo("America/Bogota");
        assertThat(ctx.logoUrl())
                .isEqualTo("https://klasio.s3.us-east-1.amazonaws.com/logos/abc/img.png");
    }

    @Test
    void findById_withoutLogoKey_returnsNullLogoUrl() {
        UUID id = UUID.randomUUID();
        Query query = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(new Object[]{
                id, "test-slug", "Test League", "en", "America/Bogota", null}));
        when(logoStorage.getPublicUrl(null)).thenReturn(null);

        TenantContext ctx = adapter.findById(id);

        assertThat(ctx.logoUrl()).isNull();
    }

    @Test
    void findById_unknownTenant_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        Query query = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        assertThatThrownBy(() -> adapter.findById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant not found");
    }
}
