package com.klasio.membership.application;

import com.klasio.membership.application.port.input.RejectProofCommand;
import com.klasio.membership.application.service.RejectProofService;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.model.PaymentProofId;
import com.klasio.membership.domain.model.ProofStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.program.domain.model.ProgramModality;
import com.klasio.shared.infrastructure.exception.PaymentProofNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RejectProofServiceTest {

    @Mock private PaymentProofRepository proofRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private RejectProofService service;

    private static final UUID TENANT_ID     = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID PROOF_ID      = UUID.randomUUID();
    private static final UUID ADMIN_ID      = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new RejectProofService(proofRepository, membershipRepository, eventPublisher);
    }

    /** Proof whose membershipId is MEMBERSHIP_ID. */
    private PaymentProof pendingProof() {
        return PaymentProof.upload(TENANT_ID, MEMBERSHIP_ID, UUID.randomUUID(),
                "proofs/t/m/f.pdf", "recibo.pdf", "application/pdf", 100L, UUID.randomUUID());
    }

    /** Membership in PENDING_PAYMENT_VALIDATION (proof already uploaded). */
    private Membership membershipInValidation() {
        Membership m = Membership.create(TENANT_ID, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), UUID.randomUUID(), "Plan", 10, ProgramModality.HOURS_BASED,
                LocalDate.of(2026, 4, 1), ADMIN_ID);
        m.markProofUploaded();
        m.clearDomainEvents();
        return m;
    }

    @Test
    @DisplayName("transitions proof to REJECTED and reverts membership to PENDING_PAYMENT")
    void rejectsProofAndRevertsMembers() {
        PaymentProof proof = pendingProof();
        Membership membership = membershipInValidation();
        String reason = "El monto no coincide.";

        when(proofRepository.findById(TENANT_ID, PaymentProofId.of(PROOF_ID)))
                .thenReturn(Optional.of(proof));
        when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                .thenReturn(Optional.of(membership));
        when(proofRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(membershipRepository).save(any());

        PaymentProof result = service.execute(new RejectProofCommand(TENANT_ID, PROOF_ID, ADMIN_ID, reason));

        assertThat(result.getStatus()).isEqualTo(ProofStatus.REJECTED);
        assertThat(result.getRejectionReason()).isEqualTo(reason);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PENDING_PAYMENT);

        verify(membershipRepository).save(membership);
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
    }

    @Test
    @DisplayName("throws when rejection reason is blank")
    void throwsWhenReasonBlank() {
        PaymentProof proof = pendingProof();
        when(proofRepository.findById(TENANT_ID, PaymentProofId.of(PROOF_ID)))
                .thenReturn(Optional.of(proof));

        assertThatThrownBy(() ->
                service.execute(new RejectProofCommand(TENANT_ID, PROOF_ID, ADMIN_ID, "  ")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("throws PaymentProofNotFoundException when proof does not exist")
    void throwsWhenProofNotFound() {
        when(proofRepository.findById(TENANT_ID, PaymentProofId.of(PROOF_ID)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                service.execute(new RejectProofCommand(TENANT_ID, PROOF_ID, ADMIN_ID, "reason")))
                .isInstanceOf(PaymentProofNotFoundException.class);
    }
}
