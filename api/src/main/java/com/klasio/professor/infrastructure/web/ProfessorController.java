package com.klasio.professor.infrastructure.web;

import com.klasio.professor.application.dto.CreateProfessorCommand;
import com.klasio.professor.application.dto.ProfessorDetail;
import com.klasio.professor.application.dto.ProfessorSummary;
import com.klasio.professor.application.dto.UpdateProfessorCommand;
import com.klasio.professor.application.port.input.CreateProfessorUseCase;
import com.klasio.professor.application.port.input.DeactivateProfessorUseCase;
import com.klasio.professor.application.port.input.GetProfessorDetailUseCase;
import com.klasio.professor.application.port.input.ListProfessorsUseCase;
import com.klasio.professor.application.port.input.ReactivateProfessorUseCase;
import com.klasio.professor.application.port.input.UpdateProfessorUseCase;
import com.klasio.shared.infrastructure.exception.ProfessorNotFoundException;
import jakarta.persistence.EntityManager;
import com.klasio.professor.domain.model.Professor;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/professors")
public class ProfessorController {

    private final CreateProfessorUseCase createProfessorUseCase;
    private final ListProfessorsUseCase listProfessorsUseCase;
    private final GetProfessorDetailUseCase getProfessorDetailUseCase;
    private final UpdateProfessorUseCase updateProfessorUseCase;
    private final DeactivateProfessorUseCase deactivateProfessorUseCase;
    private final ReactivateProfessorUseCase reactivateProfessorUseCase;
    private final EntityManager em;

    public ProfessorController(CreateProfessorUseCase createProfessorUseCase,
                               ListProfessorsUseCase listProfessorsUseCase,
                               GetProfessorDetailUseCase getProfessorDetailUseCase,
                               UpdateProfessorUseCase updateProfessorUseCase,
                               DeactivateProfessorUseCase deactivateProfessorUseCase,
                               ReactivateProfessorUseCase reactivateProfessorUseCase,
                               EntityManager em) {
        this.createProfessorUseCase = createProfessorUseCase;
        this.listProfessorsUseCase = listProfessorsUseCase;
        this.getProfessorDetailUseCase = getProfessorDetailUseCase;
        this.updateProfessorUseCase = updateProfessorUseCase;
        this.deactivateProfessorUseCase = deactivateProfessorUseCase;
        this.reactivateProfessorUseCase = reactivateProfessorUseCase;
        this.em = em;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ProfessorResponseDto.ProfessorDetailResponse> createProfessor(
            @Valid @RequestBody ProfessorRequestDto.CreateProfessorRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        CreateProfessorCommand command = new CreateProfessorCommand(
                tenantId,
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phoneNumber(),
                request.identityDocumentType(),
                request.identityNumber(),
                userId
        );

        Professor professor = createProfessorUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ProfessorResponseDto.ProfessorDetailResponse.fromDomain(professor));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('PROFESSOR')")
    public ResponseEntity<ProfessorResponseDto.ProfessorDetailResponse> getMyProfile() {
        UUID tenantId = extractTenantId();
        UUID userId   = extractUserId();

        // Resolve email from users table, then find professor by tenant + email
        @SuppressWarnings("unchecked")
        java.util.List<String> emails = em.createNativeQuery(
                        "SELECT email FROM users WHERE id = :userId")
                .setParameter("userId", userId)
                .getResultList();

        if (emails.isEmpty()) {
            throw new ProfessorNotFoundException("No user found for id: " + userId);
        }

        @SuppressWarnings("unchecked")
        java.util.List<UUID> ids = em.createNativeQuery(
                        "SELECT id FROM professors WHERE tenant_id = :tenantId AND email = :email")
                .setParameter("tenantId", tenantId)
                .setParameter("email", emails.get(0))
                .getResultList();

        if (ids.isEmpty()) {
            throw new ProfessorNotFoundException("No professor profile found for user: " + userId);
        }

        ProfessorDetail detail = getProfessorDetailUseCase.execute(tenantId, ids.get(0));
        return ResponseEntity.ok(ProfessorResponseDto.ProfessorDetailResponse.fromDetail(detail));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<Page<ProfessorResponseDto.ProfessorSummaryResponse>> listProfessors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status) {

        UUID tenantId = extractTenantId();

        Page<ProfessorResponseDto.ProfessorSummaryResponse> result = listProfessorsUseCase
                .execute(tenantId, PageRequest.of(page, size), status)
                .map(ProfessorResponseDto.ProfessorSummaryResponse::fromSummary);

        return ResponseEntity.ok(result);
    }

    @GetMapping("/{professorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ProfessorResponseDto.ProfessorDetailResponse> getProfessorDetail(
            @PathVariable UUID professorId) {

        UUID tenantId = extractTenantId();
        ProfessorDetail detail = getProfessorDetailUseCase.execute(tenantId, professorId);

        return ResponseEntity.ok(ProfessorResponseDto.ProfessorDetailResponse.fromDetail(detail));
    }

    @PutMapping("/{professorId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ProfessorResponseDto.ProfessorDetailResponse> updateProfessor(
            @PathVariable UUID professorId,
            @Valid @RequestBody ProfessorRequestDto.UpdateProfessorRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        UpdateProfessorCommand command = new UpdateProfessorCommand(
                tenantId, professorId, request.firstName(), request.lastName(), request.email(),
                request.phoneNumber(), request.identityDocumentType(), request.identityNumber(), userId);

        Professor professor = updateProfessorUseCase.execute(command);

        return ResponseEntity.ok(ProfessorResponseDto.ProfessorDetailResponse.fromDomain(professor));
    }

    @PostMapping("/{professorId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ProfessorResponseDto.ProfessorDetailResponse> deactivateProfessor(
            @PathVariable UUID professorId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        Professor professor = deactivateProfessorUseCase.execute(tenantId, professorId, userId);

        return ResponseEntity.ok(ProfessorResponseDto.ProfessorDetailResponse.fromDomain(professor));
    }

    @PostMapping("/{professorId}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ProfessorResponseDto.ProfessorDetailResponse> reactivateProfessor(
            @PathVariable UUID professorId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        Professor professor = reactivateProfessorUseCase.execute(tenantId, professorId, userId);

        return ResponseEntity.ok(ProfessorResponseDto.ProfessorDetailResponse.fromDomain(professor));
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
