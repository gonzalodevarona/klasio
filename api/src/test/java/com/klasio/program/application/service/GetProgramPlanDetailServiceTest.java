package com.klasio.program.application.service;

import com.klasio.program.application.dto.ProgramPlanDetail;
import com.klasio.program.domain.model.ProgramModality;
import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.port.ProgramPlanRepository;
import com.klasio.shared.domain.port.UserDisplayNamePort;
import com.klasio.shared.infrastructure.exception.ProgramPlanNotFoundException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetProgramPlanDetailServiceTest {

    @Mock private ProgramPlanRepository planRepository;
    @Mock private UserDisplayNamePort userDisplayNamePort;

    private GetProgramPlanDetailService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();
    private static final UUID MANAGER_ID = UUID.randomUUID();
    private static final UUID CREATOR_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GetProgramPlanDetailService(planRepository, userDisplayNamePort);
    }

    @Test
    @DisplayName("should return plan detail with resolved manager and creator names")
    void execute_resolvesBothManagerAndCreatorNames() {
        ProgramPlan plan = ProgramPlan.create(PROGRAM_ID, TENANT_ID, "4 Hours",
                ProgramModality.HOURS_BASED, new BigDecimal("90000"), 4, List.of(), MANAGER_ID, CREATOR_ID);
        UUID planId = plan.getId().value();

        when(planRepository.findById(TENANT_ID, planId)).thenReturn(Optional.of(plan));
        when(userDisplayNamePort.findDisplayName(MANAGER_ID)).thenReturn(Optional.of("Carlos Manager"));
        when(userDisplayNamePort.findDisplayName(CREATOR_ID)).thenReturn(Optional.of("Ana Admin"));

        ProgramPlanDetail result = service.execute(TENANT_ID, planId);

        assertThat(result.managerName()).isEqualTo("Carlos Manager");
        assertThat(result.createdBy()).isEqualTo("Ana Admin");
        assertThat(result.updatedBy()).isNull();
    }

    @Test
    @DisplayName("should fall back to UUID string when user not found")
    void execute_fallsBackToUuidString_whenUserNotFound() {
        ProgramPlan plan = ProgramPlan.create(PROGRAM_ID, TENANT_ID, "4 Hours",
                ProgramModality.HOURS_BASED, new BigDecimal("90000"), 4, List.of(), MANAGER_ID, CREATOR_ID);
        UUID planId = plan.getId().value();

        when(planRepository.findById(TENANT_ID, planId)).thenReturn(Optional.of(plan));
        when(userDisplayNamePort.findDisplayName(any())).thenReturn(Optional.empty());

        ProgramPlanDetail result = service.execute(TENANT_ID, planId);

        assertThat(result.managerName()).isEqualTo(MANAGER_ID.toString());
        assertThat(result.createdBy()).isEqualTo(CREATOR_ID.toString());
    }

    @Test
    @DisplayName("should throw ProgramPlanNotFoundException when plan does not exist")
    void execute_throwsWhenPlanNotFound() {
        UUID planId = UUID.randomUUID();
        when(planRepository.findById(TENANT_ID, planId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(TENANT_ID, planId))
                .isInstanceOf(ProgramPlanNotFoundException.class)
                .hasMessage("Plan not found");
    }
}
