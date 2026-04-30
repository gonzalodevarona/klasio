package com.klasio.attendance.application.service;

import com.klasio.attendance.application.dto.AttendanceStatsView;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository;
import com.klasio.attendance.domain.port.AttendanceRegistrationRepository.StatsProjection;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAttendanceStatsServiceTest {

    @Mock AttendanceRegistrationRepository registrationRepository;
    @InjectMocks GetAttendanceStatsService service;

    private static final UUID TENANT_ID  = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();

    @Test
    @DisplayName("returns correct stats from projection")
    void happyPath_returnsStats() {
        when(registrationRepository.computeStatsForStudent(TENANT_ID, STUDENT_ID))
                .thenReturn(new StatsProjection(8L, 2L, 1L, 3L, 16L));

        AttendanceStatsView result = service.execute(TENANT_ID, STUDENT_ID);

        assertThat(result.attended()).isEqualTo(8L);
        assertThat(result.cancelledByStudent()).isEqualTo(2L);
        assertThat(result.cancelledBySystem()).isEqualTo(1L);
        assertThat(result.absent()).isEqualTo(3L);
        assertThat(result.totalHoursConsumed()).isEqualTo(16L);
        assertThat(result.attendanceRatePercent()).isEqualTo(72); // 8/(8+3)*100 rounded
    }

    @Test
    @DisplayName("returns 0% rate when denominator is zero")
    void zeroDenominator_returnsZeroRate() {
        when(registrationRepository.computeStatsForStudent(TENANT_ID, STUDENT_ID))
                .thenReturn(new StatsProjection(0L, 5L, 0L, 0L, 0L));

        AttendanceStatsView result = service.execute(TENANT_ID, STUDENT_ID);

        assertThat(result.attendanceRatePercent()).isEqualTo(0);
    }

    @Test
    @DisplayName("100% rate when attended > 0 and absent = 0")
    void noAbsent_returns100Rate() {
        when(registrationRepository.computeStatsForStudent(TENANT_ID, STUDENT_ID))
                .thenReturn(new StatsProjection(5L, 0L, 0L, 0L, 10L));

        AttendanceStatsView result = service.execute(TENANT_ID, STUDENT_ID);

        assertThat(result.attendanceRatePercent()).isEqualTo(100);
    }
}
