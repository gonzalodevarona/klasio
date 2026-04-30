package com.klasio.admin.dashboard.application.service;

import com.klasio.admin.dashboard.application.dto.AdminDashboardDto;
import com.klasio.admin.dashboard.application.dto.DashboardStudentDto;
import com.klasio.admin.dashboard.application.port.AdminDashboardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAdminDashboardServiceTest {

    @Mock
    private AdminDashboardRepository repository;

    private GetAdminDashboardService service;

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        service = new GetAdminDashboardService(repository);
    }

    @Test
    @DisplayName("maps all KPI counts from repository")
    void mapsAllKpiFields() {
        when(repository.countStudents(TENANT_ID)).thenReturn(248L);
        when(repository.countNewStudentsThisMonth(TENANT_ID)).thenReturn(12L);
        when(repository.countActiveMemberships(TENANT_ID)).thenReturn(1840L);
        when(repository.countPendingProofs(TENANT_ID)).thenReturn(14L);
        when(repository.countActivePrograms(TENANT_ID)).thenReturn(7L);
        when(repository.findStudentSummaries(TENANT_ID)).thenReturn(List.of());

        AdminDashboardDto result = service.execute(TENANT_ID);

        assertThat(result.studentCount()).isEqualTo(248);
        assertThat(result.newStudentsThisMonth()).isEqualTo(12);
        assertThat(result.activeMembershipCount()).isEqualTo(1840);
        assertThat(result.pendingPaymentProofs()).isEqualTo(14);
        assertThat(result.activeProgramCount()).isEqualTo(7);
        assertThat(result.students()).isEmpty();
    }

    @Test
    @DisplayName("passes tenantId to every repository call")
    void passesTenantIdToEveryCall() {
        when(repository.countStudents(TENANT_ID)).thenReturn(0L);
        when(repository.countNewStudentsThisMonth(TENANT_ID)).thenReturn(0L);
        when(repository.countActiveMemberships(TENANT_ID)).thenReturn(0L);
        when(repository.countPendingProofs(TENANT_ID)).thenReturn(0L);
        when(repository.countActivePrograms(TENANT_ID)).thenReturn(0L);
        when(repository.findStudentSummaries(TENANT_ID)).thenReturn(List.of());

        service.execute(TENANT_ID);

        verify(repository).countStudents(TENANT_ID);
        verify(repository).countNewStudentsThisMonth(TENANT_ID);
        verify(repository).countActiveMemberships(TENANT_ID);
        verify(repository).countPendingProofs(TENANT_ID);
        verify(repository).countActivePrograms(TENANT_ID);
        verify(repository).findStudentSummaries(TENANT_ID);
    }

    @Test
    @DisplayName("includes student summaries in result")
    void includesStudentSummaries() {
        DashboardStudentDto student = new DashboardStudentDto(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Carlos Rodriguez",
                "Swimming Advanced",
                "ACTIVE",
                4,
                24
        );
        when(repository.countStudents(TENANT_ID)).thenReturn(1L);
        when(repository.countNewStudentsThisMonth(TENANT_ID)).thenReturn(0L);
        when(repository.countActiveMemberships(TENANT_ID)).thenReturn(20L);
        when(repository.countPendingProofs(TENANT_ID)).thenReturn(0L);
        when(repository.countActivePrograms(TENANT_ID)).thenReturn(1L);
        when(repository.findStudentSummaries(TENANT_ID)).thenReturn(List.of(student));

        AdminDashboardDto result = service.execute(TENANT_ID);

        assertThat(result.students()).hasSize(1);
        assertThat(result.students().get(0).name()).isEqualTo("Carlos Rodriguez");
        assertThat(result.students().get(0).membershipStatus()).isEqualTo("ACTIVE");
        assertThat(result.students().get(0).availableHours()).isEqualTo(4);
        assertThat(result.students().get(0).purchasedHours()).isEqualTo(24);
    }
}
