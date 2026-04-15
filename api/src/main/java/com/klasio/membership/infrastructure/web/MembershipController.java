package com.klasio.membership.infrastructure.web;

import com.klasio.membership.application.dto.ActivateMembershipCommand;
import com.klasio.membership.application.dto.AdjustHoursCommand;
import com.klasio.membership.application.dto.CreateMembershipCommand;
import com.klasio.membership.application.dto.MembershipHistoryEntryDto;
import com.klasio.membership.application.dto.ValidatePaymentCommand;
import com.klasio.membership.application.dto.HourTransactionSummaryDto;
import com.klasio.membership.application.dto.CreateSelfMembershipCommand;
import com.klasio.membership.application.port.input.ActivateMembershipUseCase;
import com.klasio.membership.application.port.input.AdjustHoursUseCase;
import com.klasio.membership.application.dto.RenewMembershipCommand;
import com.klasio.membership.application.port.input.CreateMembershipUseCase;
import com.klasio.membership.application.port.input.CreateSelfMembershipUseCase;
import com.klasio.membership.application.port.input.GetActiveMembershipUseCase;
import com.klasio.membership.application.port.input.GetHourTransactionsUseCase;
import com.klasio.membership.application.port.input.GetMembershipHistoryUseCase;
import com.klasio.membership.application.port.input.GetMembershipUseCase;
import com.klasio.membership.application.port.input.ListMembershipsUseCase;
import com.klasio.membership.application.port.input.RenewMembershipUseCase;
import com.klasio.membership.application.port.input.UploadPaymentProofCommand;
import com.klasio.membership.application.port.input.UploadPaymentProofUseCase;
import com.klasio.membership.application.port.input.ValidatePaymentUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.ProgramNamePort;
import com.klasio.membership.domain.port.StudentIdPort;
import com.klasio.membership.domain.port.StudentNamePort;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class MembershipController {

    private final CreateMembershipUseCase createMembershipUseCase;
    private final CreateSelfMembershipUseCase createSelfMembershipUseCase;
    private final RenewMembershipUseCase renewMembershipUseCase;
    private final ValidatePaymentUseCase validatePaymentUseCase;
    private final ActivateMembershipUseCase activateMembershipUseCase;
    private final AdjustHoursUseCase adjustHoursUseCase;
    private final GetMembershipUseCase getMembershipUseCase;
    private final ListMembershipsUseCase listMembershipsUseCase;
    private final GetActiveMembershipUseCase getActiveMembershipUseCase;
    private final GetMembershipHistoryUseCase getMembershipHistoryUseCase;
    private final GetHourTransactionsUseCase getHourTransactionsUseCase;
    private final UploadPaymentProofUseCase uploadProofUseCase;
    private final StudentNamePort studentNamePort;
    private final ProgramNamePort programNamePort;
    private final StudentIdPort studentIdPort;

    public MembershipController(CreateMembershipUseCase createMembershipUseCase,
                                 CreateSelfMembershipUseCase createSelfMembershipUseCase,
                                 RenewMembershipUseCase renewMembershipUseCase,
                                 ValidatePaymentUseCase validatePaymentUseCase,
                                 ActivateMembershipUseCase activateMembershipUseCase,
                                 AdjustHoursUseCase adjustHoursUseCase,
                                 GetMembershipUseCase getMembershipUseCase,
                                 ListMembershipsUseCase listMembershipsUseCase,
                                 GetActiveMembershipUseCase getActiveMembershipUseCase,
                                 GetMembershipHistoryUseCase getMembershipHistoryUseCase,
                                 GetHourTransactionsUseCase getHourTransactionsUseCase,
                                 UploadPaymentProofUseCase uploadProofUseCase,
                                 StudentNamePort studentNamePort,
                                 ProgramNamePort programNamePort,
                                 StudentIdPort studentIdPort) {
        this.createMembershipUseCase = createMembershipUseCase;
        this.createSelfMembershipUseCase = createSelfMembershipUseCase;
        this.renewMembershipUseCase = renewMembershipUseCase;
        this.validatePaymentUseCase = validatePaymentUseCase;
        this.activateMembershipUseCase = activateMembershipUseCase;
        this.adjustHoursUseCase = adjustHoursUseCase;
        this.getMembershipUseCase = getMembershipUseCase;
        this.listMembershipsUseCase = listMembershipsUseCase;
        this.getActiveMembershipUseCase = getActiveMembershipUseCase;
        this.getMembershipHistoryUseCase = getMembershipHistoryUseCase;
        this.getHourTransactionsUseCase = getHourTransactionsUseCase;
        this.uploadProofUseCase = uploadProofUseCase;
        this.studentNamePort = studentNamePort;
        this.programNamePort = programNamePort;
        this.studentIdPort = studentIdPort;
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

    // GET /api/v1/me/memberships  (STUDENT — returns their own memberships)
    @GetMapping("/me/memberships")
    @PreAuthorize("hasRole('STUDENT')")
    public ResponseEntity<List<MembershipResponseDto.MembershipSummaryResponse>> getMyMemberships() {
        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        UUID studentId = studentIdPort.findStudentIdByUserId(tenantId, userId)
                .orElseThrow(() -> new IllegalStateException("No student profile found for this user"));

        Page<Membership> result = listMembershipsUseCase.execute(
                tenantId, studentId, null, null, 0, 50);

        return ResponseEntity.ok(result.map(MembershipResponseDto::toSummary).getContent());
    }

    // POST /api/v1/me/memberships  (STUDENT — self-service: payment proof required)
    @PostMapping(value = "/me/memberships", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> createSelfMembership(
            @RequestParam("planId") UUID planId,
            @RequestParam(value = "startDate", required = false) LocalDate startDate,
            @RequestParam("file") MultipartFile file) throws IOException {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();
        UUID studentId = studentIdPort.findStudentIdByUserId(tenantId, userId)
                .orElseThrow(() -> new IllegalStateException("No student profile found for this user"));

        LocalDate effectiveStartDate = startDate != null ? startDate : LocalDate.now();

        CreateSelfMembershipCommand command = new CreateSelfMembershipCommand(
                tenantId, studentId, planId, effectiveStartDate, userId);

        Membership membership = createSelfMembershipUseCase.execute(command);

        UploadPaymentProofCommand proofCommand = new UploadPaymentProofCommand(
                tenantId, membership.getId().value(), studentId,
                file.getInputStream(), file.getOriginalFilename(),
                file.getContentType(), file.getSize(), userId);
        uploadProofUseCase.execute(proofCommand);

        // Reload — uploadProofUseCase transitioned membership to PENDING_PAYMENT_VALIDATION
        Membership updated = getMembershipUseCase.execute(tenantId, membership.getId().value());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toDetailWithNames(updated, tenantId));
    }

    // POST /api/v1/me/memberships/{id}/renew  (STUDENT — reactivates same membership)
    @PostMapping(value = "/me/memberships/{id}/renew", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('STUDENT')")
    @Transactional
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> renewMembership(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();
        UUID studentId = studentIdPort.findStudentIdByUserId(tenantId, userId)
                .orElseThrow(() -> new IllegalStateException("No student profile found for this user"));

        // Verify ownership before renewing
        Membership source = getMembershipUseCase.execute(tenantId, id);
        assertStudentOwnershipIfStudent(tenantId, source.getStudentId());

        // Renew the same membership (domain validates EXPIRED/INACTIVE status)
        RenewMembershipCommand command = new RenewMembershipCommand(tenantId, id, userId);
        Membership renewed = renewMembershipUseCase.execute(command);

        // Upload proof against the same membership ID
        UploadPaymentProofCommand proofCommand = new UploadPaymentProofCommand(
                tenantId, renewed.getId().value(), studentId,
                file.getInputStream(), file.getOriginalFilename(),
                file.getContentType(), file.getSize(), userId);
        uploadProofUseCase.execute(proofCommand);

        // Reload — uploadProofUseCase transitioned membership to PENDING_PAYMENT_VALIDATION
        Membership updated = getMembershipUseCase.execute(tenantId, renewed.getId().value());
        return ResponseEntity.ok(toDetailWithNames(updated, tenantId));
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
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'STUDENT')")
    public ResponseEntity<MembershipResponseDto.MembershipDetailResponse> getMembership(
            @PathVariable UUID id) {

        UUID tenantId = extractTenantId();
        Membership membership = getMembershipUseCase.execute(tenantId, id);
        assertStudentOwnershipIfStudent(tenantId, membership.getStudentId());
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
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'PROFESSOR', 'STUDENT')")
    public ResponseEntity<org.springframework.data.domain.Page<HourTransactionSummaryDto>> getTransactions(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID tenantId = extractTenantId();
        Membership membership = getMembershipUseCase.execute(tenantId, id);
        assertStudentOwnershipIfStudent(tenantId, membership.getStudentId());
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

    /**
     * If the caller is a STUDENT, verifies the resolved studentId matches the membership's studentId.
     * Non-STUDENT roles skip this check entirely.
     */
    private void assertStudentOwnershipIfStudent(UUID tenantId, UUID membershipStudentId) {
        String role = extractRole();
        if (!"STUDENT".equals(role)) {
            return;
        }
        UUID userId = extractUserId();
        UUID studentId = studentIdPort.findStudentIdByUserId(tenantId, userId)
                .orElseThrow(() -> new IllegalStateException("No student profile found for this user"));
        if (!studentId.equals(membershipStudentId)) {
            throw new AccessDeniedException("Students may only access their own memberships");
        }
    }

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
