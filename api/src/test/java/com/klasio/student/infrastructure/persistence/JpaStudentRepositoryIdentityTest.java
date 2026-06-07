package com.klasio.student.infrastructure.persistence;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class JpaStudentRepositoryIdentityTest {

    @Test
    void existsByIdentityNumberInTenant_delegatesToSpringData() {
        SpringDataStudentRepository springData = mock(SpringDataStudentRepository.class);
        StudentMapper mapper = mock(StudentMapper.class);
        UUID tenantId = UUID.randomUUID();
        when(springData.existsByTenantIdAndIdentityNumber(tenantId, "123")).thenReturn(true);

        JpaStudentRepository repo = new JpaStudentRepository(springData, mapper);

        assertThat(repo.existsByIdentityNumberInTenant(tenantId, "123")).isTrue();
    }
}
