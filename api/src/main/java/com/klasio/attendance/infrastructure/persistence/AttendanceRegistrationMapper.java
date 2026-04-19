package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.model.AttendanceRegistration;
import com.klasio.attendance.domain.model.AttendanceRegistrationId;
import com.klasio.attendance.domain.model.AttendanceRegistrationStatus;
import org.springframework.stereotype.Component;

@Component
public class AttendanceRegistrationMapper {

    public AttendanceRegistration toDomain(AttendanceRegistrationJpaEntity entity) {
        return AttendanceRegistration.reconstitute(
                AttendanceRegistrationId.of(entity.getId()),
                entity.getTenantId(),
                entity.getSessionId(),
                entity.getClassId(),
                entity.getStudentId(),
                entity.getEnrollmentId(),
                entity.getMembershipId(),
                entity.getLevelAtRegistration(),
                entity.getIntendedHours(),
                AttendanceRegistrationStatus.valueOf(entity.getStatus()),
                entity.getSessionDate(),
                entity.getSessionStartTime(),
                entity.getSessionEndTime(),
                entity.getCancelledAt(),
                entity.getCancelledBy(),
                entity.getCancellationReason(),
                entity.getMarkedAt(),
                entity.getMarkedBy(),
                entity.getCorrectedAt(),
                entity.getCorrectedBy(),
                entity.getCorrectionReason(),
                entity.getCreatedAt(),
                entity.getCreatedBy(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy()
        );
    }

    public AttendanceRegistrationJpaEntity toEntity(AttendanceRegistration registration) {
        AttendanceRegistrationJpaEntity entity = new AttendanceRegistrationJpaEntity();
        entity.setId(registration.getId().value());
        entity.setTenantId(registration.getTenantId());
        entity.setSessionId(registration.getSessionId());
        entity.setClassId(registration.getClassId());
        entity.setStudentId(registration.getStudentId());
        entity.setEnrollmentId(registration.getEnrollmentId());
        entity.setMembershipId(registration.getMembershipId());
        entity.setLevelAtRegistration(registration.getLevelAtRegistration());
        entity.setIntendedHours(registration.getIntendedHours());
        entity.setStatus(registration.getStatus().name());
        entity.setSessionDate(registration.getSessionDate());
        entity.setSessionStartTime(registration.getSessionStartTime());
        entity.setSessionEndTime(registration.getSessionEndTime());
        entity.setCancelledAt(registration.getCancelledAt());
        entity.setCancelledBy(registration.getCancelledBy());
        entity.setCancellationReason(registration.getCancellationReason());
        entity.setMarkedAt(registration.getMarkedAt());
        entity.setMarkedBy(registration.getMarkedBy());
        entity.setCorrectedAt(registration.getCorrectedAt());
        entity.setCorrectedBy(registration.getCorrectedBy());
        entity.setCorrectionReason(registration.getCorrectionReason());
        entity.setCreatedAt(registration.getCreatedAt());
        entity.setCreatedBy(registration.getCreatedBy());
        entity.setUpdatedAt(registration.getUpdatedAt());
        entity.setUpdatedBy(registration.getUpdatedBy());
        return entity;
    }
}
