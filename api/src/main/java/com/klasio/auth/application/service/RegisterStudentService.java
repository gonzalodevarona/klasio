package com.klasio.auth.application.service;

import com.klasio.auth.application.dto.RegisterStudentCommand;
import com.klasio.auth.application.port.*;
import com.klasio.auth.domain.event.StudentRegisteredEvent;
import com.klasio.auth.domain.exception.EmailAlreadyRegisteredException;
import com.klasio.auth.domain.exception.IdentityNumberAlreadyRegisteredException;
import com.klasio.auth.domain.exception.PasswordPolicyViolationException;
import com.klasio.auth.domain.model.EmailVerificationToken;
import com.klasio.auth.domain.model.PasswordPolicy;
import com.klasio.auth.domain.model.User;
import com.klasio.auth.infrastructure.config.AuthProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
public class RegisterStudentService {

    private final UserRepository userRepository;
    private final StudentProfilePort studentProfilePort;
    private final TenantResolverPort tenantResolverPort;
    private final PasswordEncoder passwordEncoder;
    private final TokenGenerator tokenGenerator;
    private final EmailVerificationTokenRepository evtRepository;
    private final AuthEmailSender authEmailSender;
    private final AuthProperties authProperties;
    private final ApplicationEventPublisher eventPublisher;

    public RegisterStudentService(UserRepository userRepository,
                                  StudentProfilePort studentProfilePort,
                                  TenantResolverPort tenantResolverPort,
                                  PasswordEncoder passwordEncoder,
                                  TokenGenerator tokenGenerator,
                                  EmailVerificationTokenRepository evtRepository,
                                  AuthEmailSender authEmailSender,
                                  AuthProperties authProperties,
                                  ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.studentProfilePort = studentProfilePort;
        this.tenantResolverPort = tenantResolverPort;
        this.passwordEncoder = passwordEncoder;
        this.tokenGenerator = tokenGenerator;
        this.evtRepository = evtRepository;
        this.authEmailSender = authEmailSender;
        this.authProperties = authProperties;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public void register(RegisterStudentCommand command) {
        UUID tenantId = tenantResolverPort.resolveTenantIdBySlug(command.tenantSlug())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Tenant with slug '%s' not found".formatted(command.tenantSlug())));

        if (userRepository.existsByEmailAndTenantId(command.email(), tenantId)) {
            throw new EmailAlreadyRegisteredException();
        }

        if (studentProfilePort.existsByIdentityNumberInTenant(tenantId, command.documentNumber())) {
            throw new IdentityNumberAlreadyRegisteredException();
        }

        List<String> violations = PasswordPolicy.validate(command.password());
        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }

        String passwordHash = passwordEncoder.encode(command.password());
        User user = User.createUnverified(tenantId, command.email(), passwordHash);
        userRepository.save(user);

        UUID studentId = studentProfilePort.createStudentProfile(
                tenantId, command.firstName(), command.lastName(), command.email(),
                command.dateOfBirth(), command.documentType(), command.documentNumber(),
                command.eps(), command.tutorFullName(), command.tutorRelationship(),
                command.tutorContact(), user.getId());

        String rawToken = tokenGenerator.generateRawToken();
        String hashedToken = tokenGenerator.hashToken(rawToken);
        Instant expiresAt = Instant.now().plus(authProperties.emailVerificationExpiryHours(), ChronoUnit.HOURS);
        EmailVerificationToken token = EmailVerificationToken.create(user.getId(), hashedToken, expiresAt);
        evtRepository.save(token);

        authEmailSender.sendVerificationEmail(command.email(), rawToken, command.tenantSlug());

        eventPublisher.publishEvent(new StudentRegisteredEvent(
                user.getId(), tenantId, studentId, command.email(), Instant.now()));
    }
}
