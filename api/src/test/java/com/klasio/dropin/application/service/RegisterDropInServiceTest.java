package com.klasio.dropin.application.service;

import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.ClassDetailsPort;
import com.klasio.attendance.domain.port.ClassDetailsPort.ClassRegistrationView;
import com.klasio.attendance.domain.port.ClassDetailsPort.ScheduleEntryView;
import com.klasio.attendance.domain.port.ClassSessionRepository;
import com.klasio.attendance.domain.port.ProfessorIdLookupPort;
import com.klasio.attendance.domain.model.ClassSession;
import com.klasio.attendance.domain.model.ClassSessionId;
import com.klasio.dropin.application.dto.RegisterDropInCommand;
import com.klasio.dropin.application.dto.RegisterDropInResult;
import com.klasio.dropin.domain.model.DropInAttendee;
import com.klasio.dropin.domain.model.DropInAttendeeId;
import com.klasio.dropin.domain.model.DropInPayment;
import com.klasio.dropin.domain.model.DropInPaymentId;
import com.klasio.dropin.domain.model.PaymentMethod;
import com.klasio.dropin.domain.port.DropInAttendancePort;
import com.klasio.dropin.domain.port.DropInAttendeeRepository;
import com.klasio.dropin.domain.port.DropInPaymentRepository;
import com.klasio.dropin.domain.port.DropInPriceLookupPort;
import com.klasio.shared.infrastructure.exception.DropInNotAvailableException;
import com.klasio.shared.infrastructure.exception.PhoneAlreadyExistsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.*;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RegisterDropInServiceTest {

    @Mock ClassDetailsPort classDetailsPort;
    @Mock ProfessorIdLookupPort professorIdLookupPort;
    @Mock ClassSessionRepository classSessionRepository;
    @Mock DropInAttendeeRepository attendeeRepo;
    @Mock DropInPaymentRepository paymentRepo;
    @Mock DropInPriceLookupPort priceLookup;
    @Mock DropInAttendancePort attendancePort;
    @Mock AttendanceRegistrationRepository registrationRepository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks RegisterDropInService service;

    private final UUID tenantId  = UUID.randomUUID();
    private final UUID classId   = UUID.randomUUID();
    private final UUID programId = UUID.randomUUID();
    private final UUID sessionId = UUID.randomUUID();
    private final UUID actorId   = UUID.randomUUID();

    // Use dynamic times so we are always inside the marking window during test execution.
    // Session started 10 minutes ago and ends 50 minutes from now (window = [-20..+10] rel. to session bounds).
    private static final java.time.ZoneId BOGOTA = java.time.ZoneId.of("America/Bogota");
    private final java.time.ZonedDateTime nowBogota = java.time.ZonedDateTime.now(BOGOTA);
    private final LocalDate date = nowBogota.toLocalDate();
    private final LocalTime start = nowBogota.minusMinutes(10).toLocalTime().withSecond(0).withNano(0);
    private final LocalTime end   = nowBogota.plusMinutes(50).toLocalTime().withSecond(0).withNano(0);

    private ClassRegistrationView classView;
    private ClassSession session;

    @BeforeEach
    void setup() {
        ScheduleEntryView entry = new ScheduleEntryView(date.getDayOfWeek(), null, start, end);
        classView = new ClassRegistrationView(
                classId, programId, UUID.randomUUID(), "BEGINNER", "ACTIVE", "RECURRING", 20,
                "Test Class", List.of(entry));

        session = mock(ClassSession.class);
        when(session.getId()).thenReturn(new ClassSessionId(sessionId));

        when(classDetailsPort.findForRegistration(any(), any())).thenReturn(Optional.of(classView));
        when(priceLookup.findPrice(any(), any())).thenReturn(Optional.of(new BigDecimal("25000")));
        when(classSessionRepository.findOrCreate(any(), any(), any(), any(), any(), any())).thenReturn(session);
        when(paymentRepo.findByAttendeeAndSession(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void execute_newAttendee_createsAttendeePaymentAndRegistration() {
        when(attendeeRepo.findByPhoneAndTenant(anyString(), any())).thenReturn(Optional.empty());
        DropInAttendee newAttendee = DropInAttendee.create(tenantId, "Ana García", "3001234567", actorId, Instant.now());
        when(attendeeRepo.save(any())).thenReturn(newAttendee);
        DropInPayment payment = mock(DropInPayment.class);
        when(payment.getId()).thenReturn(new DropInPaymentId(UUID.randomUUID()));
        when(payment.getDomainEvents()).thenReturn(Collections.emptyList());
        when(paymentRepo.save(any())).thenReturn(payment);
        UUID regId = UUID.randomUUID();
        when(attendancePort.recordPresent(any())).thenReturn(regId);

        RegisterDropInResult result = service.execute(newCmd(null, "Ana García", "3001234567"));

        assertThat(result.attendeeWasNew()).isTrue();
        verify(attendeeRepo, times(2)).save(any()); // early FK-safe persist + post-visit update
        verify(paymentRepo).save(any());
        verify(attendancePort).recordPresent(any());
    }

    @Test
    void execute_existingAttendeeById_usesExistingAttendee() {
        UUID existingId = UUID.randomUUID();
        DropInAttendee attendee = mock(DropInAttendee.class);
        when(attendee.getId()).thenReturn(new DropInAttendeeId(existingId));
        when(attendee.getDomainEvents()).thenReturn(Collections.emptyList());
        when(attendeeRepo.findByIdAndTenant(existingId, tenantId)).thenReturn(Optional.of(attendee));
        when(attendeeRepo.save(any())).thenReturn(attendee);
        DropInPayment payment = mock(DropInPayment.class);
        when(payment.getId()).thenReturn(new DropInPaymentId(UUID.randomUUID()));
        when(payment.getDomainEvents()).thenReturn(Collections.emptyList());
        when(paymentRepo.save(any())).thenReturn(payment);
        when(attendancePort.recordPresent(any())).thenReturn(UUID.randomUUID());

        RegisterDropInResult result = service.execute(newCmd(existingId, null, null));

        assertThat(result.attendeeWasNew()).isFalse();
    }

    @Test
    void execute_programHasNullDropInPrice_throwsDropInNotAvailable() {
        when(priceLookup.findPrice(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.execute(newCmd(null, "Ana", "3001234567")))
                .isInstanceOf(DropInNotAvailableException.class);
    }

    @Test
    void execute_phoneCollision_throwsPhoneAlreadyExists() {
        DropInAttendee existing = mock(DropInAttendee.class);
        when(existing.getId()).thenReturn(new DropInAttendeeId(UUID.randomUUID()));
        when(existing.getFullName()).thenReturn("María García");
        when(existing.getTotalVisits()).thenReturn(3);
        when(attendeeRepo.findByPhoneAndTenant("3001234567", tenantId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.execute(newCmd(null, "Other Name", "3001234567")))
                .isInstanceOf(PhoneAlreadyExistsException.class)
                .satisfies(e -> {
                    var ex = (PhoneAlreadyExistsException) e;
                    assertThat(ex.fullName()).isEqualTo("María García");
                    assertThat(ex.totalVisits()).isEqualTo(3);
                });
    }

    @Test
    void execute_idempotent_returnsExistingPaymentWithoutCreatingNewRows() {
        UUID existingPaymentId = UUID.randomUUID();
        UUID existingRegId = UUID.randomUUID();
        UUID existingAttendeeId = UUID.randomUUID();

        DropInAttendee attendee = mock(DropInAttendee.class);
        when(attendee.getId()).thenReturn(new DropInAttendeeId(existingAttendeeId));
        when(attendee.getTotalVisits()).thenReturn(2);
        when(attendeeRepo.findByIdAndTenant(existingAttendeeId, tenantId)).thenReturn(Optional.of(attendee));

        DropInPayment existingPayment = mock(DropInPayment.class);
        when(existingPayment.getId()).thenReturn(new DropInPaymentId(existingPaymentId));
        when(existingPayment.getAttendeeId()).thenReturn(existingAttendeeId);
        when(paymentRepo.findByAttendeeAndSession(existingAttendeeId, sessionId)).thenReturn(Optional.of(existingPayment));

        com.klasio.attendance.domain.model.AttendanceRegistration reg = mock(com.klasio.attendance.domain.model.AttendanceRegistration.class);
        when(reg.getId()).thenReturn(new com.klasio.attendance.domain.model.AttendanceRegistrationId(existingRegId));
        when(registrationRepository.findByDropInPaymentId(tenantId, existingPaymentId)).thenReturn(Optional.of(reg));

        RegisterDropInResult result = service.execute(newCmd(existingAttendeeId, null, null));

        assertThat(result.paymentId()).isEqualTo(existingPaymentId);
        assertThat(result.registrationId()).isEqualTo(existingRegId);
        verify(attendeeRepo, never()).save(any());
        verify(paymentRepo, never()).save(any());
        verify(attendancePort, never()).recordPresent(any());
    }

    private RegisterDropInCommand newCmd(UUID existingId, String name, String phone) {
        return new RegisterDropInCommand(
                tenantId, classId, date, start,
                existingId, name, phone,
                new BigDecimal("25000"), PaymentMethod.CASH,
                actorId, "ADMIN", programId);
    }
}
