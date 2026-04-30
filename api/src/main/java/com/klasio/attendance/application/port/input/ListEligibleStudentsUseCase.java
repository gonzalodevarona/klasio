package com.klasio.attendance.application.port.input;

import com.klasio.attendance.domain.port.EligibleStudentLookupPort.EligibleStudentView;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface ListEligibleStudentsUseCase {
    List<EligibleStudentView> execute(UUID tenantId,
                                      UUID classId,
                                      LocalDate sessionDate,
                                      LocalTime startTime,
                                      String nameFilter,
                                      String levelFilter,
                                      String role,
                                      UUID actorUserId,
                                      UUID programIdFromJwt);
}
