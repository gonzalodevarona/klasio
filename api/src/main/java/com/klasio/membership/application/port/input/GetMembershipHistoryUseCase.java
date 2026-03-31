package com.klasio.membership.application.port.input;
import com.klasio.membership.application.dto.MembershipHistoryEntryDto;
import java.util.List;
import java.util.UUID;
public interface GetMembershipHistoryUseCase {
    List<MembershipHistoryEntryDto> execute(UUID tenantId, UUID studentId, UUID programId);
    String exportCsv(UUID tenantId, UUID studentId, UUID programId);
}
