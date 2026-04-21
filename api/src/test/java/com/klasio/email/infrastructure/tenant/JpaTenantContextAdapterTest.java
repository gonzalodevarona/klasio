package com.klasio.email.infrastructure.tenant;

import com.klasio.email.domain.model.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JpaTenantContextAdapterTest {

    private final EntityManager em = mock(EntityManager.class);
    private final JpaTenantContextAdapter adapter = new JpaTenantContextAdapter(em);

    @Test
    void findById_returnsTenantContext() {
        UUID id = UUID.randomUUID();
        Query query = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(new Object[]{id, "test-slug", "Test League"}));

        TenantContext ctx = adapter.findById(id);

        assertThat(ctx.id()).isEqualTo(id);
        assertThat(ctx.slug()).isEqualTo("test-slug");
        assertThat(ctx.name()).isEqualTo("Test League");
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
