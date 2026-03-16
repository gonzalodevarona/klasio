package com.klasio.tenant.application.service;

import com.klasio.shared.infrastructure.exception.SlugAlreadyExistsException;
import com.klasio.tenant.application.dto.CreateTenantCommand;
import com.klasio.tenant.domain.event.TenantCreated;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.port.LogoStorage;
import com.klasio.tenant.domain.port.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateTenantServiceTest {

    @Mock
    private TenantRepository tenantRepository;

    @Mock
    private LogoStorage logoStorage;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private CreateTenantService service;

    @BeforeEach
    void setUp() {
        service = new CreateTenantService(tenantRepository, logoStorage, eventPublisher);
    }

    @Test
    @DisplayName("should create tenant, save it, and publish domain events")
    void happyPath_createsTenantAndPublishesEvents() {
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);

        CreateTenantCommand command = new CreateTenantCommand(
                "Liga Bogota Futbol",
                "Football",
                null,
                "contact@liga.com",
                "+57 300 1234567",
                "Bogota, Colombia",
                null,
                null,
                0,
                UUID.randomUUID()
        );

        Tenant result = service.execute(command);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("Liga Bogota Futbol");
        assertThat(result.getSportDiscipline()).isEqualTo("Football");
        assertThat(result.getContactInfo().email()).isEqualTo("contact@liga.com");

        ArgumentCaptor<Tenant> tenantCaptor = ArgumentCaptor.forClass(Tenant.class);
        verify(tenantRepository).save(tenantCaptor.capture());
        assertThat(tenantCaptor.getValue().getName()).isEqualTo("Liga Bogota Futbol");

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(eventCaptor.capture());
        assertThat(eventCaptor.getValue()).isInstanceOf(TenantCreated.class);

        TenantCreated event = (TenantCreated) eventCaptor.getValue();
        assertThat(event.name()).isEqualTo("Liga Bogota Futbol");
        assertThat(event.createdBy()).isEqualTo(command.createdBy());
    }

    @Test
    @DisplayName("should throw SlugAlreadyExistsException with suggested slug when slug is taken")
    void duplicateSlug_throwsSlugAlreadyExistsException() {
        when(tenantRepository.existsBySlug("liga-bogota")).thenReturn(true);

        CreateTenantCommand command = new CreateTenantCommand(
                "Liga Bogota",
                "Football",
                "liga-bogota",
                "contact@liga.com",
                null,
                null,
                null,
                null,
                0,
                UUID.randomUUID()
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(SlugAlreadyExistsException.class)
                .satisfies(ex -> {
                    SlugAlreadyExistsException slugEx = (SlugAlreadyExistsException) ex;
                    assertThat(slugEx.getSuggestedSlug()).isEqualTo("liga-bogota-2");
                });

        verify(tenantRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("should propagate exception and not save tenant when logo upload fails")
    void logoUploadFailure_propagatesExceptionAndDoesNotSave() {
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        when(logoStorage.upload(any(UUID.class), any(InputStream.class), anyString(), anyLong()))
                .thenThrow(new RuntimeException("S3 connection failed"));

        InputStream logoData = new ByteArrayInputStream(new byte[]{1, 2, 3});
        CreateTenantCommand command = new CreateTenantCommand(
                "Liga Bogota",
                "Football",
                null,
                "contact@liga.com",
                null,
                null,
                logoData,
                "image/png",
                3,
                UUID.randomUUID()
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("S3 connection failed");

        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("should delete uploaded logo when tenant save fails (compensating action)")
    void logoRollbackOnSaveFailure_deletesUploadedLogo() {
        when(tenantRepository.existsBySlug(anyString())).thenReturn(false);
        String uploadedKey = "logos/test/image.png";
        when(logoStorage.upload(any(UUID.class), any(InputStream.class), anyString(), anyLong()))
                .thenReturn(uploadedKey);
        doThrow(new RuntimeException("Database error")).when(tenantRepository).save(any(Tenant.class));

        InputStream logoData = new ByteArrayInputStream(new byte[]{1, 2, 3});
        CreateTenantCommand command = new CreateTenantCommand(
                "Liga Bogota",
                "Football",
                null,
                "contact@liga.com",
                null,
                null,
                logoData,
                "image/png",
                3,
                UUID.randomUUID()
        );

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Database error");

        verify(logoStorage).delete(eq(uploadedKey));
    }
}
