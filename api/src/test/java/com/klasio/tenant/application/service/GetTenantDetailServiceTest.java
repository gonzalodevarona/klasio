package com.klasio.tenant.application.service;

import com.klasio.shared.infrastructure.exception.TenantNotFoundException;
import com.klasio.tenant.application.dto.TenantDetail;
import com.klasio.tenant.domain.model.ContactInfo;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantSlug;
import com.klasio.tenant.domain.port.LogoStorage;
import com.klasio.tenant.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetTenantDetailServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private LogoStorage logoStorage;

    private GetTenantDetailService service;

    @BeforeEach
    void setUp() {
        service = new GetTenantDetailService(tenantRepository, logoStorage);
    }

    @Test
    @DisplayName("should return tenant detail with logo URL when tenant has a logo")
    void execute_tenantWithLogo_returnsDetailWithLogoUrl() {
        String slug = "liga-bogota";
        String logoKey = "logos/liga-bogota/logo.png";
        String presignedUrl = "https://s3.example.com/logos/liga-bogota/logo.png?signed=true";

        Tenant tenant = Tenant.create(
                "Liga Bogota",
                "Football",
                TenantSlug.fromName("Liga Bogota"),
                new ContactInfo("contact@liga.com", "+57 300 1234567", "Bogota"),
                UUID.randomUUID(),
                logoKey
        );
        tenant.clearDomainEvents();

        when(tenantRepository.findBySlug(slug)).thenReturn(Optional.of(tenant));
        when(logoStorage.generatePresignedUrl(logoKey)).thenReturn(presignedUrl);

        TenantDetail result = service.execute(slug);

        assertThat(result.name()).isEqualTo("Liga Bogota");
        assertThat(result.slug()).isEqualTo("liga-bogota");
        assertThat(result.logoUrl()).isEqualTo(presignedUrl);
        assertThat(result.contactEmail()).isEqualTo("contact@liga.com");
        assertThat(result.contactPhone()).isEqualTo("+57 300 1234567");
        assertThat(result.contactAddress()).isEqualTo("Bogota");
        assertThat(result.status()).isEqualTo("ACTIVE");

        verify(tenantRepository).findBySlug(slug);
        verify(logoStorage).generatePresignedUrl(logoKey);
    }

    @Test
    @DisplayName("should return tenant detail with null logo URL when tenant has no logo")
    void execute_tenantWithoutLogo_returnsDetailWithNullLogoUrl() {
        String slug = "liga-bogota";

        Tenant tenant = Tenant.create(
                "Liga Bogota",
                "Football",
                TenantSlug.fromName("Liga Bogota"),
                new ContactInfo("contact@liga.com", "+57 300 1234567", "Bogota"),
                UUID.randomUUID(),
                null
        );
        tenant.clearDomainEvents();

        when(tenantRepository.findBySlug(slug)).thenReturn(Optional.of(tenant));

        TenantDetail result = service.execute(slug);

        assertThat(result.name()).isEqualTo("Liga Bogota");
        assertThat(result.logoUrl()).isNull();

        verify(tenantRepository).findBySlug(slug);
        verify(logoStorage, never()).generatePresignedUrl(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("should throw TenantNotFoundException when tenant does not exist")
    void execute_tenantNotFound_throwsException() {
        String slug = "non-existent";

        when(tenantRepository.findBySlug(slug)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(slug))
                .isInstanceOf(TenantNotFoundException.class)
                .hasMessageContaining("non-existent");

        verify(tenantRepository).findBySlug(slug);
        verify(logoStorage, never()).generatePresignedUrl(org.mockito.ArgumentMatchers.anyString());
    }
}
