package com.klasio.membership.domain;

import com.klasio.membership.domain.event.PaymentProofApproved;
import com.klasio.membership.domain.event.PaymentProofRejected;
import com.klasio.membership.domain.event.PaymentProofUploaded;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.model.ProofStatus;
import com.klasio.shared.domain.DomainEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PaymentProofTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID ACTOR_ID = UUID.randomUUID();
    private static final String FILE_KEY = "proofs/t/m/file.pdf";
    private static final String FILE_NAME = "recibo.pdf";
    private static final String CONTENT_TYPE_PDF = "application/pdf";
    private static final long VALID_SIZE = 1_000_000L;

    private PaymentProof defaultProof() {
        return PaymentProof.upload(TENANT_ID, MEMBERSHIP_ID, STUDENT_ID,
                FILE_KEY, FILE_NAME, CONTENT_TYPE_PDF, VALID_SIZE, ACTOR_ID);
    }

    // ---- upload() ----

    @Nested
    @DisplayName("upload()")
    class Upload {

        @Test
        @DisplayName("creates proof in PENDING status with correct fields")
        void createsPendingProof() {
            PaymentProof proof = defaultProof();

            assertEquals(ProofStatus.PENDING, proof.getStatus());
            assertEquals(TENANT_ID, proof.getTenantId());
            assertEquals(MEMBERSHIP_ID, proof.getMembershipId());
            assertEquals(STUDENT_ID, proof.getStudentId());
            assertEquals(FILE_KEY, proof.getFileKey());
            assertEquals(FILE_NAME, proof.getOriginalFileName());
            assertEquals(CONTENT_TYPE_PDF, proof.getContentType());
            assertEquals(VALID_SIZE, proof.getFileSizeBytes());
            assertEquals(ACTOR_ID, proof.getCreatedBy());
            assertNotNull(proof.getId());
            assertNotNull(proof.getUploadedAt());
            assertNull(proof.getValidatedBy());
            assertNull(proof.getValidatedAt());
            assertNull(proof.getRejectionReason());
        }

        @Test
        @DisplayName("registers PaymentProofUploaded domain event")
        void registersUploadEvent() {
            PaymentProof proof = defaultProof();

            List<DomainEvent> events = proof.getDomainEvents();
            assertEquals(1, events.size());
            assertInstanceOf(PaymentProofUploaded.class, events.get(0));
            PaymentProofUploaded event = (PaymentProofUploaded) events.get(0);
            assertEquals(proof.getId().value(), event.proofId());
            assertEquals(TENANT_ID, event.tenantId());
            assertEquals(MEMBERSHIP_ID, event.membershipId());
            assertEquals(STUDENT_ID, event.studentId());
        }

        @Test
        @DisplayName("accepts JPG and PNG content types")
        void acceptsAllowedMimeTypes() {
            assertDoesNotThrow(() -> PaymentProof.upload(
                    TENANT_ID, MEMBERSHIP_ID, STUDENT_ID, FILE_KEY, "photo.jpg",
                    "image/jpeg", VALID_SIZE, ACTOR_ID));

            assertDoesNotThrow(() -> PaymentProof.upload(
                    TENANT_ID, MEMBERSHIP_ID, STUDENT_ID, FILE_KEY, "photo.png",
                    "image/png", VALID_SIZE, ACTOR_ID));
        }

        @Test
        @DisplayName("rejects unsupported MIME type")
        void rejectsUnsupportedMimeType() {
            assertThrows(IllegalArgumentException.class, () ->
                    PaymentProof.upload(TENANT_ID, MEMBERSHIP_ID, STUDENT_ID,
                            FILE_KEY, "file.docx", "application/msword", VALID_SIZE, ACTOR_ID));
        }

        @Test
        @DisplayName("rejects file exceeding 5 MB limit")
        void rejectsOversizeFile() {
            long oversizeBytes = 5_242_881L;
            assertThrows(IllegalArgumentException.class, () ->
                    PaymentProof.upload(TENANT_ID, MEMBERSHIP_ID, STUDENT_ID,
                            FILE_KEY, FILE_NAME, CONTENT_TYPE_PDF, oversizeBytes, ACTOR_ID));
        }

        @Test
        @DisplayName("rejects blank fileKey")
        void rejectsBlankFileKey() {
            assertThrows(IllegalArgumentException.class, () ->
                    PaymentProof.upload(TENANT_ID, MEMBERSHIP_ID, STUDENT_ID,
                            "  ", FILE_NAME, CONTENT_TYPE_PDF, VALID_SIZE, ACTOR_ID));
        }

        @Test
        @DisplayName("rejects null tenantId")
        void rejectsNullTenantId() {
            assertThrows(NullPointerException.class, () ->
                    PaymentProof.upload(null, MEMBERSHIP_ID, STUDENT_ID,
                            FILE_KEY, FILE_NAME, CONTENT_TYPE_PDF, VALID_SIZE, ACTOR_ID));
        }
    }

    // ---- approve() ----

    @Nested
    @DisplayName("approve()")
    class Approve {

        @Test
        @DisplayName("transitions PENDING proof to APPROVED and registers event")
        void approvesPendingProof() {
            PaymentProof proof = defaultProof();
            proof.clearDomainEvents();
            UUID admin = UUID.randomUUID();

            proof.approve(admin, true);

            assertEquals(ProofStatus.APPROVED, proof.getStatus());
            assertEquals(admin, proof.getValidatedBy());
            assertNotNull(proof.getValidatedAt());

            List<DomainEvent> events = proof.getDomainEvents();
            assertEquals(1, events.size());
            PaymentProofApproved event = (PaymentProofApproved) events.get(0);
            assertEquals(proof.getId().value(), event.proofId());
            assertEquals(admin, event.validatedBy());
            assertTrue(event.activateDirectly());
        }

        @Test
        @DisplayName("sets activateDirectly=false when delegating")
        void approvesWithDelegation() {
            PaymentProof proof = defaultProof();
            proof.clearDomainEvents();

            proof.approve(ACTOR_ID, false);

            PaymentProofApproved event = (PaymentProofApproved) proof.getDomainEvents().get(0);
            assertFalse(event.activateDirectly());
        }

        @Test
        @DisplayName("throws when trying to approve an already APPROVED proof")
        void throwsWhenAlreadyApproved() {
            PaymentProof proof = defaultProof();
            proof.approve(ACTOR_ID, true);

            assertThrows(IllegalStateException.class, () -> proof.approve(ACTOR_ID, true));
        }

        @Test
        @DisplayName("throws when trying to approve a REJECTED proof")
        void throwsWhenRejected() {
            PaymentProof proof = defaultProof();
            proof.reject("wrong amount", ACTOR_ID);

            assertThrows(IllegalStateException.class, () -> proof.approve(ACTOR_ID, true));
        }

        @Test
        @DisplayName("throws on null validatedBy")
        void throwsOnNullActor() {
            PaymentProof proof = defaultProof();
            assertThrows(NullPointerException.class, () -> proof.approve(null, true));
        }
    }

    // ---- reject() ----

    @Nested
    @DisplayName("reject()")
    class Reject {

        @Test
        @DisplayName("transitions PENDING proof to REJECTED with reason and registers event")
        void rejectsPendingProof() {
            PaymentProof proof = defaultProof();
            proof.clearDomainEvents();
            String reason = "El comprobante no muestra el monto pagado.";

            proof.reject(reason, ACTOR_ID);

            assertEquals(ProofStatus.REJECTED, proof.getStatus());
            assertEquals(reason, proof.getRejectionReason());
            assertEquals(ACTOR_ID, proof.getValidatedBy());
            assertNotNull(proof.getValidatedAt());

            List<DomainEvent> events = proof.getDomainEvents();
            assertEquals(1, events.size());
            PaymentProofRejected event = (PaymentProofRejected) events.get(0);
            assertEquals(proof.getId().value(), event.proofId());
            assertEquals(reason, event.rejectionReason());
        }

        @Test
        @DisplayName("throws when reason is blank")
        void throwsWhenReasonBlank() {
            PaymentProof proof = defaultProof();
            assertThrows(IllegalArgumentException.class, () ->
                    proof.reject("   ", ACTOR_ID));
        }

        @Test
        @DisplayName("throws when reason is null")
        void throwsWhenReasonNull() {
            PaymentProof proof = defaultProof();
            assertThrows(IllegalArgumentException.class, () ->
                    proof.reject(null, ACTOR_ID));
        }

        @Test
        @DisplayName("throws when trying to reject an already APPROVED proof")
        void throwsWhenApproved() {
            PaymentProof proof = defaultProof();
            proof.approve(ACTOR_ID, true);

            assertThrows(IllegalStateException.class, () ->
                    proof.reject("reason", ACTOR_ID));
        }
    }

    // ---- supersede() ----

    @Nested
    @DisplayName("supersede()")
    class Supersede {

        @Test
        @DisplayName("supersedes a PENDING proof")
        void supersedesPending() {
            PaymentProof proof = defaultProof();
            proof.supersede();
            assertEquals(ProofStatus.SUPERSEDED, proof.getStatus());
        }

        @Test
        @DisplayName("supersedes a REJECTED proof")
        void supersedessRejected() {
            PaymentProof proof = defaultProof();
            proof.reject("wrong", ACTOR_ID);
            proof.supersede();
            assertEquals(ProofStatus.SUPERSEDED, proof.getStatus());
        }

        @Test
        @DisplayName("throws when trying to supersede an APPROVED proof")
        void throwsWhenApproved() {
            PaymentProof proof = defaultProof();
            proof.approve(ACTOR_ID, true);
            assertThrows(IllegalStateException.class, proof::supersede);
        }

        @Test
        @DisplayName("no domain event is emitted on supersede")
        void noEventOnSupersede() {
            PaymentProof proof = defaultProof();
            proof.clearDomainEvents();
            proof.supersede();
            assertTrue(proof.getDomainEvents().isEmpty());
        }
    }

    // ---- clearDomainEvents() ----

    @Test
    @DisplayName("clearDomainEvents() empties the event list")
    void clearsDomainEvents() {
        PaymentProof proof = defaultProof();
        assertFalse(proof.getDomainEvents().isEmpty());
        proof.clearDomainEvents();
        assertTrue(proof.getDomainEvents().isEmpty());
    }
}
