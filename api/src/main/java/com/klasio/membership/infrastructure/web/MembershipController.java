package com.klasio.membership.infrastructure.web;

import com.klasio.membership.application.dto.ActivateMembershipCommand;
import com.klasio.membership.application.dto.AdjustHoursCommand;
import com.klasio.membership.application.dto.CreateMembershipCommand;
import com.klasio.membership.application.dto.MembershipHistoryEntryDto;
import com.klasio.membership.application.dto.ValidatePaymentCommand;
import com.klasio.membership.application.dto.HourTransactionSummaryDto;
import com.klasio.membership.application.port.input.ActivateMembershipUseCase;
import com.klasio.membership.application.port.input.AdjustHoursUseCase;
import com.klasio.membership.application.port.input.CreateMembershipUseCase;
import com.klasio.membership.application.port.input.GetActiveMembershipUseCase;
import com.klasio.membership.application.port.input.GetHourTransactionsUseCase;
import com.klasio.membership.application.port.input.GetMembershipHistoryUseCase;
import com.klasio.membership.application.port.input.GetMembershipUseCase;
import com.klasio.membership.application.port.input.ListMembershipsUseCase;
import com.klasio.membership.application.port.input.ValidatePaymentUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.ProgramNamePort;
import com.klasio.membership.domain.port.StudentNamePort;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MembershipController {

    private final CreateMembershipUseCase createMembershipUseCase;
    private final ValidatePaymentUseCase validatePaymentUseCase;
    private final ActivateMembershipUseCase activateMembershipUseCase;
    private final AdjustHoursUseCase adjustHoursUseCase;
    private final GetMembershipUseCase getMembershipUseCase;
    private final ListMembershipsUseCase listMembershipsUseCase;
    private final GetActiveMembershipUseCase getActiveMembershipUseCase;
    private final GetMembershipHistoryUseCase getMembershipHistoryUseCase;
    private final GetHourTransactionsUseCase getHourTransactionsUseCase;
    private final StudentNamePort studentNamePort;
    private final ProgramNamePort programNamePort;

    public MembershipController(CreateMembershipUseCase createMembershipUseCase,
                                 ValidatePaymentUseCase validatePaymentUseCase,
                                 ActivateMembershipUseCase activateMembershipUseCase,
                                 AdjustHoursUseCase adjustHoursUseCase,
                                 GetMembershipUseCase getMembershipUseCase,
                                 ListMembershipsUseCase listMembershipsUseCase,
                                 GetActiveMembershipUseCase getActiveMembershipUseCase,
                                 GetMembershipHistoryUseCase getMembershipHistoryUseCase,
                                 GetHourTransactionsUseCase getHourTransactionsUseCase,
                                 StudentNamePort studentNamePort,
                                 ProgramNamePort programNamePort) {
        this.createMembershipUseCase = createMembershipUseCase;
        this.validatePaymentUseCase = validatePaymentUseCase;
        this.activateMembershipUseCase = activateMembershipUseCase;
        this.adjustHoursUseCase = adjustHoursUseCase;
        this.getMembershipUseCase = getMembershipUseCase;
        this.listMembershipsUseCase = listMembershipsUseCase;
        this.getActiveMembershipUseCase = getActiveMembershipUseCase;
        this.getMembershipHistoryUseCase = getMembershipHistoryUseCase;
        this.getHourTransactionsUseCase = getHourTransactionsUseCase;
        this.studentNamePort = studentNamePort;
        this.programNamePort = programNamePort;
    }

    // GET /api/v1/memberships?studentId=&programId=&status=&page=0&size=20
    @GetMapping("/memberships")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<Page<MembershipResponseDto.MembershipSummaryResponse>> listMemberships(
            @RequestParam(required = false) UUID studentId,
            @RequestParam(required = false) UUID programId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = extractTenantId();
        MembershipStatus statusEnum = status != null ? MembershipStatus.valueOf(status) : null;

        Page<Membership> result = listMembershipsUseCase.execute(
                tenantId, studentId, programId, statusEnum, page, size);

        return ResponseEntity.ok(result.map(MembershipResponseDto::toSummary));
    }

    // POST /api/v1/memberships
    @PostMapping("/memberships")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> createMembership(
            @Valid @RequestBody MembershipRequestDto.CreateMembershipRequest request) {

        UUID actorId = extractUserId();
        UUID tenantId = extractTenantId();

        CreateMembershipCommand command = new CreateMembershipCommand(
                tenantId,
                request.studentId(),
                request.planId(),
                request.startDate(),
                request.paymentValidated(),
                request.activateDirectly(),
                actorId
        );

        Membership membership = createMembershipUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDetailWithNames(membership, tenantId));
    }

    // GET /api/v1/memberships/{id}
    @GetMapping("/memberships/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> getMembership(
            @PathVariable UUID id) {

        UUID tenantId = extractTenantId();
        Membership membership = getMembershipUseCase.execute(tenantId, id);
        return ResponseEntity.ok(toDetailWithNames(membership, tenantId));
    }

    // GET /api/v1/memberships/active?studentId=&programId=
    @GetMapping("/memberships/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR')")
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> getActiveMembership(
            @RequestParam UUID studentId,
            @RequestParam UUID programId) {

        UUID tenantId = extractTenantId();
        Membership membership = getActiveMembershipUseCase.execute(tenantId, studentId, programId);
        return ResponseEntity.ok(toDetailWithNames(membership, tenantId));
    }

    // PATCH /api/v1/memberships/{id}/validate-payment
    @PatchMapping("/memberships/{id}/validate-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> validatePayment(
            @PathVariable UUID id,
            @Valid @RequestBody MembershipRequestDto.ValidatePaymentRequest request) {

        UUID actorId = extractUserId();
        UUID tenantId = extractTenantId();

        ValidatePaymentCommand command = new ValidatePaymentCommand(
                tenantId, id, request.activateDirectly(), actorId);

        Membership membership = validatePaymentUseCase.execute(command);
        return ResponseEntity.ok(toDetailWithNames(membership, tenantId));
    }

    // PATCH /api/v1/memberships/{id}/activate
    @PatchMapping("/memberships/{id}/activate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> activateMembership(
            @PathVariable UUID id) {

        UUID actorId = extractUserId();
        UUID tenantId = extractTenantId();
        String role = extractRole();
        UUID managerProgramId = extractManagerProgramId();

        ActivateMembershipCommand command = new ActivateMembershipCommand(
                tenantId, id, actorId, role, managerProgramId);

        Membership membership = activateMembershipUseCase.execute(command);
        return ResponseEntity.ok(toDetailWithNames(membership, tenantId));
    }

    // POST /api/v1/memberships/{id}/adjust-hours
    @PostMapping("/memberships/{id}/adjust-hours")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> adjustHours(
            @PathVariable UUID id,
            @Valid @RequestBody MembershipRequestDto.AdjustHoursRequest request) {

        UUID actorId = extractUserId();
        UUID tenantId = extractTenantId();
        String role = extractRole();

        AdjustHoursCommand command = new AdjustHoursCommand(
                tenantId, id, request.delta(), request.reason(), actorId, role);

        Membership membership = adjustHoursUseCase.execute(command);
        return ResponseEntity.ok(toDetailWithNames(membership, tenantId));
    }

    // GET /api/v1/memberships/{id}/transactions?page=0&size=20
    @GetMapping("/memberships/{id}/transactions")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR')")
    public ResponseEntity<org.springframework.data.domain.Page<HourTransactionSummaryDto>> getTransactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = extractTenantId();
        return ResponseEntity.ok(getHourTransactionsUseCase.execute(tenantId, id, page, size));
    }

    // GET /api/v1/students/{studentId}/programs/{programId}/membership-history
    @GetMapping("/students/{studentId}/programs/{programId}/membership-history")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<?> getMembershipHistory(
            @PathVariable UUID studentId,
            @PathVariable UUID programId,
            @RequestHeader(value = HttpHeaders.ACCEPT, defaultValue = "application/json") String accept) {

        UUID tenantId = extractTenantId();

        if ("text/csv".equalsIgnoreCase(accept)) {
            String csv = getMembershipHistoryUseCase.exportCsv(tenantId, studentId, programId);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/csv"))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"membership-history.csv\"")
                    .body(csv);
        }

        List<MembershipHistoryEntryDto> history = getMembershipHistoryUseCase.execute(
                tenantId, studentId, programId);
        return ResponseEntity.ok(history);
    }

    // ---- Helpers ----

    private MembershipResponseDto.MembershipDetailResponse toDetailWithNames(
            Membership membership, UUID tenantId) {
        String studentName = studentNamePort
                .findFullName(membership.getStudentId(), tenantId)
                .orElse(membership.getStudentId().toString());
        String programName = programNamePort
                .findName(membership.getProgramId(), tenantId)
                .orElse(membership.getProgramId().toString());
        return MembershipResponseDto.toDetail(membership, studentName, programName);
    }

    @SuppressWarnings("unchecked")
    private UUID extractUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        return UUID.fromString((String) details.get("userId"));
    }

    private UUID extractTenantId() {
        String tenantId = TenantContextInterceptor.getCurrentTenant();
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }

    private String extractRole() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.replace("ROLE_", ""))
                .findFirst()
                .orElse("UNKNOWN");
    }

    @SuppressWarnings("unchecked")
    private UUID extractManagerProgramId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        String programId = (String) details.get("programId");
        if (programId != null) {
            try { return UUID.fromString(programId); } catch (IllegalArgumentException ignored) {}
        }
        return null;
    }
}
