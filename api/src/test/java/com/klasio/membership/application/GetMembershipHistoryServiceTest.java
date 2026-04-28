package com.klasio.membership.application;

import com.klasio.membership.application.dto.MembershipHistoryEntryDto;
import com.klasio.membership.application.service.GetMembershipHistoryService;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipId;
import com.klasio.membership.domain.model.MembershipStatus;
import com.klasio.membership.domain.port.MembershipRepository;
import com.klasio.program.domain.model.ProgramModality;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetMembershipHistoryServiceTest {

    @Mock private MembershipRepository membershipRepository;

    private GetMembershipHistoryService service;

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID STUDENT_ID = UUID.randomUUID();
    private static final UUID PROGRAM_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new GetMembershipHistoryService(membershipRepository);
    }

    @Nested
    @DisplayName("execute()")
    class Execute {

        @Test
        @DisplayName("returns history entries sorted by start date desc")
        void execute_returnsMappedHistory() {
            Membership m1 = buildMembership(LocalDate.of(2026, 2, 1), 10, 3, MembershipStatus.EXPIRED);
            Membership m2 = buildMembership(LocalDate.of(2026, 3, 1), 10, 8, MembershipStatus.ACTIVE);
            when(membershipRepository.findAllByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                    .thenReturn(List.of(m2, m1));

            List<MembershipHistoryEntryDto> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID);

            assertThat(result).hasSize(2);
            assertThat(result.get(0).availableHours()).isEqualTo(8);
            assertThat(result.get(1).availableHours()).isEqualTo(3);
            assertThat(result.get(0).consumedHours()).isEqualTo(2); // 10 - 8
            assertThat(result.get(1).consumedHours()).isEqualTo(7); // 10 - 3
            assertThat(result.get(0).modality()).isEqualTo("HOURS_BASED");
        }

        @Test
        @DisplayName("returns empty list when no memberships found")
        void execute_empty_returnsEmptyList() {
            when(membershipRepository.findAllByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                    .thenReturn(List.of());

            List<MembershipHistoryEntryDto> result = service.execute(TENANT_ID, STUDENT_ID, PROGRAM_ID);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("exportCsv()")
    class ExportCsv {

        @Test
        @DisplayName("produces CSV with header and data rows")
        void exportCsv_producesValidCsv() {
            Membership m = buildMembership(LocalDate.of(2026, 3, 1), 10, 8, MembershipStatus.ACTIVE);
            when(membershipRepository.findAllByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                    .thenReturn(List.of(m));

            String csv = service.exportCsv(TENANT_ID, STUDENT_ID, PROGRAM_ID);

            assertThat(csv).contains("id,modality,purchasedHours,consumedHours,availableHours,startDate,expirationDate,status");
            assertThat(csv).contains("ACTIVE");
            assertThat(csv).contains("2026-03-01");
            String[] lines = csv.split("\n");
            assertThat(lines).hasSize(2); // header + 1 data row
        }

        @Test
        @DisplayName("produces CSV with header only when no memberships")
        void exportCsv_empty_returnsHeaderOnly() {
            when(membershipRepository.findAllByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                    .thenReturn(List.of());

            String csv = service.exportCsv(TENANT_ID, STUDENT_ID, PROGRAM_ID);

            assertThat(csv).contains("id,modality,purchasedHours");
            String[] lines = csv.split("\n");
            assertThat(lines).hasSize(1);
        }

        @Test
        @DisplayName("renders UNLIMITED rows with dashes instead of null for hour columns")
        void exportCsv_unlimitedMembership_rendersHourColumnsAsDashes() {
            Membership m = buildUnlimitedMembership(LocalDate.of(2026, 4, 1), MembershipStatus.ACTIVE);
            when(membershipRepository.findAllByStudentIdAndProgramId(TENANT_ID, STUDENT_ID, PROGRAM_ID))
                    .thenReturn(List.of(m));

            String csv = service.exportCsv(TENANT_ID, STUDENT_ID, PROGRAM_ID);

            assertThat(csv).contains("UNLIMITED");
            assertThat(csv).contains("—");
            assertThat(csv).doesNotContain("null");
            String[] lines = csv.split("\n");
            // Skip header line (lines[0]); first data row is lines[1]
            assertThat(lines[1]).contains(",UNLIMITED,—,—,—,");
        }
    }

    private Membership buildMembership(LocalDate start, int purchased, int available, MembershipStatus status) {
        return Membership.reconstitute(
                MembershipId.generate(),
                TENANT_ID, STUDENT_ID,
                UUID.randomUUID(), PROGRAM_ID,
                UUID.randomUUID(), "Test Plan",
                ProgramModality.HOURS_BASED,
                purchased, available,
                start, start.withDayOfMonth(start.lengthOfMonth()),
                status,
                true, UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), Instant.now(),
                Instant.now(), UUID.randomUUID(),
                null, null
        );
    }

    private Membership buildUnlimitedMembership(LocalDate start, MembershipStatus status) {
        return Membership.reconstitute(
                MembershipId.generate(),
                TENANT_ID, STUDENT_ID,
                UUID.randomUUID(), PROGRAM_ID,
                UUID.randomUUID(), "Unlimited Plan",
                ProgramModality.UNLIMITED,
                null, null,
                start, start.withDayOfMonth(start.lengthOfMonth()),
                status,
                true, UUID.randomUUID(), Instant.now(),
                UUID.randomUUID(), Instant.now(),
                Instant.now(), UUID.randomUUID(),
                null, null
        );
    }
}
