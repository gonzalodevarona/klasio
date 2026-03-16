package com.klasio.tenant.application.service;

import com.klasio.tenant.application.dto.TenantSummary;
import com.klasio.tenant.domain.model.ContactInfo;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.model.TenantSlug;
import com.klasio.tenant.domain.model.TenantStatus;
import com.klasio.tenant.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListTenantsServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    private ListTenantsService service;

    @BeforeEach
    void setUp() {
        service = new ListTenantsService(tenantRepository);
    }

    @Test
    @DisplayName("should return paginated list of tenant summaries")
    void execute_returnsPaginatedTenantSummaries() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Tenant tenant = Tenant.create(
                "Liga Bogota",
                "Football",
                TenantSlug.fromName("Liga Bogota"),
                new ContactInfo("contact@liga.com", "+57 300 1234567", "Bogota"),
                UUID.randomUUID(),
                null
        );
        tenant.clearDomainEvents();

        Page<Tenant> tenantPage = new PageImpl<>(List.of(tenant), pageable, 1);
        when(tenantRepository.findAll(eq(pageable), isNull())).thenReturn(tenantPage);

        Page<TenantSummary> result = service.execute(pageable, null);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).name()).isEqualTo("Liga Bogota");
        assertThat(result.getContent().get(0).slug()).isEqualTo("liga-bogota");
        assertThat(result.getContent().get(0).status()).isEqualTo("ACTIVE");
        assertThat(result.getTotalElements()).isEqualTo(1);
        verify(tenantRepository).findAll(eq(pageable), isNull());
    }

    @Test
    @DisplayName("should filter by status when status filter is provided")
    void execute_withStatusFilter_filtersbyStatus() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Tenant tenant = Tenant.create(
                "Liga Bogota",
                "Football",
                TenantSlug.fromName("Liga Bogota"),
                new ContactInfo("contact@liga.com", "+57 300 1234567", "Bogota"),
                UUID.randomUUID(),
                null
        );
        tenant.clearDomainEvents();

        Page<Tenant> tenantPage = new PageImpl<>(List.of(tenant), pageable, 1);
        when(tenantRepository.findAll(eq(pageable), eq(TenantStatus.ACTIVE))).thenReturn(tenantPage);

        Page<TenantSummary> result = service.execute(pageable, "ACTIVE");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).status()).isEqualTo("ACTIVE");
        verify(tenantRepository).findAll(eq(pageable), eq(TenantStatus.ACTIVE));
    }

    @Test
    @DisplayName("should return empty page when no tenants exist")
    void execute_noTenants_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Tenant> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(tenantRepository.findAll(eq(pageable), isNull())).thenReturn(emptyPage);

        Page<TenantSummary> result = service.execute(pageable, null);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    @DisplayName("should handle case-insensitive status filter")
    void execute_withLowercaseStatusFilter_filtersCorrectly() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Tenant> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);
        when(tenantRepository.findAll(eq(pageable), eq(TenantStatus.INACTIVE))).thenReturn(emptyPage);

        Page<TenantSummary> result = service.execute(pageable, "inactive");

        assertThat(result.getContent()).isEmpty();
        verify(tenantRepository).findAll(eq(pageable), eq(TenantStatus.INACTIVE));
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid status filter")
    void execute_withInvalidStatusFilter_throwsException() {
        Pageable pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"));

        assertThatThrownBy(() -> service.execute(pageable, "INVALID"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
