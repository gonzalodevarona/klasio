package com.klasio.membership.application.port.input;
import com.klasio.membership.domain.model.Membership;
import com.klasio.membership.domain.model.MembershipStatus;
import org.springframework.data.domain.Page;
import java.util.UUID;
public interface ListMembershipsUseCase {
    Page<Membership> execute(UUID tenantId, UUID studentId, UUID programId, MembershipStatus status, int page, int size);
}
