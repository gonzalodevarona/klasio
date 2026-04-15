package com.klasio.membership.infrastructure.web;

import com.klasio.membership.application.port.input.ApproveProofCommand;
import com.klasio.membership.application.port.input.ApproveProofUseCase;
import com.klasio.membership.application.port.input.DelegatedMembershipDto;
import com.klasio.membership.application.port.input.GetMembershipUseCase;
import com.klasio.membership.application.port.input.GetPaymentProofUseCase;
import com.klasio.membership.application.port.input.GetProofDownloadUrlUseCase;
import com.klasio.membership.application.port.input.ListDelegatedMembershipsUseCase;
import com.klasio.membership.application.port.input.ListMembershipProofsUseCase;
import com.klasio.membership.application.port.input.ListPendingProofsUseCase;
import com.klasio.membership.application.port.input.PaymentProofDto;
import com.klasio.membership.application.port.input.ProofQueueItemDto;
import com.klasio.membership.application.port.input.RejectProofCommand;
import com.klasio.membership.application.port.input.RejectProofUseCase;
import com.klasio.membership.application.port.input.UploadPaymentProofCommand;
import com.klasio.membership.application.port.input.UploadPaymentProofUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.port.StudentIdPort;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PaymentProofController {

    private final UploadPaymentProofUseCase uploadProofUseCase;
    private final GetPaymentProofUseCase getProofUseCase;
    private final ListPendingProofsUseCase listPendingProofsUseCase;
    private final ListMembershipProofsUseCase listMembershipProofsUseCase;
    private final GetProofDownloadUrlUseCase getDownloadUrlUseCase;
    private final GetMembershipUseCase getMembershipUseCase;
    private final ApproveProofUseCase approveProofUseCase;
    private final RejectProofUseCase rejectProofUseCase;
    private final ListDelegatedMembershipsUseCase listDelegatedUseCase;
    private final StudentIdPort studentIdPort;

    public PaymentProofController(UploadPaymentProofUseCase uploadProofUseCase,
                                  GetPaymentProofUseCase getProofUseCase,
                                  ListPendingProofsUseCase listPendingProofsUseCase,
                                  ListMembershipProofsUseCase listMembershipProofsUseCase,
                                  GetProofDownloadUrlUseCase getDownloadUrlUseCase,
                                  GetMembershipUseCase getMembershipUseCase,
                                  ApproveProofUseCase approveProofUseCase,
                                  RejectProofUseCase rejectProofUseCase,
                                  ListDelegatedMembershipsUseCase listDelegatedUseCase,
                                  StudentIdPort studentIdPort) {
        this.uploadProofUseCase = uploadProofUseCase;
        this.getProofUseCase = getProofUseCase;
        this.listPendingProofsUseCase = listPendingProofsUseCase;
        this.listMembershipProofsUseCase = listMembershipProofsUseCase;
        this.getDownloadUrlUseCase = getDownloadUrlUseCase;
        this.getMembershipUseCase = getMembershipUseCase;
        this.approveProofUseCase = approveProofUseCase;
        this.rejectProofUseCase = rejectProofUseCase;
        this.listDelegatedUseCase = listDelegatedUseCase;
        this.studentIdPort = studentIdPort;
    }

    // POST /api/v1/memberships/{id}/payment-proof  (STUDENT)
    @PostMapping(value = "/memberships/{id}/payment-proof",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPERADMIN')")
    public ResponseEntity<PaymentProofResponseDto> uploadProof(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file) throws IOException {

        UUID actorId  = extractUserId();
        UUID tenantId = extractTenantId();
        UUID studentId = extractStudentId(tenantId, actorId);

        UploadPaymentProofCommand command = new UploadPaymentProofCommand(
                tenantId, id, studentId,
                file.getInputStream(),
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize(),
                actorId
        );

        PaymentProof proof = uploadProofUseCase.execute(command);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(PaymentProofResponseDto.from(proof));
    }

    // GET /api/v1/memberships/{id}/payment-proof  (STUDENT own / ADMIN)
    @GetMapping("/memberships/{id}/payment-proof")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<PaymentProofDto> getProof(@PathVariable UUID id) {
        UUID tenantId = extractTenantId();
        UUID actorId  = extractUserId();
        String role   = extractRole();

        PaymentProofDto dto = getProofUseCase.execute(tenantId, id, actorId, role);
        return ResponseEntity.ok(dto);
    }

    // GET /api/v1/memberships/{id}/payment-proofs  (timeline — all proofs for a membership)
    @GetMapping("/memberships/{id}/payment-proofs")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<List<PaymentProofResponseDto>> listMembershipProofs(@PathVariable UUID id) {
        UUID tenantId = extractTenantId();
        assertStudentOwnershipIfStudent(tenantId, id);

        List<PaymentProof> proofs = listMembershipProofsUseCase.execute(tenantId, id);
        List<PaymentProofResponseDto> response = proofs.stream()
                .map(PaymentProofResponseDto::from)
                .toList();
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/payment-proofs  (ADMIN, SUPERADMIN)
    @GetMapping("/payment-proofs")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<List<ProofQueueItemDto>> listPendingProofs() {
        UUID tenantId = extractTenantId();
        return ResponseEntity.ok(listPendingProofsUseCase.execute(tenantId));
    }

    // GET /api/v1/payment-proofs/{proofId}/download-url  (ADMIN, SUPERADMIN, MANAGER, STUDENT own)
    @GetMapping("/payment-proofs/{proofId}/download-url")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER', 'STUDENT')")
    public ResponseEntity<Map<String, String>> getDownloadUrl(@PathVariable UUID proofId) {
        UUID tenantId = extractTenantId();
        UUID actorId  = extractUserId();
        String role   = extractRole();
        String url = getDownloadUrlUseCase.execute(tenantId, proofId, actorId, role);
        return ResponseEntity.ok(Map.of("downloadUrl", url));
    }

    // POST /api/v1/payment-proofs/{proofId}/approve  (ADMIN, SUPERADMIN)
    @PostMapping("/payment-proofs/{proofId}/approve")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<PaymentProofResponseDto> approveProof(
            @PathVariable UUID proofId,
            @RequestBody ApproveProofRequest request) {

        UUID actorId  = extractUserId();
        UUID tenantId = extractTenantId();

        ApproveProofCommand command = new ApproveProofCommand(
                tenantId, proofId, actorId, request.activateDirectly());

        PaymentProof proof = approveProofUseCase.execute(command);
        return ResponseEntity.ok(PaymentProofResponseDto.from(proof));
    }

    // POST /api/v1/payment-proofs/{proofId}/reject  (ADMIN, SUPERADMIN)
    @PostMapping("/payment-proofs/{proofId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public ResponseEntity<PaymentProofResponseDto> rejectProof(
            @PathVariable UUID proofId,
            @RequestBody RejectProofRequest request) {

        UUID actorId  = extractUserId();
        UUID tenantId = extractTenantId();

        RejectProofCommand command = new RejectProofCommand(
                tenantId, proofId, actorId, request.rejectionReason());

        PaymentProof proof = rejectProofUseCase.execute(command);
        return ResponseEntity.ok(PaymentProofResponseDto.from(proof));
    }

    // GET /api/v1/programs/{programId}/delegated-memberships  (MANAGER/ADMIN/SUPERADMIN)
    @GetMapping("/programs/{programId}/delegated-memberships")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPERADMIN')")
    public ResponseEntity<List<DelegatedMembershipDto>> listDelegatedMemberships(
            @PathVariable UUID programId) {

        UUID tenantId = extractTenantId();
        return ResponseEntity.ok(listDelegatedUseCase.execute(tenantId, programId));
    }

    // GET /api/v1/payment-proofs/delegated  (MANAGER — programId inferred from JWT)
    @GetMapping("/payment-proofs/delegated")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN', 'SUPERADMIN')")
    public ResponseEntity<List<DelegatedMembershipDto>> listMyDelegatedMemberships() {
        UUID tenantId = extractTenantId();
        UUID programId = extractProgramId();
        return ResponseEntity.ok(listDelegatedUseCase.execute(tenantId, programId));
    }

    // ---- Request / Response DTOs ----

    public record ApproveProofRequest(boolean activateDirectly) {}

    public record RejectProofRequest(@NotBlank String rejectionReason) {}

    // ---- Helpers ----

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

    @SuppressWarnings("unchecked")
    private UUID extractProgramId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        String programId = (String) details.get("programId");
        return programId != null ? UUID.fromString(programId) : null;
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

    private UUID extractStudentId(UUID tenantId, UUID userId) {
        return studentIdPort.findStudentIdByUserId(tenantId, userId)
                .orElseThrow(() -> new IllegalStateException(
                        "No student record found for userId=" + userId + " in tenant=" + tenantId));
    }

    /**
     * If the caller is a STUDENT, verifies the membership belongs to them.
     */
    private void assertStudentOwnershipIfStudent(UUID tenantId, UUID membershipId) {
        if (!"STUDENT".equals(extractRole())) {
            return;
        }
        UUID userId = extractUserId();
        UUID studentId = extractStudentId(tenantId, userId);
        Membership membership = getMembershipUseCase.execute(tenantId, membershipId);
        if (!studentId.equals(membership.getStudentId())) {
            throw new AccessDeniedException("Students may only access their own payment proofs");
        }
    }

}
