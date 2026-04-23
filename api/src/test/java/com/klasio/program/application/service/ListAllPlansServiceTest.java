package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramPlanSummary;
import com.klasio.program.domain.model.Program;
import com.klasio.program.domain.model.ProgramId;
import com.klasio.program.domain.model.ProgramModality;
import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.model.ProgramStatus;
import com.klasio.program.domain.port.ProgramPlanRepository;
import com.klasio.program.domain.port.ProgramRepository;
import com.klasio.shared.domain.port.UserDisplayNamePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListAllPlansServiceTest {

    @Mock private ProgramPlanRepository planRepository;
    @Mock private ProgramRepository programRepository;
    @Mock private UserDisplayNamePort userDisplayNamePort;

    private ListAllPlansService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID MANAGER_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ListAllPlansService(planRepository, programRepository, userDisplayNamePort);
    }

    @Test
    @DisplayName("should return summaries with resolved manager names and program names")
    void execute_resolvesBothManagerAndProgramNames() {
        ProgramPlan plan = ProgramPlan.create(PROGRAM_ID, TENANT_ID, "4 Hours",
                ProgramModality.HOURS_BASED, new BigDecimal("90000"), 4, List.of(), MANAGER_ID, UUID.randomUUID());
        Program program = Program.reconstitute(new ProgramId(PROGRAM_ID), TENANT_ID, "Kids Soccer",
                ProgramStatus.ACTIVE, Instant.now(), UUID.randomUUID(), null, null);

        when(planRepository.findAllByTenant(TENANT_ID, null)).thenReturn(List.of(plan));
        when(programRepository.findById(TENANT_ID, PROGRAM_ID)).thenReturn(Optional.of(program));
        when(userDisplayNamePort.findDisplayName(MANAGER_ID)).thenReturn(Optional.of("Carlos Manager"));

        List<ProgramPlanSummary> result = service.execute(TENANT_ID, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).managerName()).isEqualTo("Carlos Manager");
        assertThat(result.get(0).programName()).isEqualTo("Kids Soccer");
    }
}
