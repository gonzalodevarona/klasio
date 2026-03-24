package com.klasio.program.infrastructure.web;

import com.klasio.program.application.dto.CreateProgramCommand;
import com.klasio.program.application.dto.ProgramDetail;
import com.klasio.program.application.dto.UpdateProgramCommand;
import com.klasio.program.application.port.input.CreateProgramUseCase;
import com.klasio.program.application.port.input.DeactivateProgramUseCase;
import com.klasio.program.application.port.input.GetProgramDetailUseCase;
import com.klasio.program.application.port.input.ListProgramsUseCase;
import com.klasio.program.application.port.input.ReactivateProgramUseCase;
import com.klasio.program.application.port.input.UpdateProgramUseCase;
import com.klasio.program.domain.model.Program;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/programs")
public class ProgramController {

    private final CreateProgramUseCase createProgramUseCase;
    private final ListProgramsUseCase listProgramsUseCase;
    private final GetProgramDetailUseCase getProgramDetailUseCase;
    private final UpdateProgramUseCase updateProgramUseCase;
    private final DeactivateProgramUseCase deactivateProgramUseCase;
    private final ReactivateProgramUseCase reactivateProgramUseCase;

    public ProgramController(CreateProgramUseCase createProgramUseCase,
                             ListProgramsUseCase listProgramsUseCase,
                             GetProgramDetailUseCase getProgramDetailUseCase,
                             UpdateProgramUseCase updateProgramUseCase,
                             DeactivateProgramUseCase deactivateProgramUseCase,
                             ReactivateProgramUseCase reactivateProgramUseCase) {
        this.createProgramUseCase = createProgramUseCase;
        this.listProgramsUseCase = listProgramsUseCase;
        this.getProgramDetailUseCase = getProgramDetailUseCase;
        this.updateProgramUseCase = updateProgramUseCase;
        this.deactivateProgramUseCase = deactivateProgramUseCase;
        this.reactivateProgramUseCase = reactivateProgramUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ProgramResponseDto.ProgramDetailResponse> createProgram(
            @Valid @RequestBody ProgramRequestDto.CreateProgramRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        CreateProgramCommand command = new CreateProgramCommand(
                tenantId,
                request.name(),
                userId
        );

        Program program = createProgramUseCase.execute(command);

        ProgramResponseDto.ProgramDetailResponse response =
                ProgramResponseDto.ProgramDetailResponse.fromDomain(program);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<Page<ProgramResponseDto.ProgramSummaryResponse>> listPrograms(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        UUID tenantId = extractTenantId();

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));

        Page<ProgramResponseDto.ProgramSummaryResponse> result = listProgramsUseCase
                .execute(tenantId, pageable, status)
                .map(ProgramResponseDto.ProgramSummaryResponse::fromSummary);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{programId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ProgramResponseDto.ProgramDetailResponse> getProgramDetail(
            @PathVariable UUID programId) {

        UUID tenantId = extractTenantId();
        ProgramDetail detail = getProgramDetailUseCase.execute(tenantId, programId);

        return ResponseEntity.ok(ProgramResponseDto.ProgramDetailResponse.fromDetail(detail));
    }

    @PutMapping("/{programId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ProgramResponseDto.ProgramDetailResponse> updateProgram(
            @PathVariable UUID programId,
            @Valid @RequestBody ProgramRequestDto.UpdateProgramRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        UpdateProgramCommand command = new UpdateProgramCommand(
                tenantId, programId, request.name(), userId);

        Program program = updateProgramUseCase.execute(command);

        return ResponseEntity.ok(ProgramResponseDto.ProgramDetailResponse.fromDomain(program));
    }

    @PostMapping("/{programId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ProgramResponseDto.ProgramDetailResponse> deactivateProgram(
            @PathVariable UUID programId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        Program program = deactivateProgramUseCase.execute(tenantId, programId, userId);

        return ResponseEntity.ok(ProgramResponseDto.ProgramDetailResponse.fromDomain(program));
    }

    @PostMapping("/{programId}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<ProgramResponseDto.ProgramDetailResponse> reactivateProgram(
            @PathVariable UUID programId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        Program program = reactivateProgramUseCase.execute(tenantId, programId, userId);

        return ResponseEntity.ok(ProgramResponseDto.ProgramDetailResponse.fromDomain(program));
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
