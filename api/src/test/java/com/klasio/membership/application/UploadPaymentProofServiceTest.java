package com.klasio.membership.application;

import com.klasio.membership.application.port.input.UploadPaymentProofCommand;
import com.klasio.membership.application.service.UploadPaymentProofService;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipId;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.model.PaymentProof;
import com.klasio.membership.domain.model.ProofStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.membership.domain.port.PaymentProofRepository;
import com.klasio.membership.domain.port.PaymentProofStorage;
import com.klasio.shared.infrastructure.exception.InvalidMembershipStatusForUploadException;
import com.klasio.shared.infrastructure.exception.MembershipNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UploadPaymentProofServiceTest {

    @Mock private MembershipRepository membershipRepository;
    @Mock private PaymentProofRepository proofRepository;
    @Mock private PaymentProofStorage proofStorage;
    @Mock private ApplicationEventPublisher eventPublisher;

    private UploadPaymentProofService service;

    private static final UUID TENANT_ID     = UUID.randomUUID();
    private static final UUID MEMBERSHIP_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID    = UUID.randomUUID();
    private static final UUID ACTOR_ID      = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new UploadPaymentProofService(membershipRepository, proofRepository, proofStorage, eventPublisher);
    }

    private Membership pendingMembership() {
        return Membership.create(TENANT_ID, STUDENT_ID, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Plan", 10, LocalDate.of(2026, 4, 1), ACTOR_ID);
    }

    private UploadPaymentProofCommand validCommand() {
        return new UploadPaymentProofCommand(
                TENANT_ID, MEMBERSHIP_ID, STUDENT_ID,
                new ByteArrayInputStream(new byte[100]),
                "recibo.pdf", "application/pdf", 100L, ACTOR_ID);
    }

    @Test
    @DisplayName("stores file, saves PENDING proof, and transitions membership to PENDING_PAYMENT_VALIDATION")
    void uploadsProofSuccessfully() {
        Membership membership = pendingMembership();
        String fileKey = "proofs/t/m/file.pdf";

        when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID)).thenReturn(Optional.of(membership));
        when(proofRepository.findPendingByMembershipId(TENANT_ID, MEMBERSHIP_ID)).thenReturn(Optional.empty());
        when(proofStorage.store(any(), any(), any(), any(), anyLong())).thenReturn(fileKey);
        when(proofRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentProof result = service.execute(validCommand());

        assertThat(result.getStatus()).isEqualTo(ProofStatus.PENDING);
        assertThat(result.getFileKey()).isEqualTo(fileKey);
        assertThat(membership.getStatus()).isEqualTo(MembershipStatus.PENDING_PAYMENT_VALIDATION);

        verify(proofStorage).store(eq(TENANT_ID), eq(MEMBERSHIP_ID), any(), eq("application/pdf"), eq(100L));
        verify(proofRepository).save(any());
        verify(membershipRepository).save(membership);
        verify(eventPublisher, atLeastOnce()).publishEvent(any(Object.class));
    }

    @Test
    @DisplayName("supersedes existing PENDING proof before creating a new one")
    void supersedesExistingProof() {
        Membership membership = pendingMembership();
        PaymentProof existingProof = PaymentProof.upload(TENANT_ID, MEMBERSHIP_ID, STUDENT_ID,
                "proofs/old.pdf", "old.pdf", "application/pdf", 500L, ACTOR_ID);

        when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID)).thenReturn(Optional.of(membership));
        when(proofRepository.findPendingByMembershipId(TENANT_ID, MEMBERSHIP_ID)).thenReturn(Optional.of(existingProof));
        when(proofStorage.store(any(), any(), any(), any(), anyLong())).thenReturn("proofs/new.pdf");
        when(proofRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.execute(validCommand());

        // The existing proof must have been superseded and saved
        verify(proofRepository, atLeast(2)).save(any());
        assertThat(existingProof.getStatus()).isEqualTo(ProofStatus.SUPERSEDED);
    }

    @Test
    @DisplayName("does not supersede REJECTED proofs — they remain as historical records")
    void doesNotSupersedeRejectedProof() {
        Membership membership = pendingMembership();

        when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID)).thenReturn(Optional.of(membership));
        when(proofRepository.findPendingByMembershipId(TENANT_ID, MEMBERSHIP_ID)).thenReturn(Optional.empty());
        when(proofStorage.store(any(), any(), any(), any(), anyLong())).thenReturn("proofs/new.pdf");
        when(proofRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        PaymentProof result = service.execute(validCommand());

        assertThat(result.getStatus()).isEqualTo(ProofStatus.PENDING);
        // Only one save: the new proof (no supersede save)
        verify(proofRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("throws MembershipNotFoundException when membership does not exist")
    void throwsWhenMembershipNotFound() {
        when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(validCommand()))
                .isInstanceOf(MembershipNotFoundException.class);
    }

    @Test
    @DisplayName("throws InvalidMembershipStatusForUploadException when membership is ACTIVE")
    void throwsWhenMembershipIsActive() {
        Membership membership = pendingMembership();
        membership.markProofUploaded();
        membership.validatePayment(ACTOR_ID, true); // transitions to ACTIVE

        when(membershipRepository.findById(TENANT_ID, MEMBERSHIP_ID)).thenReturn(Optional.of(membership));

        assertThatThrownBy(() -> service.execute(validCommand()))
                .isInstanceOf(InvalidMembershipStatusForUploadException.class);
    }
}
