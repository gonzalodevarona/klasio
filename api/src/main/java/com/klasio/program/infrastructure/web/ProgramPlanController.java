package com.klasio.program.infrastructure.web;

import com.klasio.program.application.dto.CreateProgramPlanCommand;
import com.klasio.program.application.dto.ProgramPlanDetail;
import com.klasio.program.application.dto.UpdateProgramPlanCommand;
import com.klasio.program.application.port.input.CreateProgramPlanUseCase;
import com.klasio.program.application.port.input.DeactivateProgramPlanUseCase;
import com.klasio.program.application.port.input.GetProgramPlanDetailUseCase;
import com.klasio.program.application.port.input.ListProgramPlansUseCase;
import com.klasio.program.application.port.input.ReactivateProgramPlanUseCase;
import com.klasio.program.application.port.input.UpdateProgramPlanUseCase;
import com.klasio.program.domain.model.ProgramPlan;
import com.klasio.program.domain.model.ScheduleEntry;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/programs/{programId}/plans")
public class ProgramPlanController {

    private final CreateProgramPlanUseCase createProgramPlanUseCase;
    private final ListProgramPlansUseCase listProgramPlansUseCase;
    private final GetProgramPlanDetailUseCase getProgramPlanDetailUseCase;
    private final UpdateProgramPlanUseCase updateProgramPlanUseCase;
    private final DeactivateProgramPlanUseCase deactivateProgramPlanUseCase;
    private final ReactivateProgramPlanUseCase reactivateProgramPlanUseCase;

    public ProgramPlanController(CreateProgramPlanUseCase createProgramPlanUseCase,
                                 ListProgramPlansUseCase listProgramPlansUseCase,
                                 GetProgramPlanDetailUseCase getProgramPlanDetailUseCase,
                                 UpdateProgramPlanUseCase updateProgramPlanUseCase,
                                 DeactivateProgramPlanUseCase deactivateProgramPlanUseCase,
                                 ReactivateProgramPlanUseCase reactivateProgramPlanUseCase) {
        this.createProgramPlanUseCase = createProgramPlanUseCase;
        this.listProgramPlansUseCase = listProgramPlansUseCase;
        this.getProgramPlanDetailUseCase = getProgramPlanDetailUseCase;
        this.updateProgramPlanUseCase = updateProgramPlanUseCase;
        this.deactivateProgramPlanUseCase = deactivateProgramPlanUseCase;
        this.reactivateProgramPlanUseCase = reactivateProgramPlanUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ProgramPlanResponseDto.ProgramPlanDetailResponse> createPlan(
            @PathVariable UUID programId,
            @Valid @RequestBody ProgramPlanRequestDto.CreateProgramPlanRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        List<ScheduleEntry> scheduleEntries = toScheduleEntries(request.scheduleEntries());

        CreateProgramPlanCommand command = new CreateProgramPlanCommand(
                tenantId, programId, request.name(), request.modality(), request.cost(),
                request.hours(), scheduleEntries, request.managerId(), userId);

        ProgramPlan plan = createProgramPlanUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProgramPlanResponseDto.ProgramPlanDetailResponse.fromDomain(plan));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<List<ProgramPlanResponseDto.ProgramPlanSummaryResponse>> listPlans(
            @PathVariable UUID programId) {

        UUID tenantId = extractTenantId();

        List<ProgramPlanResponseDto.ProgramPlanSummaryResponse> result = listProgramPlansUseCase
                .execute(tenantId, programId).stream()
                .map(ProgramPlanResponseDto.ProgramPlanSummaryResponse::fromSummary)
                .toList();

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{planId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ProgramPlanResponseDto.ProgramPlanDetailResponse> getPlanDetail(
            @PathVariable UUID programId,
            @PathVariable UUID planId) {

        UUID tenantId = extractTenantId();
        ProgramPlanDetail detail = getProgramPlanDetailUseCase.execute(tenantId, planId);

        return ResponseEntity.ok(ProgramPlanResponseDto.ProgramPlanDetailResponse.fromDetail(detail));
    }

    @PutMapping("/{planId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ProgramPlanResponseDto.ProgramPlanDetailResponse> updatePlan(
            @PathVariable UUID programId,
            @PathVariable UUID planId,
            @Valid @RequestBody ProgramPlanRequestDto.UpdateProgramPlanRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        List<ScheduleEntry> scheduleEntries = toScheduleEntries(request.scheduleEntries());

        UpdateProgramPlanCommand command = new UpdateProgramPlanCommand(
                tenantId, programId, planId, request.name(), request.cost(),
                request.hours(), scheduleEntries, request.managerId(), userId);

        ProgramPlan plan = updateProgramPlanUseCase.execute(command);

        return ResponseEntity.ok(ProgramPlanResponseDto.ProgramPlanDetailResponse.fromDomain(plan));
    }

    @PostMapping("/{planId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ProgramPlanResponseDto.ProgramPlanDetailResponse> deactivatePlan(
            @PathVariable UUID programId,
            @PathVariable UUID planId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        ProgramPlan plan = deactivateProgramPlanUseCase.execute(tenantId, planId, userId);

        return ResponseEntity.ok(ProgramPlanResponseDto.ProgramPlanDetailResponse.fromDomain(plan));
    }

    @PostMapping("/{planId}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ProgramPlanResponseDto.ProgramPlanDetailResponse> reactivatePlan(
            @PathVariable UUID programId,
            @PathVariable UUID planId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        ProgramPlan plan = reactivateProgramPlanUseCase.execute(tenantId, planId, userId);

        return ResponseEntity.ok(ProgramPlanResponseDto.ProgramPlanDetailResponse.fromDomain(plan));
    }

    private List<ScheduleEntry> toScheduleEntries(List<ProgramPlanRequestDto.ScheduleEntryRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        return requests.stream()
                .map(r -> new ScheduleEntry(
                        DayOfWeek.valueOf(r.dayOfWeek()),
                        LocalTime.parse(r.startTime()),
                        LocalTime.parse(r.endTime())))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private UUID extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        return UUID.fromString((String) details.get("userId"));
    }

    @SuppressWarnings("unchecked")
    private UUID extractTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        String tenantId = (String) details.get("tenantId");

        if (tenantId == null) {
            tenantId = TenantContextInterceptor.getCurrentTenant();
        }

        if (tenantId == null) {
            throw new IllegalStateException(
                    "No tenant context available. Provide tenant_id in JWT or X-Tenant-Id header.");
        }

        return UUID.fromString(tenantId);
    }
}
