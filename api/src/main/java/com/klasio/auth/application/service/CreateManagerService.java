package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.AdminSummary;
import com.klasio.auth.application.dto.CreateAdminCommand;
import com.klasio.auth.application.port.PasswordEncoder;
import com.klasio.auth.application.port.TenantNamePort;
import com.klasio.auth.application.port.UserRepository;
import com.klasio.auth.domain.exception.EmailAlreadyRegisteredException;
import com.klasio.auth.domain.exception.IdentityNumberAlreadyRegisteredException;
import com.klasio.auth.domain.model.Role;
import com.klasio.auth.domain.model.User;
import com.klasio.professor.domain.model.Professor;
import com.klasio.professor.domain.port.ProfessorRepository;
import com.klasio.shared.domain.model.IdentityDocumentType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CreateManagerService {

    private static final String TEMP_CHARS_LOWER   = "abcdefghijklmnopqrstuvwxyz";
    private static final String TEMP_CHARS_UPPER   = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String TEMP_CHARS_DIGIT   = "0123456789";
    private static final String TEMP_CHARS_SPECIAL = "!@#$%^&*()_+";
    private static final String TEMP_CHARS_ALL      =
            TEMP_CHARS_LOWER + TEMP_CHARS_UPPER + TEMP_CHARS_DIGIT + TEMP_CHARS_SPECIAL;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TenantNamePort tenantNamePort;
    private final ProfessorRepository professorRepository;

    public CreateManagerService(UserRepository userRepository,
                                PasswordEncoder passwordEncoder,
                                TenantNamePort tenantNamePort,
                                ProfessorRepository professorRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tenantNamePort = tenantNamePort;
        this.professorRepository = professorRepository;
    }

    /**
     * Generates a cryptographically secure temporary password that satisfies the password policy
     * (8+ chars, uppercase, digit, special char). Used when the caller does not supply a password.
     * The manager must reset it via "forgot password" before their first login.
     */
    private String generateTempPassword() {
        char[] pwd = new char[12];
        // Guarantee one character from each required category.
        pwd[0] = TEMP_CHARS_UPPER  .charAt(SECURE_RANDOM.nextInt(TEMP_CHARS_UPPER.length()));
        pwd[1] = TEMP_CHARS_DIGIT  .charAt(SECURE_RANDOM.nextInt(TEMP_CHARS_DIGIT.length()));
        pwd[2] = TEMP_CHARS_SPECIAL.charAt(SECURE_RANDOM.nextInt(TEMP_CHARS_SPECIAL.length()));
        // Fill the rest from the full charset.
        for (int i = 3; i < pwd.length; i++) {
            pwd[i] = TEMP_CHARS_ALL.charAt(SECURE_RANDOM.nextInt(TEMP_CHARS_ALL.length()));
        }
        // Shuffle to avoid predictable positions.
        for (int i = pwd.length - 1; i > 0; i--) {
            int j = SECURE_RANDOM.nextInt(i + 1);
            char tmp = pwd[i]; pwd[i] = pwd[j]; pwd[j] = tmp;
        }
        return new String(pwd);
    }

    public AdminSummary execute(CreateAdminCommand command) {
        if (userRepository.existsByEmailAndTenantId(command.email(), command.tenantId())) {
            throw new EmailAlreadyRegisteredException();
        }

        if (userRepository.existsByIdentityNumberAndTenantId(command.tenantId(), command.identityNumber())) {
            throw new IdentityNumberAlreadyRegisteredException();
        }

        IdentityDocumentType docType;
        try {
            docType = IdentityDocumentType.valueOf(command.identityDocumentType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid identity document type: " + command.identityDocumentType());
        }

        // Use the provided password or auto-generate a secure temporary one.
        String rawPassword = (command.password() != null && !command.password().isBlank())
                ? command.password()
                : generateTempPassword();

        String passwordHash = passwordEncoder.encode(rawPassword);
        User manager = User.createActive(command.tenantId(), command.email(), passwordHash,
                Role.MANAGER, docType, command.identityNumber(),
                command.firstName(), command.lastName(), command.phoneNumber());
        userRepository.save(manager);

        // A Manager is also a Professor. Create the professor record immediately so the
        // manager appears in professor lists and can be assigned to classes. Status is ACTIVE
        // (no invitation flow — the manager already has credentials).
        Professor professor = Professor.createForManager(
                manager.getId(), command.tenantId(),
                manager.getFirstName(), manager.getLastName(), manager.getEmail(),
                manager.getPhoneNumber(), docType, command.identityNumber(), command.createdBy());
        professorRepository.save(professor);

        Map<UUID, String> names = tenantNamePort.findNamesByIds(Set.of(command.tenantId()));
        String tenantName = names.getOrDefault(command.tenantId(), command.tenantId().toString());

        return new AdminSummary(
                manager.getId(), manager.getTenantId(), tenantName,
                manager.getEmail(), manager.getFirstName(), manager.getLastName(),
                manager.getIdentityDocumentType().name(), manager.getIdentityNumber(),
                manager.getPhoneNumber(), manager.getStatus().name(), manager.getCreatedAt()
        );
    }
}
