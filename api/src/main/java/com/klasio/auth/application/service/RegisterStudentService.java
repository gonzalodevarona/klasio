package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.RegisterStudentCommand;
import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.domain.exception.SelfRegistrationConflictException;
import com.klasio.auth.domain.exception.SelfRegistrationDisabledException;
import com.klasio.shared.domain.model.IdentityDocumentType;
import com.klasio.shared.infrastructure.exception.StudentEmailAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.StudentIdentityNumberAlreadyExistsException;
import com.klasio.shared.infrastructure.exception.TenantNotFoundException;
import com.klasio.student.application.dto.CreateStudentCommand;
import com.klasio.student.application.port.input.CreateStudentUseCase;
import com.klasio.student.domain.model.BloodType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class RegisterStudentService {

    private static final UUID SYSTEM_ACTOR = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final TenantResolverPort tenantResolverPort;
    private final CreateStudentUseCase createStudentUseCase;

    public RegisterStudentService(TenantResolverPort tenantResolverPort,
                                  CreateStudentUseCase createStudentUseCase) {
        this.tenantResolverPort = tenantResolverPort;
        this.createStudentUseCase = createStudentUseCase;
    }

    @Transactional
    public void register(RegisterStudentCommand command) {
        UUID tenantId = tenantResolverPort.resolveTenantIdBySlug(command.tenantSlug())
                .orElseThrow(() -> new TenantNotFoundException(
                        "Tenant '%s' not found".formatted(command.tenantSlug())));

        if (!tenantResolverPort.isSelfRegistrationEnabled(tenantId)) {
            throw new SelfRegistrationDisabledException();
        }

        CreateStudentCommand createCommand = new CreateStudentCommand(
                tenantId,
                command.firstName(),
                command.lastName(),
                command.email(),
                command.dateOfBirth(),
                command.eps(),
                command.identityNumber(),
                IdentityDocumentType.valueOf(command.identityDocumentType()),
                parseBloodType(command.bloodType()),
                command.phone(),
                command.tutorFirstName(),
                command.tutorLastName(),
                command.tutorRelationship(),
                command.tutorPhone(),
                command.tutorEmail(),
                SYSTEM_ACTOR
        );

        try {
            createStudentUseCase.execute(createCommand);
        } catch (StudentEmailAlreadyExistsException | StudentIdentityNumberAlreadyExistsException ex) {
            throw new SelfRegistrationConflictException();
        }
    }

    private BloodType parseBloodType(String label) {
        if (label == null || label.isBlank()) {
            return null;
        }
        return BloodType.fromLabel(label);
    }
}
