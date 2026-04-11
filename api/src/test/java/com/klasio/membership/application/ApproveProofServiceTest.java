package com.klasio.membership.application;

import com.klasio.membership.application.port.input.ApproveProofCommand;
import com.klasio.membership.application.service.ApproveProofService;
import com.klasio.membership.domain.model.DelegationReminder;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipId;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.model.PaymentProofId;
import com.klasio.membership.domain.model.ProofStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.membership.infrastructure.persistence.DelegationReminderJpaAdapter;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import com.klasio.shared.infrastructure.exception.PaymentProofNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
class ApproveProofServiceTest {

    @Mock private PaymentProofRepository proofRepository;
    @Mock private MembershipRepository membershipRepository;
    @Mock private DelegationReminderJpaAdapter delegationReminderAdapter;
    @Mock private ApplicationEventPublisher eventPublisher;

    private ApproveProofService service;

    private static final UUID TENANT_ID     = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID    = UUID.randomUUID();
    private static final UUID ADMIN_ID      = UUID.randomUUID();
    private static final UUID PROOF_ID      = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ApproveProofService(proofRepository, membershipRepository,
                delegationReminderAdapter, eventPublisher);
    }

    private PaymentProof pendingProof() {
        return PaymentProof.upload(TENANT_ID, MEMBERSHIP_ID, STUDENT_ID,
                "proofs/t/m/file.pdf", "recibo.pdf", "application/pdf", 100L, STUDENT_ID);
    }

    private Membership pendingPaymentMembership() {
        Membership m = Membership.create(TENANT_ID, STUDENT_ID, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Plan", 10, LocalDate.of(2026, 4, 1), ADMIN_ID);
        m.markProofUploaded(); // PENDING_PAYMENT → PENDING_PAYMENT_VALIDATION
        m.clearDomainEvents();
        return m;
    }

    @Test
    @DisplayName("approve with activateDirectly=true transitions proof to APPROVED and membership to ACTIVE")
    void approvesDirectly() {
        PaymentProof proof = pendingProof();
        Membership membership = pendingPaymentMembership();

        when(proofRepository.findById(TENANT_ID, PaymentProofId.of(PROOF_ID)))
                .thenReturn(Optional.of(proof));
        when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                .thenReturn(Optional.of(membership));
        when(proofRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(membershipRepository).save(any());

        ApproveProofCommand command = new ApproveProofCommand(TENANT_ID, PROOF_ID, ADMIN_ID, true);
        PaymentProof result = service.execute(command);

        assertThat(result.getStatus()).isEqualTo(ProofStatus.APPROVED);
        assertThat(membership.getStatus().name()).isEqualTo("ACTIVE");
        verify(eventPublisher, atLeastOnce()).publishEvent(any());
        verify(delegationReminderAdapter, never()).save(any());
    }

    @Test
    @DisplayName("approve with activateDirectly=false creates DelegationReminder and transitions to PENDING_MANAGER_ACTIVATION")
    void approvesWithDelegation() {
        PaymentProof proof = pendingProof();
        Membership membership = pendingPaymentMembership();

        when(proofRepository.findById(TENANT_ID, PaymentProofId.of(PROOF_ID)))
                .thenReturn(Optional.of(proof));
        when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                .thenReturn(Optional.of(membership));
        when(proofRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(membershipRepository).save(any());
        when(delegationReminderAdapter.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ApproveProofCommand command = new ApproveProofCommand(TENANT_ID, PROOF_ID, ADMIN_ID, false);
        service.execute(command);

        assertThat(membership.getStatus().name()).isEqualTo("PENDING_MANAGER_ACTIVATION");
        verify(delegationReminderAdapter).save(any(DelegationReminder.class));
    }

    @Test
    @DisplayName("throws PaymentProofNotFoundException when proof does not exist")
    void throwsWhenProofNotFound() {
        when(proofRepository.findById(TENANT_ID, PaymentProofId.of(PROOF_ID)))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new ApproveProofCommand(TENANT_ID, PROOF_ID, ADMIN_ID, true)))
                .isInstanceOf(PaymentProofNotFoundException.class);
    }

    @Test
    @DisplayName("throws MembershipNotFoundException when membership does not exist")
    void throwsWhenMembershipNotFound() {
        PaymentProof proof = pendingProof();

        when(proofRepository.findById(TENANT_ID, PaymentProofId.of(PROOF_ID)))
                .thenReturn(Optional.of(proof));
        when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new ApproveProofCommand(TENANT_ID, PROOF_ID, ADMIN_ID, true)))
                .isInstanceOf(MembershipNotFoundException.class);
    }
}
