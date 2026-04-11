package com.klasio.membership.domain.model;

import com.klasio.membership.domain.event.PaymentProofApproved;
import com.klasio.membership.domain.event.PaymentProofRejected;
import com.klasio.membership.domain.event.PaymentProofUploaded;
import com.klasio.shared.domain.DomainEvent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * PaymentProof aggregate root.
 * Represents a single uploaded proof document for a membership payment.
 * Zero Spring imports — pure Java domain model.
 */
public class PaymentProof {

    private static final long MAX_FILE_SIZE_BYTES = 5_242_880L; // 5 MB
    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("application/pdf", "image/jpeg", "image/png");

    private final PaymentProofId id;
    private final UUID tenantId;
    private final UUID membershipId;
    private final UUID studentId;

    private final String fileKey;
    private final String originalFileName;
    private final String contentType;
    private final long fileSizeBytes;

    private ProofStatus status;
    private final Instant uploadedAt;

    private UUID validatedBy;
    private Instant validatedAt;
    private String rejectionReason;

    private final UUID createdBy;
    private final Instant createdAt;

    private final List<DomainEvent> domainEvents = new ArrayList<>();

    private PaymentProof(PaymentProofId id,
                         UUID tenantId,
                         UUID membershipId,
                         UUID studentId,
                         String fileKey,
                         String originalFileName,
                         String contentType,
                         long fileSizeBytes,
                         ProofStatus status,
                         Instant uploadedAt,
                         UUID validatedBy,
                         Instant validatedAt,
                         String rejectionReason,
                         UUID createdBy,
                         Instant createdAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.membershipId = membershipId;
        this.studentId = studentId;
        this.fileKey = fileKey;
        this.originalFileName = originalFileName;
        this.contentType = contentType;
        this.fileSizeBytes = fileSizeBytes;
        this.status = status;
        this.uploadedAt = uploadedAt;
        this.validatedBy = validatedBy;
        this.validatedAt = validatedAt;
        this.rejectionReason = rejectionReason;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    // ---- Factory ----

    public static PaymentProof upload(UUID tenantId,
                                      UUID membershipId,
                                      UUID studentId,
                                      String fileKey,
                                      String originalFileName,
                                      String contentType,
                                      long fileSizeBytes,
                                      UUID uploadedBy) {
        Objects.requireNonNull(tenantId, "tenantId must not be null");
        Objects.requireNonNull(membershipId, "membershipId must not be null");
        Objects.requireNonNull(studentId, "studentId must not be null");
        Objects.requireNonNull(uploadedBy, "uploadedBy must not be null");

        if (fileKey == null || fileKey.isBlank()) {
            throw new IllegalArgumentException("fileKey must not be blank");
        }
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new IllegalArgumentException("originalFileName must not be blank");
        }
        if (!ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Invalid content type '%s'. Allowed: %s".formatted(contentType, ALLOWED_MIME_TYPES));
        }
        if (fileSizeBytes <= 0 || fileSizeBytes > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "File size %d exceeds maximum allowed %d bytes".formatted(fileSizeBytes, MAX_FILE_SIZE_BYTES));
        }

        Instant now = Instant.now();
        PaymentProofId proofId = PaymentProofId.generate();

        PaymentProof proof = new PaymentProof(
                proofId, tenantId, membershipId, studentId,
                fileKey, originalFileName, contentType, fileSizeBytes,
                ProofStatus.PENDING, now,
                null, null, null,
                uploadedBy, now
        );

        proof.domainEvents.add(new PaymentProofUploaded(
                proofId.value(), tenantId, membershipId, studentId, now));

        return proof;
    }

    public static PaymentProof reconstitute(PaymentProofId id,
                                            UUID tenantId,
                                            UUID membershipId,
                                            UUID studentId,
                                            String fileKey,
                                            String originalFileName,
                                            String contentType,
                                            long fileSizeBytes,
                                            ProofStatus status,
                                            Instant uploadedAt,
                                            UUID validatedBy,
                                            Instant validatedAt,
                                            String rejectionReason,
                                            UUID createdBy,
                                            Instant createdAt) {
        return new PaymentProof(id, tenantId, membershipId, studentId,
                fileKey, originalFileName, contentType, fileSizeBytes,
                status, uploadedAt, validatedBy, validatedAt, rejectionReason,
                createdBy, createdAt);
    }

    // ---- Domain methods ----

    public void approve(UUID validatedBy, boolean activateDirectly) {
        Objects.requireNonNull(validatedBy, "validatedBy must not be null");
        if (this.status != ProofStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot approve a proof that is not PENDING. Current status: " + this.status);
        }

        Instant now = Instant.now();
        this.status = ProofStatus.APPROVED;
        this.validatedBy = validatedBy;
        this.validatedAt = now;

        domainEvents.add(new PaymentProofApproved(
                id.value(), tenantId, membershipId, studentId,
                validatedBy, activateDirectly, now));
    }

    public void reject(String reason, UUID validatedBy) {
        Objects.requireNonNull(validatedBy, "validatedBy must not be null");
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("rejectionReason must not be blank");
        }
        if (this.status != ProofStatus.PENDING) {
            throw new IllegalStateException(
                    "Cannot reject a proof that is not PENDING. Current status: " + this.status);
        }

        Instant now = Instant.now();
        this.status = ProofStatus.REJECTED;
        this.rejectionReason = reason;
        this.validatedBy = validatedBy;
        this.validatedAt = now;

        domainEvents.add(new PaymentProofRejected(
                id.value(), tenantId, membershipId, studentId,
                reason, validatedBy, now));
    }

    public void supersede() {
        if (this.status == ProofStatus.APPROVED || this.status == ProofStatus.SUPERSEDED) {
            throw new IllegalStateException(
                    "Cannot supersede a proof with status: " + this.status);
        }
        this.status = ProofStatus.SUPERSEDED;
        // No domain event — internal state management only
    }

    // ---- Domain events ----

    public List<DomainEvent> getDomainEvents() {
        return Collections.unmodifiableList(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }

    // ---- Getters ----

    public PaymentProofId getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public UUID getMembershipId() { return membershipId; }
    public UUID getStudentId() { return studentId; }
    public String getFileKey() { return fileKey; }
    public String getOriginalFileName() { return originalFileName; }
    public String getContentType() { return contentType; }
    public long getFileSizeBytes() { return fileSizeBytes; }
    public ProofStatus getStatus() { return status; }
    public Instant getUploadedAt() { return uploadedAt; }
    public UUID getValidatedBy() { return validatedBy; }
    public Instant getValidatedAt() { return validatedAt; }
    public String getRejectionReason() { return rejectionReason; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
