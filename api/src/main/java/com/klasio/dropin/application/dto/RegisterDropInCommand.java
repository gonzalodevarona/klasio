package com.klasio.dropin.application.dto;

import com.klasio.dropin.domain.model.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public record RegisterDropInCommand(
    UUID tenantId,
    UUID classId,
    LocalDate sessionDate,
    LocalTime startTime,
    UUID existingAttendeeId,
    String newAttendeeFullName,
    String newAttendeePhone,
    BigDecimal amount,
    PaymentMethod paymentMethod,
    UUID actorUserId,
    String actorRole,
    UUID programIdFromJwt
) {}
