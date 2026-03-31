package com.klasio.membership.application.service;

import com.klasio.membership.application.dto.MembershipHistoryEntryDto;
import com.klasio.membership.application.port.input.GetMembershipHistoryUseCase;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.port.MembershipRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetMembershipHistoryService implements GetMembershipHistoryUseCase {

    private final MembershipRepository membershipRepository;

    public GetMembershipHistoryService(MembershipRepository membershipRepository) {
        this.membershipRepository = membershipRepository;
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public List<MembershipHistoryEntryDto> execute(UUID tenantId, UUID studentId, UUID programId) {
        List<Membership> memberships = membershipRepository
                .findAllByStudentIdAndProgramId(tenantId, studentId, programId);

        return memberships.stream()
                .map(m -> new MembershipHistoryEntryDto(
                        m.getId().value(),
                        m.getPurchasedHours(),
                        m.getPurchasedHours() - m.getAvailableHours(),
                        m.getAvailableHours(),
                        m.getStartDate(),
                        m.getExpirationDate(),
                        m.getStatus(),
                        m.getActivatedAt()
                ))
                .toList();
    }

    @Override
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN')")
    public String exportCsv(UUID tenantId, UUID studentId, UUID programId) {
        List<MembershipHistoryEntryDto> history = execute(tenantId, studentId, programId);

        StringBuilder csv = new StringBuilder();
        csv.append("id,purchasedHours,consumedHours,availableHours,startDate,expirationDate,status,activatedAt\n");

        for (MembershipHistoryEntryDto entry : history) {
            csv.append(entry.id()).append(',')
               .append(entry.purchasedHours()).append(',')
               .append(entry.consumedHours()).append(',')
               .append(entry.availableHours()).append(',')
               .append(entry.startDate()).append(',')
               .append(entry.expirationDate()).append(',')
               .append(entry.status()).append(',')
               .append(entry.activatedAt() != null ? entry.activatedAt() : "")
               .append('\n');
        }

        return csv.toString();
    }
}
