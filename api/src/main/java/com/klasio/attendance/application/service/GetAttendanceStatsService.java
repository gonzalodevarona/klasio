package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.AttendanceStatsView;
import com.klasio.attendance.application.port.input.GetAttendanceStatsUseCase;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAttendanceStatsService implements GetAttendanceStatsUseCase {

    private final AttendanceRegistrationRepository registrationRepository;

    public GetAttendanceStatsService(AttendanceRegistrationRepository registrationRepository) {
        this.registrationRepository = registrationRepository;
    }

    @Override
    public AttendanceStatsView execute(UUID tenantId, UUID studentId) {
        AttendanceRegistrationRepository.StatsProjection p =
                registrationRepository.computeStatsForStudent(tenantId, studentId);

        long denominator = p.attended() + p.absent();
        int rate = denominator == 0
                ? 0
                : (int) (p.attended() * 100L / denominator);

        return new AttendanceStatsView(
                p.attended(),
                p.cancelledByStudent(),
                p.cancelledBySystem(),
                p.absent(),
                p.totalHoursConsumed(),
                rate
        );
    }
}
