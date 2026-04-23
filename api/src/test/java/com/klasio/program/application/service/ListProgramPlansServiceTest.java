package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramPlanSummary;
import com.klasio.program.domain.model.ProgramModality;
import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.port.ProgramPlanRepository;
import com.klasio.shared.domain.port.UserDisplayNamePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListProgramPlansServiceTest {

    @Mock private ProgramPlanRepository planRepository;
    @Mock private UserDisplayNamePort userDisplayNamePort;

    private ListProgramPlansService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID MANAGER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ListProgramPlansService(planRepository, userDisplayNamePort);
    }

    @Test
    @DisplayName("should return summaries with resolved manager names")
    void execute_resolvesManagerNames() {
        ProgramPlan plan = ProgramPlan.create(PROGRAM_ID, TENANT_ID, "4 Hours",
                ProgramModality.HOURS_BASED, new BigDecimal("90000"), 4, List.of(), MANAGER_ID, UUID.randomUUID());

        when(planRepository.findAllByProgram(TENANT_ID, PROGRAM_ID)).thenReturn(List.of(plan));
        when(userDisplayNamePort.findDisplayName(MANAGER_ID)).thenReturn(Optional.of("Carlos Manager"));

        List<ProgramPlanSummary> result = service.execute(TENANT_ID, PROGRAM_ID);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).managerName()).isEqualTo("Carlos Manager");
    }

    @Test
    @DisplayName("should fall back to UUID string for unknown manager")
    void execute_fallsBackToUuidString() {
        ProgramPlan plan = ProgramPlan.create(PROGRAM_ID, TENANT_ID, "4 Hours",
                ProgramModality.HOURS_BASED, new BigDecimal("90000"), 4, List.of(), MANAGER_ID, UUID.randomUUID());

        when(planRepository.findAllByProgram(TENANT_ID, PROGRAM_ID)).thenReturn(List.of(plan));
        when(userDisplayNamePort.findDisplayName(MANAGER_ID)).thenReturn(Optional.empty());

        List<ProgramPlanSummary> result = service.execute(TENANT_ID, PROGRAM_ID);

        assertThat(result.get(0).managerName()).isEqualTo(MANAGER_ID.toString());
    }
}
