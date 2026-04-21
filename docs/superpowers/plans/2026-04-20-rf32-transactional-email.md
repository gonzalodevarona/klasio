# RF-32 Transactional Email (Brevo) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the synchronous `JavaMailSender`/`PostmarkAuthEmailSender` path with a platform-level, fire-and-forget `com.klasio.email` module that delivers 9 transactional email types via Brevo HTTP API (in-repo Thymeleaf templates for system-critical copy, Brevo-hosted templates for informational copy).

**Architecture:** New `com.klasio.email` hexagonal module owns one inbound port (`EmailService`), three outbound ports (`EmailTransport`, `TemplateRenderer`, `TenantContextPort`), and a `EmailDispatcherService` that orchestrates tenant context caching (Caffeine), template rendering, and transport dispatch. Listeners stay in each originating module (`auth`, `membership`, `professor`, `attendance`) and call `EmailService.send(EmailType, EmailRecipient, tenantId, params)` asynchronously after TX commit. Three transport implementations are profile-selected via `@ConditionalOnProperty`: `LoggingEmailTransport` (local), `InMemoryEmailTransport` (test), `BrevoEmailTransport` (dev/staging/prod) with Spring Retry exponential backoff.

**Tech Stack:** Java 21, Spring Boot 3.4.3, Spring Retry 2.x + AOP, Thymeleaf 3.x (two dedicated `TemplateEngine` beans), Caffeine cache, Spring `RestClient` (HTTP to Brevo), WireMock 3.x (wire-format IT), Awaitility (async listener ITs).

---

## File Structure

### New files
```
api/src/main/java/com/klasio/email/
  application/
    EmailService.java
    EmailType.java
    EmailRecipient.java
  domain/model/
    OutboundEmail.java
    EmailSender.java
    TenantContext.java
    RenderedTemplate.java
  domain/port/
    EmailTransport.java
    TemplateRenderer.java
    TenantContextPort.java
  infrastructure/
    config/
      EmailProperties.java
      BrevoProperties.java
      FrontendProperties.java
      EmailExecutorConfig.java
      EmailRetryConfig.java
      ThymeleafEmailConfig.java
    transport/
      InMemoryEmailTransport.java
      LoggingEmailTransport.java
      BrevoEmailTransport.java
    template/
      ThymeleafTemplateRenderer.java
    tenant/
      JpaTenantContextAdapter.java
    service/
      EmailDispatcherService.java

api/src/main/resources/email-templates/
  layouts/base.html
  professor-invitation.html / .txt
  student-verification.html / .txt
  password-recovery.html / .txt
  payment-proof-uploaded.html / .txt
  payment-rejected.html / .txt
  missing-template-fallback.html / .txt

api/src/main/resources/application-test.yml

api/src/main/java/com/klasio/shared/infrastructure/web/
  FrontendUrlBuilder.java

api/src/main/java/com/klasio/auth/domain/event/
  VerificationEmailResendRequested.java
api/src/main/java/com/klasio/auth/infrastructure/notification/
  AuthEmailListener.java

api/src/main/java/com/klasio/professor/infrastructure/notification/
  ProfessorInvitationEmailListener.java

api/src/main/java/com/klasio/membership/domain/port/
  StudentEmailPort.java   (new)
  TenantAdminEmailPort.java (new)
api/src/main/java/com/klasio/membership/infrastructure/persistence/
  StudentEmailAdapter.java (new)
  TenantAdminEmailAdapter.java (new)

api/src/main/java/com/klasio/attendance/domain/port/
  StudentEmailPort.java   (new — own copy, own adapter)
api/src/main/java/com/klasio/attendance/infrastructure/persistence/
  AttendanceStudentEmailAdapter.java (new)
```

### Modified files
```
api/pom.xml
api/src/main/resources/application.yml
api/src/main/resources/application-local.yml
api/src/main/java/com/klasio/auth/domain/event/StudentRegisteredEvent.java
api/src/main/java/com/klasio/auth/domain/event/PasswordResetRequestedEvent.java
api/src/main/java/com/klasio/auth/application/service/RegisterStudentService.java
api/src/main/java/com/klasio/auth/application/service/RequestPasswordResetService.java
api/src/main/java/com/klasio/auth/application/service/ResendVerificationEmailService.java
api/src/main/java/com/klasio/professor/domain/event/ProfessorCreated.java
api/src/main/java/com/klasio/professor/domain/model/Professor.java
api/src/main/java/com/klasio/membership/domain/event/MembershipActivated.java
api/src/main/java/com/klasio/membership/domain/event/MembershipExpiryWarning.java
api/src/main/java/com/klasio/membership/domain/model/Membership.java
api/src/main/java/com/klasio/membership/infrastructure/scheduler/MembershipExpirationJob.java
api/src/main/java/com/klasio/membership/infrastructure/notification/MembershipNotificationListener.java
api/src/main/java/com/klasio/membership/infrastructure/notification/PaymentProofNotificationListener.java
api/src/main/java/com/klasio/attendance/infrastructure/notification/SessionEventsNotificationListener.java
docker/docker-compose.yml
functional-requirements.md
```

### Deleted files
```
api/src/main/java/com/klasio/auth/application/port/AuthEmailSender.java
api/src/main/java/com/klasio/auth/infrastructure/mail/PostmarkAuthEmailSender.java
```

---

## Phase 1 — Scaffold `com.klasio.email` module

### Task 1: Maven dependencies + YAML config skeleton

**Files:**
- Modify: `api/pom.xml`
- Modify: `api/src/main/resources/application.yml`
- Modify: `api/src/main/resources/application-local.yml`

- [ ] **Step 1: Remove `spring-boot-starter-mail`, add new deps to `api/pom.xml`**

In `api/pom.xml`, find and remove:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
```

Add in its place:
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-aop</artifactId>
</dependency>
<dependency>
    <groupId>org.wiremock</groupId>
    <artifactId>wiremock</artifactId>
    <version>3.13.0</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 2: Add email config block to `application.yml`**

Append after the existing `klasio:` block entries:
```yaml
  frontend:
    url-template: ${KLASIO_FRONTEND_URL_TEMPLATE:http://localhost:3000}
  email:
    transport: ${KLASIO_EMAIL_TRANSPORT:logging}
    from:
      address: ${KLASIO_EMAIL_FROM_ADDRESS:noreply@klasio.app}
      name-suffix: " via Klasio"
    retry:
      max-attempts: 3
      initial-delay-ms: 1000
      multiplier: 5
      max-delay-ms: 30000
    brevo:
      api-key: ${BREVO_API_KEY:}
      api-base-url: https://api.brevo.com/v3
      template-ids:
        membership-activated: ${BREVO_TEMPLATE_MEMBERSHIP_ACTIVATED:0}
        membership-expiry-warning: ${BREVO_TEMPLATE_MEMBERSHIP_EXPIRY:0}
        membership-depleted: ${BREVO_TEMPLATE_MEMBERSHIP_DEPLETED:0}
        class-session-change: ${BREVO_TEMPLATE_CLASS_SESSION_CHANGE:0}
```

Also remove `klasio.auth.from-email` line from `application.yml` (it moves to `klasio.email.from.address`).

- [ ] **Step 3: Update `application-local.yml`**

Remove the entire `spring.mail.*` block:
```yaml
  mail:
    host: localhost
    port: 1025
    username: ""
    password: ""
    properties:
      mail.smtp.auth: false
      mail.smtp.starttls.enable: false
```

Add under the `klasio:` section:
```yaml
  email:
    transport: logging
```

- [ ] **Step 4: Verify the app still compiles (will fail on `AuthEmailSender` references — that's expected for now)**

```bash
cd api && mvn compile -q 2>&1 | grep "ERROR" | head -20
```

Expected: compile errors only about `AuthEmailSender` (not yet deleted). Ignore for now — they'll be resolved in Task 12.

- [ ] **Step 5: Commit**

```bash
git add api/pom.xml api/src/main/resources/application.yml api/src/main/resources/application-local.yml
git commit -m "chore(email): add thymeleaf/retry/aop deps, scaffold email YAML config, drop spring-boot-starter-mail"
```

---

### Task 2: Value types, port interfaces, and `EmailType` registry

**Files:**
- Create: `api/src/main/java/com/klasio/email/application/EmailType.java`
- Create: `api/src/main/java/com/klasio/email/application/EmailService.java`
- Create: `api/src/main/java/com/klasio/email/application/EmailRecipient.java`
- Create: `api/src/main/java/com/klasio/email/domain/model/OutboundEmail.java`
- Create: `api/src/main/java/com/klasio/email/domain/model/EmailSender.java`
- Create: `api/src/main/java/com/klasio/email/domain/model/TenantContext.java`
- Create: `api/src/main/java/com/klasio/email/domain/model/RenderedTemplate.java`
- Create: `api/src/main/java/com/klasio/email/domain/port/EmailTransport.java`
- Create: `api/src/main/java/com/klasio/email/domain/port/TemplateRenderer.java`
- Create: `api/src/main/java/com/klasio/email/domain/port/TenantContextPort.java`

- [ ] **Step 1: Write tests for `EmailType` contract**

Create `api/src/test/java/com/klasio/email/application/EmailTypeTest.java`:
```java
package com.klasio.email.application;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class EmailTypeTest {

    @Test
    void inRepoTypesHaveTemplateRefWithoutPrefix() {
        assertThat(EmailType.STUDENT_VERIFICATION.source()).isEqualTo(EmailType.Source.IN_REPO);
        assertThat(EmailType.STUDENT_VERIFICATION.templateRef()).isEqualTo("student-verification");
        assertThat(EmailType.STUDENT_VERIFICATION.requiredKeys())
                .containsExactlyInAnyOrder("verificationUrl", "expiresAt", "studentName");
    }

    @Test
    void brevoHostedTypesHaveShortTemplateRef() {
        assertThat(EmailType.MEMBERSHIP_ACTIVATED.source()).isEqualTo(EmailType.Source.BREVO_HOSTED);
        assertThat(EmailType.MEMBERSHIP_ACTIVATED.templateRef()).isEqualTo("membership-activated");
    }

    @Test
    void allNineTypesAreDefined() {
        assertThat(EmailType.values()).hasSize(9);
    }
}
```

- [ ] **Step 2: Run test — expect compile failure (class doesn't exist yet)**

```bash
cd api && mvn test -pl . -Dtest=EmailTypeTest -q 2>&1 | tail -5
```

Expected: `COMPILATION ERROR`

- [ ] **Step 3: Create `EmailType.java`**

```java
package com.klasio.email.application;

import java.util.Set;

public enum EmailType {
    PROFESSOR_INVITATION(Source.IN_REPO, "professor-invitation",
            Set.of("activationUrl", "expiresAt", "professorName")),
    STUDENT_VERIFICATION(Source.IN_REPO, "student-verification",
            Set.of("verificationUrl", "expiresAt", "studentName")),
    PASSWORD_RECOVERY(Source.IN_REPO, "password-recovery",
            Set.of("resetUrl", "expiresAt")),
    PAYMENT_PROOF_UPLOADED(Source.IN_REPO, "payment-proof-uploaded",
            Set.of("studentName", "programName", "reviewUrl")),
    PAYMENT_REJECTED(Source.IN_REPO, "payment-rejected",
            Set.of("studentName", "programName", "reason", "retryUrl")),
    MEMBERSHIP_ACTIVATED(Source.BREVO_HOSTED, "membership-activated",
            Set.of("studentName", "programName", "planName", "totalHours", "expiresAt")),
    MEMBERSHIP_EXPIRY_WARNING(Source.BREVO_HOSTED, "membership-expiry-warning",
            Set.of("studentName", "programName", "expiresAt", "remainingHours")),
    MEMBERSHIP_DEPLETED(Source.BREVO_HOSTED, "membership-depleted",
            Set.of("studentName", "programName")),
    CLASS_SESSION_CHANGE(Source.BREVO_HOSTED, "class-session-change",
            Set.of("studentName", "className", "startsAt", "changeKind", "reason"));

    public enum Source { IN_REPO, BREVO_HOSTED }

    private final Source source;
    private final String templateRef;
    private final Set<String> requiredKeys;

    EmailType(Source source, String templateRef, Set<String> requiredKeys) {
        this.source = source;
        this.templateRef = templateRef;
        this.requiredKeys = requiredKeys;
    }

    public Source source()            { return source; }
    public String templateRef()       { return templateRef; }
    public Set<String> requiredKeys() { return requiredKeys; }
}
```

- [ ] **Step 4: Create remaining value types and port interfaces**

`EmailRecipient.java`:
```java
package com.klasio.email.application;
public record EmailRecipient(String email, String displayName) {}
```

`EmailService.java`:
```java
package com.klasio.email.application;

import java.util.Map;
import java.util.UUID;

public interface EmailService {
    void send(EmailType type, EmailRecipient to, UUID tenantId, Map<String, Object> params);
}
```

`EmailSender.java`:
```java
package com.klasio.email.domain.model;
public record EmailSender(String email, String displayName) {}
```

`TenantContext.java`:
```java
package com.klasio.email.domain.model;
import java.util.UUID;
public record TenantContext(UUID id, String slug, String name) {}
```

`RenderedTemplate.java`:
```java
package com.klasio.email.domain.model;
public record RenderedTemplate(String subject, String htmlBody, String textBody) {}
```

`OutboundEmail.java`:
```java
package com.klasio.email.domain.model;

import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailType;
import java.util.Map;

public record OutboundEmail(
        EmailType type,
        EmailRecipient to,
        EmailSender from,
        String subject,
        String htmlBody,
        String textBody,
        Long brevoTemplateId,
        Map<String, Object> brevoParams,
        String idempotencyKey
) {}
```

`EmailTransport.java`:
```java
package com.klasio.email.domain.port;
import com.klasio.email.domain.model.OutboundEmail;
public interface EmailTransport {
    void send(OutboundEmail email);
}
```

`TemplateRenderer.java`:
```java
package com.klasio.email.domain.port;
import com.klasio.email.domain.model.RenderedTemplate;
import java.util.Map;
public interface TemplateRenderer {
    RenderedTemplate render(String templatePath, Map<String, Object> model);
}
```

`TenantContextPort.java`:
```java
package com.klasio.email.domain.port;
import com.klasio.email.domain.model.TenantContext;
import java.util.UUID;
public interface TenantContextPort {
    TenantContext findById(UUID tenantId);
}
```

- [ ] **Step 5: Run `EmailTypeTest` — expect PASS**

```bash
cd api && mvn test -pl . -Dtest=EmailTypeTest -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/email/ api/src/test/java/com/klasio/email/
git commit -m "feat(email): add EmailType registry, EmailService port, all value types and outbound ports"
```

---

### Task 3: `EmailDispatcherService` (TDD)

**Files:**
- Create: `api/src/main/java/com/klasio/email/infrastructure/config/EmailProperties.java`
- Create: `api/src/main/java/com/klasio/email/infrastructure/config/BrevoProperties.java`
- Create: `api/src/main/java/com/klasio/email/infrastructure/config/FrontendProperties.java`
- Create: `api/src/main/java/com/klasio/email/infrastructure/config/EmailRetryConfig.java`
- Create: `api/src/main/java/com/klasio/email/infrastructure/service/EmailDispatcherService.java`
- Create: `api/src/test/java/com/klasio/email/infrastructure/service/EmailDispatcherServiceTest.java`

- [ ] **Step 1: Write failing tests for `EmailDispatcherService`**

Create `api/src/test/java/com/klasio/email/infrastructure/service/EmailDispatcherServiceTest.java`:
```java
package com.klasio.email.infrastructure.service;

import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailType;
import com.klasio.email.domain.model.*;
import com.klasio.email.domain.port.EmailTransport;
import com.klasio.email.domain.port.TemplateRenderer;
import com.klasio.email.domain.port.TenantContextPort;
import com.klasio.email.infrastructure.config.BrevoProperties;
import com.klasio.email.infrastructure.config.EmailProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailDispatcherServiceTest {

    private final EmailTransport transport = mock(EmailTransport.class);
    private final TemplateRenderer renderer = mock(TemplateRenderer.class);
    private final TenantContextPort tenantContextPort = mock(TenantContextPort.class);

    private final EmailProperties props = new EmailProperties(
            "logging",
            new EmailProperties.FromProperties("noreply@klasio.app", " via Klasio"),
            new EmailProperties.RetryProperties(3, 1000, 5.0, 30000));

    private final BrevoProperties brevoProps = new BrevoProperties(
            "test-key", "https://api.brevo.com/v3",
            Map.of("membership-activated", 42L));

    private EmailDispatcherService service;

    private final UUID tenantId = UUID.randomUUID();
    private final TenantContext tenant = new TenantContext(tenantId, "test-league", "Test League");

    @BeforeEach
    void setUp() {
        when(tenantContextPort.findById(tenantId)).thenReturn(tenant);
        service = new EmailDispatcherService(transport, renderer, tenantContextPort, props, brevoProps);
    }

    @Test
    void inRepoType_rendersTemplateAndPassesToTransport() {
        when(renderer.render(eq("student-verification"), anyMap()))
                .thenReturn(new RenderedTemplate("Verify your account", "<html/>", "plain text"));

        service.send(EmailType.STUDENT_VERIFICATION,
                new EmailRecipient("user@example.com", "Juan"),
                tenantId,
                Map.of("verificationUrl", "https://x.com", "expiresAt", "2026-05-01", "studentName", "Juan"));

        var captor = org.mockito.ArgumentCaptor.forClass(OutboundEmail.class);
        verify(transport).send(captor.capture());
        OutboundEmail sent = captor.getValue();
        assertThat(sent.type()).isEqualTo(EmailType.STUDENT_VERIFICATION);
        assertThat(sent.subject()).isEqualTo("Verify your account");
        assertThat(sent.htmlBody()).isEqualTo("<html/>");
        assertThat(sent.brevoTemplateId()).isNull();
        assertThat(sent.from().email()).isEqualTo("noreply@klasio.app");
        assertThat(sent.from().displayName()).isEqualTo("Test League via Klasio");
    }

    @Test
    void brevoHostedType_withConfiguredTemplateId_passesTemplateIdToTransport() {
        service.send(EmailType.MEMBERSHIP_ACTIVATED,
                new EmailRecipient("s@example.com", "Ana"),
                tenantId,
                Map.of("studentName", "Ana", "programName", "Tennis",
                       "planName", "Monthly", "totalHours", 20, "expiresAt", "2026-05-31"));

        var captor = org.mockito.ArgumentCaptor.forClass(OutboundEmail.class);
        verify(transport).send(captor.capture());
        OutboundEmail sent = captor.getValue();
        assertThat(sent.brevoTemplateId()).isEqualTo(42L);
        assertThat(sent.htmlBody()).isNull();
        assertThat(sent.brevoParams()).containsEntry("tenantName", "Test League");
    }

    @Test
    void brevoHostedType_withMissingTemplateId_fallsBackToInRepoFallback() {
        when(renderer.render(eq("missing-template-fallback"), anyMap()))
                .thenReturn(new RenderedTemplate("Email not configured", "<html>fallback</html>", "fallback"));

        service.send(EmailType.MEMBERSHIP_DEPLETED,
                new EmailRecipient("s@example.com", "Ana"),
                tenantId,
                Map.of("studentName", "Ana", "programName", "Tennis"));

        var captor = org.mockito.ArgumentCaptor.forClass(OutboundEmail.class);
        verify(transport).send(captor.capture());
        assertThat(captor.getValue().brevoTemplateId()).isNull();
        assertThat(captor.getValue().htmlBody()).isEqualTo("<html>fallback</html>");
    }

    @Test
    void missingRequiredParam_throwsIllegalArgument() {
        assertThatThrownBy(() -> service.send(
                EmailType.STUDENT_VERIFICATION,
                new EmailRecipient("u@x.com", "U"),
                tenantId,
                Map.of("verificationUrl", "https://x.com"))) // missing expiresAt, studentName
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Missing required params");
    }

    @Test
    void transportThrows_exceptionIsSwallowed_noExceptionEscapes() {
        when(renderer.render(anyString(), anyMap()))
                .thenReturn(new RenderedTemplate("subj", "<html/>", "txt"));
        doThrow(new RuntimeException("Brevo is down")).when(transport).send(any());

        assertThatCode(() -> service.send(
                EmailType.STUDENT_VERIFICATION,
                new EmailRecipient("u@x.com", "U"),
                tenantId,
                Map.of("verificationUrl", "https://x.com", "expiresAt", "2026-05-01", "studentName", "U")))
                .doesNotThrowAnyException();
    }

    @Test
    void eachSendCallGeneratesDistinctIdempotencyKey() {
        when(renderer.render(anyString(), anyMap()))
                .thenReturn(new RenderedTemplate("s", "<h/>", "t"));
        Map<String, Object> params = Map.of("verificationUrl", "u", "expiresAt", "e", "studentName", "n");

        service.send(EmailType.STUDENT_VERIFICATION, new EmailRecipient("a@x.com", "A"), tenantId, params);
        service.send(EmailType.STUDENT_VERIFICATION, new EmailRecipient("b@x.com", "B"), tenantId, params);

        var captor = org.mockito.ArgumentCaptor.forClass(OutboundEmail.class);
        verify(transport, times(2)).send(captor.capture());
        List<OutboundEmail> sent = captor.getAllValues();
        assertThat(sent.get(0).idempotencyKey()).isNotEqualTo(sent.get(1).idempotencyKey());
    }
}
```

- [ ] **Step 2: Run tests — expect compile failure**

```bash
cd api && mvn test -pl . -Dtest=EmailDispatcherServiceTest -q 2>&1 | tail -5
```

Expected: `COMPILATION ERROR` — `EmailDispatcherService` doesn't exist yet.

- [ ] **Step 3: Create config property classes**

`EmailProperties.java`:
```java
package com.klasio.email.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klasio.email")
public record EmailProperties(
        String transport,
        FromProperties from,
        RetryProperties retry
) {
    public record FromProperties(String address, String nameSuffix) {}
    public record RetryProperties(int maxAttempts, long initialDelayMs, double multiplier, long maxDelayMs) {}
}
```

`BrevoProperties.java`:
```java
package com.klasio.email.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Collections;
import java.util.Map;

@ConfigurationProperties(prefix = "klasio.email.brevo")
public record BrevoProperties(
        String apiKey,
        String apiBaseUrl,
        Map<String, Long> templateIds
) {
    public BrevoProperties {
        templateIds = templateIds == null ? Collections.emptyMap() : templateIds;
    }
}
```

`FrontendProperties.java`:
```java
package com.klasio.email.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "klasio.frontend")
public record FrontendProperties(String urlTemplate) {}
```

`EmailRetryConfig.java`:
```java
package com.klasio.email.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;

@Configuration
@EnableRetry
@EnableConfigurationProperties({EmailProperties.class, BrevoProperties.class, FrontendProperties.class})
public class EmailRetryConfig {}
```

- [ ] **Step 4: Create `EmailDispatcherService.java`**

```java
package com.klasio.email.infrastructure.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.email.domain.model.*;
import com.klasio.email.domain.port.EmailTransport;
import com.klasio.email.domain.port.TemplateRenderer;
import com.klasio.email.domain.port.TenantContextPort;
import com.klasio.email.infrastructure.config.BrevoProperties;
import com.klasio.email.infrastructure.config.EmailProperties;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.security.MessageDigest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class EmailDispatcherService implements EmailService {

    private final EmailTransport transport;
    private final TemplateRenderer renderer;
    private final EmailProperties props;
    private final BrevoProperties brevoProps;
    private final LoadingCache<UUID, TenantContext> tenantCache;
    private final Set<EmailType> warnedMissingTemplate = ConcurrentHashMap.newKeySet();

    public EmailDispatcherService(EmailTransport transport,
                                  TemplateRenderer renderer,
                                  TenantContextPort tenantContextPort,
                                  EmailProperties props,
                                  BrevoProperties brevoProps) {
        this.transport = transport;
        this.renderer = renderer;
        this.props = props;
        this.brevoProps = brevoProps;
        this.tenantCache = Caffeine.newBuilder()
                .maximumSize(500)
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .build(tenantContextPort::findById);
    }

    @Override
    public void send(EmailType type, EmailRecipient to, UUID tenantId, Map<String, Object> params) {
        Set<String> missing = new HashSet<>(type.requiredKeys());
        missing.removeAll(params.keySet());
        if (!missing.isEmpty()) {
            throw new IllegalArgumentException(
                    "Missing required params for " + type + ": " + missing);
        }

        try {
            MDC.put("emailType", type.name());
            MDC.put("tenantId", tenantId.toString());
            MDC.put("recipientEmailHash", sha256First8(to.email()));

            TenantContext tenant = tenantCache.get(tenantId);
            EmailSender from = new EmailSender(
                    props.from().address(),
                    tenant.name() + props.from().nameSuffix());
            String idempotencyKey = UUID.randomUUID().toString();

            OutboundEmail outbound = buildOutbound(type, to, from, tenant, params, idempotencyKey);
            transport.send(outbound);

        } catch (IllegalArgumentException e) {
            throw e; // propagate missing-param errors (fail fast)
        } catch (Exception e) {
            log.error("[EMAIL] Failed to dispatch type={} tenantId={}: {}",
                    type, tenantId, e.getMessage(), e);
        } finally {
            MDC.remove("emailType");
            MDC.remove("tenantId");
            MDC.remove("recipientEmailHash");
        }
    }

    private OutboundEmail buildOutbound(EmailType type, EmailRecipient to, EmailSender from,
                                        TenantContext tenant, Map<String, Object> params,
                                        String idempotencyKey) {
        if (type.source() == EmailType.Source.IN_REPO) {
            Map<String, Object> model = new HashMap<>(params);
            model.put("tenantName", tenant.name());
            model.put("tenantSlug", tenant.slug());
            RenderedTemplate rendered = renderer.render(type.templateRef(), model);
            return new OutboundEmail(type, to, from,
                    rendered.subject(), rendered.htmlBody(), rendered.textBody(),
                    null, null, idempotencyKey);
        }

        Long templateId = brevoProps.templateIds().get(type.templateRef());
        if (templateId == null || templateId == 0L) {
            if (warnedMissingTemplate.add(type)) {
                log.warn("[EMAIL] No Brevo template ID configured for {}, falling back to missing-template-fallback", type);
            }
            Map<String, Object> model = new HashMap<>(params);
            model.put("tenantName", tenant.name());
            model.put("emailTypeName", type.name());
            RenderedTemplate fallback = renderer.render("missing-template-fallback", model);
            return new OutboundEmail(type, to, from,
                    fallback.subject(), fallback.htmlBody(), fallback.textBody(),
                    null, null, idempotencyKey);
        }

        Map<String, Object> brevoParams = new HashMap<>(params);
        brevoParams.put("tenantName", tenant.name());
        return new OutboundEmail(type, to, from,
                null, null, null, templateId, brevoParams, idempotencyKey);
    }

    private static String sha256First8(String input) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 4; i++) sb.append(String.format("%02x", hash[i]));
            return sb.toString();
        } catch (Exception e) {
            return "unknown";
        }
    }
}
```

- [ ] **Step 5: Run tests — expect PASS**

```bash
cd api && mvn test -pl . -Dtest=EmailDispatcherServiceTest -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/email/ api/src/test/java/com/klasio/email/
git commit -m "feat(email): add EmailDispatcherService with Caffeine tenant cache and fallback routing"
```

---

### Task 4: `ThymeleafTemplateRenderer` + config

**Files:**
- Create: `api/src/main/java/com/klasio/email/infrastructure/config/ThymeleafEmailConfig.java`
- Create: `api/src/main/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRenderer.java`
- Create: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`

- [ ] **Step 1: Write failing test (needs a real template to exist)**

Create `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`:
```java
package com.klasio.email.infrastructure.template;

import com.klasio.email.domain.model.RenderedTemplate;
import com.klasio.email.infrastructure.config.ThymeleafEmailConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(classes = ThymeleafEmailConfig.class)
@TestPropertySource(properties = "spring.main.web-application-type=none")
class ThymeleafTemplateRendererTest {

    @Autowired
    ThymeleafTemplateRenderer renderer;

    @Test
    void studentVerification_extractsSubjectAndRendersBody() {
        RenderedTemplate result = renderer.render("student-verification", Map.of(
                "studentName", "Carlos",
                "verificationUrl", "https://test.klasio.com/verify?token=abc",
                "expiresAt", "2026-05-01 09:00",
                "tenantName", "Test League",
                "tenantSlug", "test-league"));

        assertThat(result.subject()).isNotBlank();
        assertThat(result.subject()).doesNotContain("${");
        assertThat(result.htmlBody()).contains("Carlos");
        assertThat(result.htmlBody()).contains("https://test.klasio.com/verify?token=abc");
        assertThat(result.textBody()).contains("Carlos");
        assertThat(result.textBody()).doesNotContain("${");
    }

    @Test
    void passwordRecovery_rendersWithoutUnresolvedVariables() {
        RenderedTemplate result = renderer.render("password-recovery", Map.of(
                "resetUrl", "https://test.klasio.com/reset?token=xyz",
                "expiresAt", "30 minutes",
                "tenantName", "Test League",
                "tenantSlug", "test-league"));

        assertThat(result.subject()).doesNotContain("${");
        assertThat(result.htmlBody()).doesNotContain("${");
        assertThat(result.textBody()).doesNotContain("${");
    }
}
```

- [ ] **Step 2: Run test — expect compile failure**

```bash
cd api && mvn test -pl . -Dtest=ThymeleafTemplateRendererTest -q 2>&1 | tail -5
```

Expected: `COMPILATION ERROR`

- [ ] **Step 3: Create `ThymeleafEmailConfig.java`**

```java
package com.klasio.email.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

@Configuration
public class ThymeleafEmailConfig {

    @Bean("emailHtmlEngine")
    TemplateEngine emailHtmlEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("email-templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    @Bean("emailTextEngine")
    TemplateEngine emailTextEngine() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("email-templates/");
        resolver.setSuffix(".txt");
        resolver.setTemplateMode(TemplateMode.TEXT);
        resolver.setCharacterEncoding("UTF-8");
        resolver.setCacheable(false);
        TemplateEngine engine = new TemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }
}
```

- [ ] **Step 4: Create `ThymeleafTemplateRenderer.java`**

```java
package com.klasio.email.infrastructure.template;

import com.klasio.email.domain.model.RenderedTemplate;
import com.klasio.email.domain.port.TemplateRenderer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Locale;
import java.util.Map;

@Component
public class ThymeleafTemplateRenderer implements TemplateRenderer {

    private final TemplateEngine htmlEngine;
    private final TemplateEngine textEngine;

    public ThymeleafTemplateRenderer(
            @Qualifier("emailHtmlEngine") TemplateEngine htmlEngine,
            @Qualifier("emailTextEngine") TemplateEngine textEngine) {
        this.htmlEngine = htmlEngine;
        this.textEngine = textEngine;
    }

    @Override
    public RenderedTemplate render(String templatePath, Map<String, Object> model) {
        Context ctx = new Context(Locale.ENGLISH, model);
        // Extract subject from th:fragment="subject" in the HTML template
        String subject = htmlEngine.process(templatePath + " :: subject", ctx).strip();
        String htmlBody = htmlEngine.process(templatePath, ctx);
        String textBody = textEngine.process(templatePath, ctx);
        return new RenderedTemplate(subject, htmlBody, textBody);
    }
}
```

- [ ] **Step 5: The test needs templates — they'll be created in Task 9. Create placeholder stubs now so the test can compile and skip gracefully**

Create `api/src/main/resources/email-templates/student-verification.html` (minimal):
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <th:block th:fragment="subject" th:text="|Verify your account at ${tenantName}|"></th:block>
</head>
<body>
  <p>Hi <span th:text="${studentName}">Student</span>,</p>
  <p><a th:href="${verificationUrl}">Verify your email</a> (expires <span th:text="${expiresAt}"></span>)</p>
</body>
</html>
```

Create `api/src/main/resources/email-templates/student-verification.txt`:
```
Hi [(${studentName})],

Please verify your email: [(${verificationUrl})]

This link expires at [(${expiresAt})].

Sent by [(${tenantName})] via Klasio.
```

Create `api/src/main/resources/email-templates/password-recovery.html`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <th:block th:fragment="subject" th:text="|Reset your ${tenantName} password|"></th:block>
</head>
<body>
  <p>Click to reset your password: <a th:href="${resetUrl}">Reset password</a></p>
  <p>Link expires in <span th:text="${expiresAt}"></span>.</p>
</body>
</html>
```

Create `api/src/main/resources/email-templates/password-recovery.txt`:
```
Reset your password: [(${resetUrl})]

This link expires in [(${expiresAt})].
```

- [ ] **Step 6: Run test — expect PASS**

```bash
cd api && mvn test -pl . -Dtest=ThymeleafTemplateRendererTest -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

- [ ] **Step 7: Commit**

```bash
git add api/src/main/java/com/klasio/email/infrastructure/config/ThymeleafEmailConfig.java \
        api/src/main/java/com/klasio/email/infrastructure/template/ \
        api/src/main/resources/email-templates/ \
        api/src/test/java/com/klasio/email/infrastructure/template/
git commit -m "feat(email): add ThymeleafTemplateRenderer with HTML+TEXT dual engine config"
```

---

### Task 5: `JpaTenantContextAdapter`

**Files:**
- Create: `api/src/main/java/com/klasio/email/infrastructure/tenant/JpaTenantContextAdapter.java`
- Test: covered by existing tenant data in integration context; unit test with mock `EntityManager`

- [ ] **Step 1: Write failing unit test**

Create `api/src/test/java/com/klasio/email/infrastructure/tenant/JpaTenantContextAdapterTest.java`:
```java
package com.klasio.email.infrastructure.tenant;

import com.klasio.email.domain.model.TenantContext;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class JpaTenantContextAdapterTest {

    private final EntityManager em = mock(EntityManager.class);
    private final JpaTenantContextAdapter adapter = new JpaTenantContextAdapter(em);

    @Test
    void findById_returnsTenantContext() {
        UUID id = UUID.randomUUID();
        Query query = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(new Object[]{id, "test-slug", "Test League"}));

        TenantContext ctx = adapter.findById(id);

        assertThat(ctx.id()).isEqualTo(id);
        assertThat(ctx.slug()).isEqualTo("test-slug");
        assertThat(ctx.name()).isEqualTo("Test League");
    }

    @Test
    void findById_unknownTenant_throwsIllegalArgument() {
        UUID id = UUID.randomUUID();
        Query query = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of());

        assertThatThrownBy(() -> adapter.findById(id))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tenant not found");
    }
}
```

- [ ] **Step 2: Run test — expect compile failure**

```bash
cd api && mvn test -pl . -Dtest=JpaTenantContextAdapterTest -q 2>&1 | tail -5
```

- [ ] **Step 3: Create `JpaTenantContextAdapter.java`**

```java
package com.klasio.email.infrastructure.tenant;

import com.klasio.email.domain.model.TenantContext;
import com.klasio.email.domain.port.TenantContextPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class JpaTenantContextAdapter implements TenantContextPort {

    private final EntityManager em;

    public JpaTenantContextAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    @Transactional(readOnly = true)
    public TenantContext findById(UUID tenantId) {
        @SuppressWarnings("unchecked")
        List<Object[]> rows = em.createQuery(
                        "SELECT t.id, t.slug, t.name FROM TenantJpaEntity t WHERE t.id = :id")
                .setParameter("id", tenantId)
                .getResultList();
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }
        Object[] r = rows.get(0);
        return new TenantContext((UUID) r[0], (String) r[1], (String) r[2]);
    }
}
```

- [ ] **Step 4: Run tests — expect PASS**

```bash
cd api && mvn test -pl . -Dtest=JpaTenantContextAdapterTest -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/email/infrastructure/tenant/ \
        api/src/test/java/com/klasio/email/infrastructure/tenant/
git commit -m "feat(email): add JpaTenantContextAdapter querying TenantJpaEntity via EntityManager"
```

---

### Task 6: `InMemoryEmailTransport` + `application-test.yml`

**Files:**
- Create: `api/src/main/java/com/klasio/email/infrastructure/transport/InMemoryEmailTransport.java`
- Create: `api/src/main/resources/application-test.yml`

- [ ] **Step 1: Create `InMemoryEmailTransport.java`**

```java
package com.klasio.email.infrastructure.transport;

import com.klasio.email.application.EmailType;
import com.klasio.email.domain.model.OutboundEmail;
import com.klasio.email.domain.port.EmailTransport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@ConditionalOnProperty(name = "klasio.email.transport", havingValue = "inmemory")
public class InMemoryEmailTransport implements EmailTransport {

    private final List<OutboundEmail> recorded = new CopyOnWriteArrayList<>();

    @Override
    public void send(OutboundEmail email) {
        recorded.add(email);
    }

    public List<OutboundEmail> recordedFor(EmailType type) {
        return recorded.stream().filter(e -> e.type() == type).toList();
    }

    public List<OutboundEmail> all() {
        return List.copyOf(recorded);
    }

    public void clear() {
        recorded.clear();
    }
}
```

- [ ] **Step 2: Create `application-test.yml`**

```yaml
klasio:
  email:
    transport: inmemory
    from:
      address: noreply@klasio.app
      name-suffix: " via Klasio"
    retry:
      max-attempts: 1
      initial-delay-ms: 0
      multiplier: 1
      max-delay-ms: 0
    brevo:
      api-key: test-key
      api-base-url: http://localhost:9999
      template-ids:
        membership-activated: 42
        membership-expiry-warning: 43
        membership-depleted: 44
        class-session-change: 45
  frontend:
    url-template: http://localhost:3000
```

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/email/infrastructure/transport/InMemoryEmailTransport.java \
        api/src/main/resources/application-test.yml
git commit -m "feat(email): add InMemoryEmailTransport and application-test.yml for IT profile"
```

---

### Task 7: `LoggingEmailTransport` + `EmailExecutorConfig`

**Files:**
- Create: `api/src/main/java/com/klasio/email/infrastructure/transport/LoggingEmailTransport.java`
- Create: `api/src/main/java/com/klasio/email/infrastructure/config/EmailExecutorConfig.java`

- [ ] **Step 1: Create `LoggingEmailTransport.java`**

```java
package com.klasio.email.infrastructure.transport;

import com.klasio.email.domain.model.OutboundEmail;
import com.klasio.email.domain.port.EmailTransport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

@Slf4j
@Component
@ConditionalOnProperty(name = "klasio.email.transport", havingValue = "logging")
public class LoggingEmailTransport implements EmailTransport {

    private static final Path PREVIEW_DIR = Path.of("local/email-previews");

    @Override
    public void send(OutboundEmail email) {
        log.info("[EMAIL] type={} to={} subject={} idempotencyKey={}",
                email.type(), email.to().email(), email.subject(), email.idempotencyKey());
        if (email.htmlBody() != null) {
            writePreview(email);
        } else {
            log.info("[EMAIL] Brevo-hosted templateId={} params={}",
                    email.brevoTemplateId(), email.brevoParams());
        }
    }

    private void writePreview(OutboundEmail email) {
        try {
            Files.createDirectories(PREVIEW_DIR);
            String filename = Instant.now().toString().replace(":", "-")
                    + "-" + email.type().name().toLowerCase() + ".html";
            Path file = PREVIEW_DIR.resolve(filename);
            Files.writeString(file, email.htmlBody());
            log.info("[EMAIL] Preview written to {}", file.toAbsolutePath());
        } catch (IOException e) {
            log.warn("[EMAIL] Failed to write preview: {}", e.getMessage());
        }
    }
}
```

- [ ] **Step 2: Create `EmailExecutorConfig.java`**

```java
package com.klasio.email.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class EmailExecutorConfig {

    @Bean("emailListenerExecutor")
    TaskExecutor emailListenerExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(2);
        ex.setMaxPoolSize(8);
        ex.setQueueCapacity(500);
        ex.setThreadNamePrefix("email-listener-");
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.initialize();
        return ex;
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/email/infrastructure/transport/LoggingEmailTransport.java \
        api/src/main/java/com/klasio/email/infrastructure/config/EmailExecutorConfig.java
git commit -m "feat(email): add LoggingEmailTransport with HTML preview and emailListenerExecutor bean"
```

---

### Task 8: `BrevoEmailTransport` + WireMock wire-format integration test

**Files:**
- Create: `api/src/main/java/com/klasio/email/infrastructure/transport/BrevoEmailTransport.java`
- Create: `api/src/test/java/com/klasio/email/infrastructure/transport/BrevoEmailTransportIT.java`

- [ ] **Step 1: Write failing WireMock integration test**

Create `api/src/test/java/com/klasio/email/infrastructure/transport/BrevoEmailTransportIT.java`:
```java
package com.klasio.email.infrastructure.transport;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailType;
import com.klasio.email.domain.model.EmailSender;
import com.klasio.email.domain.model.OutboundEmail;
import com.klasio.email.infrastructure.config.BrevoProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.*;

class BrevoEmailTransportIT {

    static WireMockServer wireMock = new WireMockServer(options().port(9877));

    @BeforeAll static void start() { wireMock.start(); WireMock.configureFor(9877); }
    @AfterAll  static void stop()  { wireMock.stop(); }
    @BeforeEach void reset()       { wireMock.resetAll(); }

    private BrevoEmailTransport transport() {
        BrevoProperties props = new BrevoProperties(
                "test-api-key", "http://localhost:9877", Map.of());
        return new BrevoEmailTransport(props);
    }

    @Test
    void inRepoEmail_sendsHtmlContentAndSubjectWithIdempotencyKey() {
        stubFor(post(urlEqualTo("/smtp/email"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"messageId\":\"<fake@brevo.com>\"}")));

        transport().send(new OutboundEmail(
                EmailType.STUDENT_VERIFICATION,
                new EmailRecipient("user@example.com", "Juan"),
                new EmailSender("noreply@klasio.app", "Test League via Klasio"),
                "Verify your account", "<html>body</html>", "plain text",
                null, null, "idem-key-123"));

        verify(postRequestedFor(urlEqualTo("/smtp/email"))
                .withHeader("api-key", equalTo("test-api-key"))
                .withHeader("Idempotency-Key", equalTo("idem-key-123"))
                .withRequestBody(matchingJsonPath("$.sender.email", equalTo("noreply@klasio.app")))
                .withRequestBody(matchingJsonPath("$.sender.name", equalTo("Test League via Klasio")))
                .withRequestBody(matchingJsonPath("$.to[0].email", equalTo("user@example.com")))
                .withRequestBody(matchingJsonPath("$.subject", equalTo("Verify your account")))
                .withRequestBody(matchingJsonPath("$.htmlContent"))
                .withRequestBody(matchingJsonPath("$.textContent")));

        // Brevo-hosted fields must NOT be present for in-repo emails
        verify(postRequestedFor(urlEqualTo("/smtp/email"))
                .withRequestBody(notMatching(".*\"templateId\".*")));
    }

    @Test
    void brevoHostedEmail_sendsTemplateIdAndParamsNoHtmlContent() {
        stubFor(post(urlEqualTo("/smtp/email"))
                .willReturn(aResponse().withStatus(201)
                        .withBody("{\"messageId\":\"<fake2@brevo.com>\"}")));

        transport().send(new OutboundEmail(
                EmailType.MEMBERSHIP_ACTIVATED,
                new EmailRecipient("student@example.com", "Ana"),
                new EmailSender("noreply@klasio.app", "Tennis Club via Klasio"),
                null, null, null,
                42L, Map.of("studentName", "Ana", "tenantName", "Tennis Club"),
                "idem-key-456"));

        verify(postRequestedFor(urlEqualTo("/smtp/email"))
                .withHeader("Idempotency-Key", equalTo("idem-key-456"))
                .withRequestBody(matchingJsonPath("$.templateId", equalTo(42)))
                .withRequestBody(matchingJsonPath("$.params.studentName", equalTo("Ana")))
                .withRequestBody(notMatching(".*\"htmlContent\".*")));
    }

    @Test
    void on5xxError_retriesAndEventuallySucceeds() {
        stubFor(post(urlEqualTo("/smtp/email"))
                .inScenario("retry").whenScenarioStateIs("STARTED")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("SECOND"));
        stubFor(post(urlEqualTo("/smtp/email"))
                .inScenario("retry").whenScenarioStateIs("SECOND")
                .willReturn(aResponse().withStatus(201)
                        .withBody("{\"messageId\":\"ok\"}")));

        assertThatCode(() -> transport().send(new OutboundEmail(
                EmailType.STUDENT_VERIFICATION,
                new EmailRecipient("u@x.com", "U"),
                new EmailSender("from@x.com", "League via Klasio"),
                "subj", "<h/>", "t", null, null, "idem")))
                .doesNotThrowAnyException();

        verify(2, postRequestedFor(urlEqualTo("/smtp/email")));
    }

    @Test
    void afterAllRetriesExhausted_noExceptionPropagates() {
        stubFor(post(urlEqualTo("/smtp/email"))
                .willReturn(aResponse().withStatus(500)));

        assertThatCode(() -> transport().send(new OutboundEmail(
                EmailType.STUDENT_VERIFICATION,
                new EmailRecipient("u@x.com", "U"),
                new EmailSender("from@x.com", "L via Klasio"),
                "s", "<h/>", "t", null, null, "idem")))
                .doesNotThrowAnyException();
    }
}
```

- [ ] **Step 2: Run test — expect compile failure**

```bash
cd api && mvn test -pl . -Dtest=BrevoEmailTransportIT -q 2>&1 | tail -5
```

- [ ] **Step 3: Create `BrevoEmailTransport.java`**

```java
package com.klasio.email.infrastructure.transport;

import com.klasio.email.domain.model.OutboundEmail;
import com.klasio.email.domain.port.EmailTransport;
import com.klasio.email.infrastructure.config.BrevoProperties;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnProperty(name = "klasio.email.transport", havingValue = "brevo")
public class BrevoEmailTransport implements EmailTransport {

    private final BrevoProperties props;
    private final RestClient restClient;

    public BrevoEmailTransport(BrevoProperties props) {
        this.props = props;
        this.restClient = RestClient.builder()
                .baseUrl(props.apiBaseUrl())
                .defaultHeader("api-key", props.apiKey() != null ? props.apiKey() : "")
                .defaultHeader("Accept", "application/json")
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    @PostConstruct
    void validate() {
        if (props.apiKey() == null || props.apiKey().isBlank()) {
            throw new IllegalStateException(
                    "BREVO_API_KEY is required when klasio.email.transport=brevo");
        }
    }

    @Override
    @Retryable(
            retryFor = {HttpServerErrorException.class,
                        HttpClientErrorException.TooManyRequests.class,
                        ResourceAccessException.class,
                        IOException.class},
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 5, maxDelay = 30000))
    public void send(OutboundEmail email) {
        Map<?, ?> response = restClient.post()
                .uri("/smtp/email")
                .header("Idempotency-Key", email.idempotencyKey())
                .body(buildBody(email))
                .retrieve()
                .body(Map.class);

        String messageId = response != null ? (String) response.get("messageId") : "unknown";
        log.info("[EMAIL] Sent type={} to={} brevoMessageId={}",
                email.type(), email.to().email(), messageId);
    }

    @Recover
    public void onFinalFailure(Exception ex, OutboundEmail email) {
        log.error("[EMAIL] All retries exhausted — type={} to={} error={}",
                email.type(), email.to().email(), ex.getMessage());
    }

    private Map<String, Object> buildBody(OutboundEmail email) {
        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of("name", email.from().displayName(), "email", email.from().email()));
        body.put("to", List.of(Map.of("email", email.to().email(), "name", email.to().displayName())));
        if (email.brevoTemplateId() != null) {
            body.put("templateId", email.brevoTemplateId());
            body.put("params", email.brevoParams() != null ? email.brevoParams() : Map.of());
        } else {
            body.put("subject", email.subject());
            body.put("htmlContent", email.htmlBody());
            if (email.textBody() != null) body.put("textContent", email.textBody());
        }
        return body;
    }
}
```

- [ ] **Step 4: Run WireMock IT — expect PASS**

```bash
cd api && mvn test -pl . -Dtest=BrevoEmailTransportIT -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/email/infrastructure/transport/BrevoEmailTransport.java \
        api/src/test/java/com/klasio/email/infrastructure/transport/BrevoEmailTransportIT.java
git commit -m "feat(email): add BrevoEmailTransport with Spring Retry backoff + WireMock wire-format IT"
```

---

## Phase 2 — In-repo Thymeleaf templates

### Task 9: All 6 email templates + base layout + fallback

**Files:**
- Create/complete all files under `api/src/main/resources/email-templates/`

- [ ] **Step 1: Create `layouts/base.html`**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head><meta charset="UTF-8"></head>
<body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px;margin:0">
  <div style="max-width:600px;margin:0 auto;background:white;padding:30px;border-radius:8px">
    <h2 th:text="${tenantName}" style="color:#2563eb;margin-bottom:24px">League</h2>
    <th:block th:fragment="body"><!-- content here --></th:block>
    <hr style="margin:24px 0;border:none;border-top:1px solid #e5e7eb">
    <p style="color:#9ca3af;font-size:12px;text-align:center">
      Sent by <span th:text="${tenantName}">League</span> via Klasio.
      If you did not expect this email, please ignore it.
    </p>
  </div>
</body>
</html>
```

- [ ] **Step 2: Replace stub `student-verification.html` with full template**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <th:block th:fragment="subject" th:text="|Verify your account at ${tenantName}|"></th:block>
</head>
<body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px">
  <div style="max-width:600px;margin:0 auto;background:white;padding:30px;border-radius:8px">
    <h2 th:text="${tenantName}" style="color:#2563eb">League</h2>
    <p>Hi <strong th:text="${studentName}">Student</strong>,</p>
    <p>Welcome! Please verify your email address to activate your account.</p>
    <p style="margin:24px 0">
      <a th:href="${verificationUrl}"
         style="background:#2563eb;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;display:inline-block">
        Verify Email
      </a>
    </p>
    <p style="color:#6b7280;font-size:14px">This link expires at <span th:text="${expiresAt}"></span>.</p>
    <hr style="margin:24px 0;border:none;border-top:1px solid #e5e7eb">
    <p style="color:#9ca3af;font-size:12px">Sent by <span th:text="${tenantName}"></span> via Klasio.</p>
  </div>
</body>
</html>
```

Replace stub `student-verification.txt` with full template:
```
Hi [(${studentName})],

Welcome to [(${tenantName})]! Please verify your email address to activate your account.

Verification link: [(${verificationUrl})]

This link expires at [(${expiresAt})].

If you did not create an account, please ignore this email.

Sent by [(${tenantName})] via Klasio.
```

- [ ] **Step 3: Replace stub `password-recovery.html` with full template**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <th:block th:fragment="subject" th:text="|Reset your ${tenantName} password|"></th:block>
</head>
<body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px">
  <div style="max-width:600px;margin:0 auto;background:white;padding:30px;border-radius:8px">
    <h2 th:text="${tenantName}" style="color:#2563eb">League</h2>
    <p>You requested a password reset for your account.</p>
    <p style="margin:24px 0">
      <a th:href="${resetUrl}"
         style="background:#2563eb;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;display:inline-block">
        Reset Password
      </a>
    </p>
    <p style="color:#6b7280;font-size:14px">This link expires in <span th:text="${expiresAt}"></span>.</p>
    <p style="color:#9ca3af;font-size:13px">If you did not request a password reset, please ignore this email.</p>
    <hr style="margin:24px 0;border:none;border-top:1px solid #e5e7eb">
    <p style="color:#9ca3af;font-size:12px">Sent by <span th:text="${tenantName}"></span> via Klasio.</p>
  </div>
</body>
</html>
```

Replace stub `password-recovery.txt`:
```
You requested a password reset for your [(${tenantName})] account.

Reset link: [(${resetUrl})]

This link expires in [(${expiresAt})].

If you did not request this, please ignore this email.

Sent by [(${tenantName})] via Klasio.
```

- [ ] **Step 4: Create `professor-invitation.html` and `.txt`**

`professor-invitation.html`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <th:block th:fragment="subject" th:text="|You have been invited to join ${tenantName} as a professor|"></th:block>
</head>
<body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px">
  <div style="max-width:600px;margin:0 auto;background:white;padding:30px;border-radius:8px">
    <h2 th:text="${tenantName}" style="color:#2563eb">League</h2>
    <p>Hi <strong th:text="${professorName}">Professor</strong>,</p>
    <p>You have been invited to join <span th:text="${tenantName}"></span> as a professor on Klasio.</p>
    <p style="margin:24px 0">
      <a th:href="${activationUrl}"
         style="background:#2563eb;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;display:inline-block">
        Accept Invitation
      </a>
    </p>
    <p style="color:#6b7280;font-size:14px">This invitation expires at <span th:text="${expiresAt}"></span>.</p>
    <hr style="margin:24px 0;border:none;border-top:1px solid #e5e7eb">
    <p style="color:#9ca3af;font-size:12px">Sent by <span th:text="${tenantName}"></span> via Klasio.</p>
  </div>
</body>
</html>
```

`professor-invitation.txt`:
```
Hi [(${professorName})],

You have been invited to join [(${tenantName})] as a professor on Klasio.

Accept your invitation: [(${activationUrl})]

This invitation expires at [(${expiresAt})].

Sent by [(${tenantName})] via Klasio.
```

- [ ] **Step 5: Create `payment-proof-uploaded.html` and `.txt`**

`payment-proof-uploaded.html`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <th:block th:fragment="subject" th:text="|New payment proof submitted by ${studentName}|"></th:block>
</head>
<body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px">
  <div style="max-width:600px;margin:0 auto;background:white;padding:30px;border-radius:8px">
    <h2 th:text="${tenantName}" style="color:#2563eb">League</h2>
    <p><strong th:text="${studentName}">Student</strong> has submitted a payment proof for the
       <strong th:text="${programName}">program</strong> program.</p>
    <p style="margin:24px 0">
      <a th:href="${reviewUrl}"
         style="background:#2563eb;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;display:inline-block">
        Review Proof
      </a>
    </p>
    <hr style="margin:24px 0;border:none;border-top:1px solid #e5e7eb">
    <p style="color:#9ca3af;font-size:12px">Sent by <span th:text="${tenantName}"></span> via Klasio.</p>
  </div>
</body>
</html>
```

`payment-proof-uploaded.txt`:
```
[(${studentName})] has submitted a payment proof for the [(${programName})] program.

Review the proof: [(${reviewUrl})]

Sent by [(${tenantName})] via Klasio.
```

- [ ] **Step 6: Create `payment-rejected.html` and `.txt`**

`payment-rejected.html`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <th:block th:fragment="subject" th:text="|Your payment proof for ${programName} was not accepted|"></th:block>
</head>
<body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px">
  <div style="max-width:600px;margin:0 auto;background:white;padding:30px;border-radius:8px">
    <h2 th:text="${tenantName}" style="color:#2563eb">League</h2>
    <p>Hi <strong th:text="${studentName}">Student</strong>,</p>
    <p>Your payment proof for the <strong th:text="${programName}">program</strong> program was not accepted.</p>
    <p><strong>Reason:</strong> <span th:text="${reason}">reason</span></p>
    <p style="margin:24px 0">
      <a th:href="${retryUrl}"
         style="background:#2563eb;color:white;padding:12px 24px;text-decoration:none;border-radius:6px;display:inline-block">
        Submit Again
      </a>
    </p>
    <hr style="margin:24px 0;border:none;border-top:1px solid #e5e7eb">
    <p style="color:#9ca3af;font-size:12px">Sent by <span th:text="${tenantName}"></span> via Klasio.</p>
  </div>
</body>
</html>
```

`payment-rejected.txt`:
```
Hi [(${studentName})],

Your payment proof for the [(${programName})] program was not accepted.

Reason: [(${reason})]

Please try again: [(${retryUrl})]

Sent by [(${tenantName})] via Klasio.
```

- [ ] **Step 7: Create `missing-template-fallback.html` and `.txt`**

`missing-template-fallback.html`:
```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<head>
  <meta charset="UTF-8">
  <th:block th:fragment="subject" th:text="|Notification from ${tenantName}|"></th:block>
</head>
<body style="font-family:Arial,sans-serif;background:#f4f4f4;padding:20px">
  <div style="max-width:600px;margin:0 auto;background:white;padding:30px;border-radius:8px">
    <h2 th:text="${tenantName}" style="color:#2563eb">League</h2>
    <p>You have a new notification from <span th:text="${tenantName}"></span>.</p>
    <p style="color:#6b7280;font-size:12px">(Email template not yet configured for: <span th:text="${emailTypeName}"></span>)</p>
    <hr style="margin:24px 0;border:none;border-top:1px solid #e5e7eb">
    <p style="color:#9ca3af;font-size:12px">Sent by <span th:text="${tenantName}"></span> via Klasio.</p>
  </div>
</body>
</html>
```

`missing-template-fallback.txt`:
```
You have a new notification from [(${tenantName})].

(Email template not configured for: [(${emailTypeName})])

Sent by [(${tenantName})] via Klasio.
```

- [ ] **Step 8: Run all template renderer tests to confirm no unresolved variables**

```bash
cd api && mvn test -pl . -Dtest=ThymeleafTemplateRendererTest -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

- [ ] **Step 9: Commit**

```bash
git add api/src/main/resources/email-templates/
git commit -m "feat(email): add all 6 in-repo Thymeleaf email templates + base layout + missing-template-fallback"
```

---

## Phase 3 — Auth migration

### Task 10: `FrontendUrlBuilder`

**Files:**
- Create: `api/src/main/java/com/klasio/shared/infrastructure/web/FrontendUrlBuilder.java`
- Create: `api/src/test/java/com/klasio/shared/infrastructure/web/FrontendUrlBuilderTest.java`

- [ ] **Step 1: Write failing test**

```java
package com.klasio.shared.infrastructure.web;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class FrontendUrlBuilderTest {

    @Test
    void withSubdomainTemplate_insertsSlugAsSubdomain() {
        FrontendUrlBuilder builder = new FrontendUrlBuilder("https://{tenantSlug}.app.klasio.com");
        assertThat(builder.build("liga-valle-tenis", "/verify-email?token=abc"))
                .isEqualTo("https://liga-valle-tenis.app.klasio.com/verify-email?token=abc");
    }

    @Test
    void withLocalTemplate_noPlaceholder_appendsPath() {
        FrontendUrlBuilder builder = new FrontendUrlBuilder("http://localhost:3000");
        assertThat(builder.build("any-slug", "/reset-password?token=xyz"))
                .isEqualTo("http://localhost:3000/reset-password?token=xyz");
    }

    @Test
    void pathWithoutLeadingSlash_isHandled() {
        FrontendUrlBuilder builder = new FrontendUrlBuilder("http://localhost:3000");
        assertThat(builder.build("slug", "verify"))
                .isEqualTo("http://localhost:3000/verify");
    }
}
```

- [ ] **Step 2: Run test — expect compile failure**

```bash
cd api && mvn test -pl . -Dtest=FrontendUrlBuilderTest -q 2>&1 | tail -5
```

- [ ] **Step 3: Create `FrontendUrlBuilder.java`**

```java
package com.klasio.shared.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FrontendUrlBuilder {

    private final String urlTemplate;

    public FrontendUrlBuilder(
            @Value("${klasio.frontend.url-template:http://localhost:3000}") String urlTemplate) {
        this.urlTemplate = urlTemplate;
    }

    public String build(String tenantSlug, String path) {
        String base = urlTemplate.contains("{tenantSlug}")
                ? urlTemplate.replace("{tenantSlug}", tenantSlug)
                : urlTemplate;
        return base + (path.startsWith("/") ? path : "/" + path);
    }
}
```

- [ ] **Step 4: Run test — expect PASS**

```bash
cd api && mvn test -pl . -Dtest=FrontendUrlBuilderTest -q 2>&1 | tail -5
```

Expected: `BUILD SUCCESS`

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/shared/infrastructure/web/FrontendUrlBuilder.java \
        api/src/test/java/com/klasio/shared/infrastructure/web/FrontendUrlBuilderTest.java
git commit -m "feat(shared): add FrontendUrlBuilder with subdomain-per-tenant support"
```

---

### Task 11: Extend auth domain events + new `VerificationEmailResendRequested`

**Files:**
- Modify: `api/src/main/java/com/klasio/auth/domain/event/StudentRegisteredEvent.java`
- Modify: `api/src/main/java/com/klasio/auth/domain/event/PasswordResetRequestedEvent.java`
- Create: `api/src/main/java/com/klasio/auth/domain/event/VerificationEmailResendRequested.java`

- [ ] **Step 1: Extend `StudentRegisteredEvent` with email fields**

Current record:
```java
public record StudentRegisteredEvent(UUID userId, UUID tenantId, UUID studentId, String email, Instant occurredAt)
```

Replace with:
```java
package com.klasio.auth.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record StudentRegisteredEvent(
        UUID userId,
        UUID tenantId,
        UUID studentId,
        String email,
        String rawToken,
        Instant expiresAt,
        String displayName,
        Instant occurredAt
) implements DomainEvent {}
```

- [ ] **Step 2: Extend `PasswordResetRequestedEvent` with token fields**

Current record:
```java
public record PasswordResetRequestedEvent(UUID userId, UUID tenantId, String email, Instant occurredAt)
```

Replace with:
```java
package com.klasio.auth.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record PasswordResetRequestedEvent(
        UUID userId,
        UUID tenantId,
        String email,
        String rawToken,
        Instant expiresAt,
        Instant occurredAt
) implements DomainEvent {}
```

- [ ] **Step 3: Create `VerificationEmailResendRequested.java`**

```java
package com.klasio.auth.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.util.UUID;

public record VerificationEmailResendRequested(
        UUID userId,
        UUID tenantId,
        String email,
        String tenantSlug,
        String rawToken,
        Instant expiresAt,
        Instant occurredAt
) implements DomainEvent {}
```

- [ ] **Step 4: Run compilation to see which call sites are now broken**

```bash
cd api && mvn compile -q 2>&1 | grep "ERROR" | head -20
```

Expected: compile errors in `RegisterStudentService`, `RequestPasswordResetService`, `ResendVerificationEmailService` and possibly their tests — will be fixed in Task 12.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/auth/domain/event/
git commit -m "feat(email): extend auth events with rawToken+expiresAt; add VerificationEmailResendRequested"
```

---

### Task 12: Migrate auth use cases + `AuthEmailListener` + delete old auth email infra

**Files:**
- Modify: `api/src/main/java/com/klasio/auth/application/service/RegisterStudentService.java`
- Modify: `api/src/main/java/com/klasio/auth/application/service/RequestPasswordResetService.java`
- Modify: `api/src/main/java/com/klasio/auth/application/service/ResendVerificationEmailService.java`
- Create: `api/src/main/java/com/klasio/auth/infrastructure/notification/AuthEmailListener.java`
- Delete: `api/src/main/java/com/klasio/auth/application/port/AuthEmailSender.java`
- Delete: `api/src/main/java/com/klasio/auth/infrastructure/mail/PostmarkAuthEmailSender.java`

- [ ] **Step 1: Update `RegisterStudentService` — remove `authEmailSender`, enrich event**

Remove `AuthEmailSender authEmailSender` field and constructor param.
Change line 98 (direct send call) and lines 100-101 (event publish) to:
```java
// Remove: authEmailSender.sendVerificationEmail(command.email(), rawToken, command.tenantSlug());

// Update the event to include token fields and displayName:
eventPublisher.publishEvent(new StudentRegisteredEvent(
        user.getId(), tenantId, studentId, command.email(),
        rawToken, expiresAt,
        command.firstName() + " " + command.lastName(),
        Instant.now()));
```

Full updated constructor (remove `AuthEmailSender` parameter):
```java
public RegisterStudentService(UserRepository userRepository,
                              StudentProfilePort studentProfilePort,
                              TenantResolverPort tenantResolverPort,
                              PasswordEncoder passwordEncoder,
                              TokenGenerator tokenGenerator,
                              EmailVerificationTokenRepository evtRepository,
                              AuthProperties authProperties,
                              ApplicationEventPublisher eventPublisher) {
```

- [ ] **Step 2: Update `RequestPasswordResetService` — remove `authEmailSender`, enrich event**

Remove `AuthEmailSender authEmailSender` field and constructor param.
Change lines 59-62:
```java
// Remove: authEmailSender.sendPasswordResetEmail(email, rawToken);

// Update event to include token fields:
eventPublisher.publishEvent(new PasswordResetRequestedEvent(
        user.getId(), user.getTenantId(), email,
        rawToken, expiresAt,
        Instant.now()));
```

Full updated constructor:
```java
public RequestPasswordResetService(UserRepository userRepository,
                                   PasswordResetTokenRepository prtRepository,
                                   TokenGenerator tokenGenerator,
                                   AuthProperties authProperties,
                                   ApplicationEventPublisher eventPublisher) {
```

- [ ] **Step 3: Update `ResendVerificationEmailService` — remove `authEmailSender`, add event publisher**

Remove `AuthEmailSender authEmailSender` field and constructor param.
Add `ApplicationEventPublisher eventPublisher` field and constructor param.
Replace line 62 (`authEmailSender.sendVerificationEmail(...)`) with:
```java
eventPublisher.publishEvent(new VerificationEmailResendRequested(
        user.getId(), user.getTenantId(), email, tenantSlug,
        rawToken, expiresAt,
        Instant.now()));
```

Full updated class structure:
```java
@Service
public class ResendVerificationEmailService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository evtRepository;
    private final TokenGenerator tokenGenerator;
    private final AuthProperties authProperties;
    private final ApplicationEventPublisher eventPublisher;

    public ResendVerificationEmailService(UserRepository userRepository,
                                          EmailVerificationTokenRepository evtRepository,
                                          TokenGenerator tokenGenerator,
                                          AuthProperties authProperties,
                                          ApplicationEventPublisher eventPublisher) {
        this.userRepository = userRepository;
        this.evtRepository = evtRepository;
        this.tokenGenerator = tokenGenerator;
        this.authProperties = authProperties;
        this.eventPublisher = eventPublisher;
    }
    // resend() method — same logic but publish event instead of direct email send
```

- [ ] **Step 4: Delete `AuthEmailSender.java` and `PostmarkAuthEmailSender.java`**

```bash
rm api/src/main/java/com/klasio/auth/application/port/AuthEmailSender.java
rm api/src/main/java/com/klasio/auth/infrastructure/mail/PostmarkAuthEmailSender.java
```

Also remove the `mail/` directory if empty:
```bash
rmdir api/src/main/java/com/klasio/auth/infrastructure/mail/ 2>/dev/null || true
```

- [ ] **Step 5: Create `AuthEmailListener.java`**

```java
package com.klasio.auth.infrastructure.notification;

import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.auth.domain.event.PasswordResetRequestedEvent;
import com.klasio.auth.domain.event.StudentRegisteredEvent;
import com.klasio.auth.domain.event.VerificationEmailResendRequested;
import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.shared.infrastructure.web.FrontendUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Map;

@Slf4j
@Component
public class AuthEmailListener {

    private final EmailService emailService;
    private final FrontendUrlBuilder urlBuilder;
    private final TenantResolverPort tenantResolverPort;

    public AuthEmailListener(EmailService emailService,
                             FrontendUrlBuilder urlBuilder,
                             TenantResolverPort tenantResolverPort) {
        this.emailService = emailService;
        this.urlBuilder = urlBuilder;
        this.tenantResolverPort = tenantResolverPort;
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStudentRegistered(StudentRegisteredEvent e) {
        String tenantSlug = tenantResolverPort.resolveSlugByTenantId(e.tenantId())
                .orElse("app");
        String verificationUrl = urlBuilder.build(tenantSlug,
                "/verify-email?token=" + e.rawToken());
        emailService.send(
                EmailType.STUDENT_VERIFICATION,
                new EmailRecipient(e.email(), e.displayName()),
                e.tenantId(),
                Map.of("studentName", e.displayName(),
                       "verificationUrl", verificationUrl,
                       "expiresAt", e.expiresAt().toString()));
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent e) {
        String tenantSlug = tenantResolverPort.resolveSlugByTenantId(e.tenantId())
                .orElse("app");
        String resetUrl = urlBuilder.build(tenantSlug,
                "/reset-password?token=" + e.rawToken());
        emailService.send(
                EmailType.PASSWORD_RECOVERY,
                new EmailRecipient(e.email(), e.email()),
                e.tenantId(),
                Map.of("resetUrl", resetUrl,
                       "expiresAt", e.expiresAt().toString()));
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onVerificationResendRequested(VerificationEmailResendRequested e) {
        String verificationUrl = urlBuilder.build(e.tenantSlug(),
                "/verify-email?token=" + e.rawToken());
        emailService.send(
                EmailType.STUDENT_VERIFICATION,
                new EmailRecipient(e.email(), e.email()),
                e.tenantId(),
                Map.of("studentName", e.email(),
                       "verificationUrl", verificationUrl,
                       "expiresAt", e.expiresAt().toString()));
    }
}
```

**Note:** `TenantResolverPort.resolveSlugByTenantId(UUID)` may not exist yet — check `TenantResolverPort`. If missing, add the method: `Optional<String> resolveSlugByTenantId(UUID tenantId)` and implement via a JPQL query on `TenantJpaEntity.slug`. If `TenantNamePort.findNamesByIds()` adapter already queries the tenant table, reuse that adapter or add the method to it.

- [ ] **Step 6: Compile to verify all errors are gone**

```bash
cd api && mvn compile -q 2>&1 | grep "ERROR" | head -20
```

Expected: no errors.

- [ ] **Step 7: Run existing auth service tests and fix any broken assertions**

```bash
cd api && mvn test -pl . -Dtest="RegisterStudentServiceTest,RequestPasswordResetServiceTest,ResendVerificationEmailServiceTest" -q 2>&1 | tail -10
```

For tests that used `verify(authEmailSender).*`: delete those assertions; replace with `verify(eventPublisher).publishEvent(any(StudentRegisteredEvent.class))` or `any(PasswordResetRequestedEvent.class)`. Also fix constructor call in test setUp — remove the `authEmailSender` mock.

- [ ] **Step 8: Run full test suite to check for regressions**

```bash
cd api && mvn test -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 9: Commit**

```bash
git add api/src/main/java/com/klasio/auth/ \
        api/src/test/java/com/klasio/auth/
git commit -m "feat(email): migrate auth email path to events — delete AuthEmailSender, wire AuthEmailListener"
```

---

## Phase 4 — Cross-module listener wiring

### Task 13: `StudentEmailPort` + `TenantAdminEmailPort` + adapters

**Files:**
- Create: `api/src/main/java/com/klasio/membership/domain/port/StudentEmailPort.java`
- Create: `api/src/main/java/com/klasio/membership/domain/port/TenantAdminEmailPort.java`
- Create: `api/src/main/java/com/klasio/membership/infrastructure/persistence/StudentEmailAdapter.java`
- Create: `api/src/main/java/com/klasio/membership/infrastructure/persistence/TenantAdminEmailAdapter.java`
- Create: `api/src/main/java/com/klasio/attendance/domain/port/StudentEmailPort.java`
- Create: `api/src/main/java/com/klasio/attendance/infrastructure/persistence/AttendanceStudentEmailAdapter.java`

- [ ] **Step 1: Create `com.klasio.membership.domain.port.StudentEmailPort`**

```java
package com.klasio.membership.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface StudentEmailPort {
    Optional<String> findEmail(UUID studentId, UUID tenantId);
}
```

- [ ] **Step 2: Create `com.klasio.membership.domain.port.TenantAdminEmailPort`**

```java
package com.klasio.membership.domain.port;

import java.util.List;
import java.util.UUID;

public interface TenantAdminEmailPort {
    List<String> findAdminEmails(UUID tenantId);
}
```

- [ ] **Step 3: Create `StudentEmailAdapter.java` (membership module)**

Follows the same pattern as `StudentNameAdapter` — queries `StudentJpaEntity` via `EntityManager`:
```java
package com.klasio.membership.infrastructure.persistence;

import com.klasio.membership.domain.port.StudentEmailPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class StudentEmailAdapter implements StudentEmailPort {

    private final EntityManager em;

    public StudentEmailAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<String> findEmail(UUID studentId, UUID tenantId) {
        @SuppressWarnings("unchecked")
        List<String> rows = em.createQuery(
                        "SELECT s.email FROM StudentJpaEntity s WHERE s.id = :id AND s.tenantId = :tenantId")
                .setParameter("id", studentId)
                .setParameter("tenantId", tenantId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
```

- [ ] **Step 4: Create `TenantAdminEmailAdapter.java` (membership module)**

Queries `UserJpaEntity` for users with `Role.ADMIN` in the given tenant:
```java
package com.klasio.membership.infrastructure.persistence;

import com.klasio.auth.domain.model.Role;
import com.klasio.membership.domain.port.TenantAdminEmailPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class TenantAdminEmailAdapter implements TenantAdminEmailPort {

    private final EntityManager em;

    public TenantAdminEmailAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> findAdminEmails(UUID tenantId) {
        return em.createQuery(
                        "SELECT u.email FROM UserJpaEntity u WHERE u.tenantId = :tenantId AND :role MEMBER OF u.roles")
                .setParameter("tenantId", tenantId)
                .setParameter("role", Role.ADMIN)
                .getResultList();
    }
}
```

- [ ] **Step 5: Create `com.klasio.attendance.domain.port.StudentEmailPort`**

```java
package com.klasio.attendance.domain.port;

import java.util.Optional;
import java.util.UUID;

public interface StudentEmailPort {
    Optional<String> findEmail(UUID studentId, UUID tenantId);
}
```

- [ ] **Step 6: Create `AttendanceStudentEmailAdapter.java`**

```java
package com.klasio.attendance.infrastructure.persistence;

import com.klasio.attendance.domain.port.StudentEmailPort;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class AttendanceStudentEmailAdapter implements StudentEmailPort {

    private final EntityManager em;

    public AttendanceStudentEmailAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public Optional<String> findEmail(UUID studentId, UUID tenantId) {
        @SuppressWarnings("unchecked")
        List<String> rows = em.createQuery(
                        "SELECT s.email FROM StudentJpaEntity s WHERE s.id = :id AND s.tenantId = :tenantId")
                .setParameter("id", studentId)
                .setParameter("tenantId", tenantId)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(rows.get(0));
    }
}
```

- [ ] **Step 7: Compile to verify**

```bash
cd api && mvn compile -q 2>&1 | grep "ERROR" | head -10
```

Expected: no errors.

- [ ] **Step 8: Commit**

```bash
git add api/src/main/java/com/klasio/membership/domain/port/StudentEmailPort.java \
        api/src/main/java/com/klasio/membership/domain/port/TenantAdminEmailPort.java \
        api/src/main/java/com/klasio/membership/infrastructure/persistence/StudentEmailAdapter.java \
        api/src/main/java/com/klasio/membership/infrastructure/persistence/TenantAdminEmailAdapter.java \
        api/src/main/java/com/klasio/attendance/domain/port/StudentEmailPort.java \
        api/src/main/java/com/klasio/attendance/infrastructure/persistence/AttendanceStudentEmailAdapter.java
git commit -m "feat(email): add StudentEmailPort + TenantAdminEmailPort with JPA adapters for membership and attendance"
```

---

### Task 14: Extend `MembershipActivated`/`ExpiryWarning` events + wire `MembershipNotificationListener`

**Files:**
- Modify: `api/src/main/java/com/klasio/membership/domain/event/MembershipActivated.java`
- Modify: `api/src/main/java/com/klasio/membership/domain/event/MembershipExpiryWarning.java`
- Modify: `api/src/main/java/com/klasio/membership/domain/model/Membership.java`
- Modify: `api/src/main/java/com/klasio/membership/infrastructure/scheduler/MembershipExpirationJob.java`
- Modify: `api/src/main/java/com/klasio/membership/infrastructure/notification/MembershipNotificationListener.java`

- [ ] **Step 1: Extend `MembershipActivated` event with display fields**

```java
package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MembershipActivated(
        UUID membershipId,
        UUID tenantId,
        UUID studentId,
        UUID programId,
        UUID actorId,
        String planName,
        int totalHours,
        LocalDate expirationDate,
        Instant occurredAt
) implements DomainEvent {}
```

- [ ] **Step 2: Extend `MembershipExpiryWarning` with `remainingHours`**

```java
package com.klasio.membership.domain.event;

import com.klasio.shared.domain.DomainEvent;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record MembershipExpiryWarning(
        UUID membershipId,
        UUID tenantId,
        UUID studentId,
        UUID programId,
        LocalDate expirationDate,
        int remainingHours,
        Instant occurredAt
) implements DomainEvent {}
```

- [ ] **Step 3: Update `Membership.java` — pass new fields to `MembershipActivated`**

In `validatePayment()` (line ~224), update event construction:
```java
domainEvents.add(new MembershipActivated(
        id.value(), tenantId, studentId, programId, validatedBy,
        planName, purchasedHours, expirationDate,
        now));
```

In `activate()` (line ~247), update event construction:
```java
domainEvents.add(new MembershipActivated(
        id.value(), tenantId, studentId, programId, activatedBy,
        planName, purchasedHours, expirationDate,
        now));
```

- [ ] **Step 4: Update `MembershipExpirationJob` — add `remainingHours` to warning event**

In `sendExpiryWarnings()` (line ~71), update event construction:
```java
MembershipExpiryWarning warning = new MembershipExpiryWarning(
        membership.getId().value(),
        membership.getTenantId(),
        membership.getStudentId(),
        membership.getProgramId(),
        membership.getExpirationDate(),
        membership.getAvailableHours(), // NEW
        Instant.now());
```

- [ ] **Step 5: Compile to surface all broken sites**

```bash
cd api && mvn compile -q 2>&1 | grep "ERROR" | head -20
```

Fix any remaining constructor call sites for `MembershipActivated` or `MembershipExpiryWarning` (e.g., in test files or other listeners).

- [ ] **Step 6: Wire `MembershipNotificationListener` (replace stubs with real email calls)**

Replace the entire file:
```java
package com.klasio.membership.infrastructure.notification;

import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.membership.domain.event.MembershipActivated;
import com.klasio.membership.domain.event.MembershipDepleted;
import com.klasio.membership.domain.event.MembershipExpiryWarning;
import com.klasio.membership.domain.port.ProgramNamePort;
import com.klasio.membership.domain.port.StudentEmailPort;
import com.klasio.membership.domain.port.StudentNamePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Map;

@Slf4j
@Component
public class MembershipNotificationListener {

    private final EmailService emailService;
    private final StudentEmailPort studentEmailPort;
    private final StudentNamePort studentNamePort;
    private final ProgramNamePort programNamePort;

    public MembershipNotificationListener(EmailService emailService,
                                          StudentEmailPort studentEmailPort,
                                          StudentNamePort studentNamePort,
                                          ProgramNamePort programNamePort) {
        this.emailService = emailService;
        this.studentEmailPort = studentEmailPort;
        this.studentNamePort = studentNamePort;
        this.programNamePort = programNamePort;
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMembershipActivated(MembershipActivated event) {
        String email = studentEmailPort.findEmail(event.studentId(), event.tenantId()).orElse(null);
        if (email == null) return;
        String name = studentNamePort.findFullName(event.studentId(), event.tenantId()).orElse(email);
        String program = programNamePort.findName(event.programId(), event.tenantId()).orElse("your program");

        emailService.send(
                EmailType.MEMBERSHIP_ACTIVATED,
                new EmailRecipient(email, name),
                event.tenantId(),
                Map.of("studentName", name,
                       "programName", program,
                       "planName", event.planName(),
                       "totalHours", event.totalHours(),
                       "expiresAt", event.expirationDate().toString()));
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMembershipExpiryWarning(MembershipExpiryWarning event) {
        String email = studentEmailPort.findEmail(event.studentId(), event.tenantId()).orElse(null);
        if (email == null) return;
        String name = studentNamePort.findFullName(event.studentId(), event.tenantId()).orElse(email);
        String program = programNamePort.findName(event.programId(), event.tenantId()).orElse("your program");

        emailService.send(
                EmailType.MEMBERSHIP_EXPIRY_WARNING,
                new EmailRecipient(email, name),
                event.tenantId(),
                Map.of("studentName", name,
                       "programName", program,
                       "expiresAt", event.expirationDate().toString(),
                       "remainingHours", event.remainingHours()));
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMembershipDepleted(MembershipDepleted event) {
        String email = studentEmailPort.findEmail(event.studentId(), event.tenantId()).orElse(null);
        if (email == null) return;
        String name = studentNamePort.findFullName(event.studentId(), event.tenantId()).orElse(email);
        String program = programNamePort.findName(event.programId(), event.tenantId()).orElse("your program");

        emailService.send(
                EmailType.MEMBERSHIP_DEPLETED,
                new EmailRecipient(email, name),
                event.tenantId(),
                Map.of("studentName", name, "programName", program));
    }
}
```

- [ ] **Step 7: Compile + run existing membership tests**

```bash
cd api && mvn test -pl . -Dtest="MembershipNotificationListenerTest,ActivateMembershipServiceTest" -q 2>&1 | tail -10
```

Fix any broken assertions in existing tests (constructor changes to `MembershipActivated`).

- [ ] **Step 8: Commit**

```bash
git add api/src/main/java/com/klasio/membership/
git commit -m "feat(email): wire MembershipNotificationListener — activate/expiry-warning/depleted emails"
```

---

### Task 15: Wire `PaymentProofNotificationListener`

**Files:**
- Modify: `api/src/main/java/com/klasio/membership/infrastructure/notification/PaymentProofNotificationListener.java`

- [ ] **Step 1: Wire `PaymentProofNotificationListener` with real email calls**

Replace the entire file:
```java
package com.klasio.membership.infrastructure.notification;

import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.membership.domain.event.PaymentProofRejected;
import com.klasio.membership.domain.event.PaymentProofUploaded;
import com.klasio.membership.domain.port.MembershipPlanSnapshotPort;
import com.klasio.membership.domain.port.StudentEmailPort;
import com.klasio.membership.domain.port.StudentNamePort;
import com.klasio.membership.domain.port.TenantAdminEmailPort;
import com.klasio.shared.infrastructure.web.FrontendUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Map;

@Slf4j
@Component
public class PaymentProofNotificationListener {

    private final EmailService emailService;
    private final StudentEmailPort studentEmailPort;
    private final StudentNamePort studentNamePort;
    private final TenantAdminEmailPort tenantAdminEmailPort;
    private final MembershipPlanSnapshotPort planSnapshotPort;
    private final FrontendUrlBuilder urlBuilder;

    public PaymentProofNotificationListener(EmailService emailService,
                                            StudentEmailPort studentEmailPort,
                                            StudentNamePort studentNamePort,
                                            TenantAdminEmailPort tenantAdminEmailPort,
                                            MembershipPlanSnapshotPort planSnapshotPort,
                                            FrontendUrlBuilder urlBuilder) {
        this.emailService = emailService;
        this.studentEmailPort = studentEmailPort;
        this.studentNamePort = studentNamePort;
        this.tenantAdminEmailPort = tenantAdminEmailPort;
        this.planSnapshotPort = planSnapshotPort;
        this.urlBuilder = urlBuilder;
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentProofUploaded(PaymentProofUploaded event) {
        String studentName = studentNamePort.findFullName(event.studentId(), event.tenantId())
                .orElse("A student");
        String programName = planSnapshotPort.findSnapshot(event.membershipId(), event.tenantId())
                .map(MembershipPlanSnapshotPort.PlanSnapshot::programName).orElse("their program");
        String reviewUrl = urlBuilder.build("app", "/payment-proofs");

        for (String adminEmail : tenantAdminEmailPort.findAdminEmails(event.tenantId())) {
            emailService.send(
                    EmailType.PAYMENT_PROOF_UPLOADED,
                    new EmailRecipient(adminEmail, adminEmail),
                    event.tenantId(),
                    Map.of("studentName", studentName,
                           "programName", programName,
                           "reviewUrl", reviewUrl));
        }
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPaymentProofRejected(PaymentProofRejected event) {
        String email = studentEmailPort.findEmail(event.studentId(), event.tenantId()).orElse(null);
        if (email == null) return;
        String name = studentNamePort.findFullName(event.studentId(), event.tenantId()).orElse(email);
        String programName = planSnapshotPort.findSnapshot(event.membershipId(), event.tenantId())
                .map(MembershipPlanSnapshotPort.PlanSnapshot::programName).orElse("your program");
        String retryUrl = urlBuilder.build("app", "/memberships");

        emailService.send(
                EmailType.PAYMENT_REJECTED,
                new EmailRecipient(email, name),
                event.tenantId(),
                Map.of("studentName", name,
                       "programName", programName,
                       "reason", event.rejectionReason(),
                       "retryUrl", retryUrl));
    }
}
```

**Note:** `onPaymentProofApproved` and `onDelegationReminderDue` stubs are removed — `APPROVED` is handled by `MembershipNotificationListener.onMembershipActivated`; `DelegationReminderDue` email is deferred (no `EmailType` defined for it in v1.0).

- [ ] **Step 2: Compile + run tests**

```bash
cd api && mvn compile -q && mvn test -pl . -Dtest="PaymentProofNotificationListenerTest" -q 2>&1 | tail -10
```

Fix any broken tests.

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/membership/infrastructure/notification/PaymentProofNotificationListener.java
git commit -m "feat(email): wire PaymentProofNotificationListener — proof-uploaded to admins, rejected to student"
```

---

### Task 16: `ProfessorCreated` event extension + `ProfessorInvitationEmailListener`

**Files:**
- Modify: `api/src/main/java/com/klasio/professor/domain/event/ProfessorCreated.java`
- Modify: `api/src/main/java/com/klasio/professor/domain/model/Professor.java`
- Create: `api/src/main/java/com/klasio/professor/infrastructure/notification/ProfessorInvitationEmailListener.java`

- [ ] **Step 1: Add `invitationExpiresAt` to `ProfessorCreated` event**

```java
package com.klasio.professor.domain.event;

import com.klasio.shared.domain.DomainEvent;
import com.klasio.shared.domain.model.IdentityDocumentType;
import java.time.Instant;
import java.util.UUID;

public record ProfessorCreated(
        UUID professorId,
        UUID tenantId,
        String firstName,
        String lastName,
        String email,
        String phoneNumber,
        IdentityDocumentType identityDocumentType,
        String identityNumber,
        UUID invitationToken,
        UUID createdBy,
        Instant invitationExpiresAt,
        Instant occurredAt
) implements DomainEvent {}
```

- [ ] **Step 2: Update `Professor.create()` to pass `expiresAt` to the event**

In `Professor.java` lines 112-114, update event construction:
```java
professor.domainEvents.add(new ProfessorCreated(
        id.value(), tenantId, normalizedFirstName, normalizedLastName, normalizedEmail, phoneNumber,
        identityDocumentType, normalizedIdentityNumber, token, createdBy,
        expiresAt, // NEW — already computed at line 103
        now));
```

- [ ] **Step 3: Create `ProfessorInvitationEmailListener.java`**

```java
package com.klasio.professor.infrastructure.notification;

import com.klasio.auth.application.port.TenantResolverPort;
import com.klasio.email.application.EmailRecipient;
import com.klasio.email.application.EmailService;
import com.klasio.email.application.EmailType;
import com.klasio.professor.domain.event.ProfessorCreated;
import com.klasio.shared.infrastructure.web.FrontendUrlBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import java.util.Map;

@Slf4j
@Component
public class ProfessorInvitationEmailListener {

    private final EmailService emailService;
    private final FrontendUrlBuilder urlBuilder;
    private final TenantResolverPort tenantResolverPort;

    public ProfessorInvitationEmailListener(EmailService emailService,
                                            FrontendUrlBuilder urlBuilder,
                                            TenantResolverPort tenantResolverPort) {
        this.emailService = emailService;
        this.urlBuilder = urlBuilder;
        this.tenantResolverPort = tenantResolverPort;
    }

    @Async("emailListenerExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProfessorCreated(ProfessorCreated event) {
        String tenantSlug = tenantResolverPort.resolveSlugByTenantId(event.tenantId())
                .orElse("app");
        String activationUrl = urlBuilder.build(tenantSlug,
                "/activate-professor?token=" + event.invitationToken());
        String professorName = event.firstName() + " " + event.lastName();

        emailService.send(
                EmailType.PROFESSOR_INVITATION,
                new EmailRecipient(event.email(), professorName),
                event.tenantId(),
                Map.of("professorName", professorName,
                       "activationUrl", activationUrl,
                       "expiresAt", event.invitationExpiresAt().toString()));
    }
}
```

- [ ] **Step 4: Compile + run professor tests**

```bash
cd api && mvn compile -q && mvn test -pl . -Dtest="CreateProfessorServiceTest" -q 2>&1 | tail -10
```

Fix any broken test assertions (constructor change to `ProfessorCreated`).

- [ ] **Step 5: Commit**

```bash
git add api/src/main/java/com/klasio/professor/
git commit -m "feat(email): extend ProfessorCreated with invitationExpiresAt; add ProfessorInvitationEmailListener"
```

---

### Task 17: `SessionEventsNotificationListener` — add email fan-out

**Files:**
- Modify: `api/src/main/java/com/klasio/attendance/infrastructure/notification/SessionEventsNotificationListener.java`

- [ ] **Step 1: Add `EmailService` and `StudentEmailPort` to `SessionEventsNotificationListener`**

In the existing listener, inject two new dependencies alongside the existing ones:
```java
private final EmailService emailService;
private final com.klasio.attendance.domain.port.StudentEmailPort studentEmailPort;
```

Add to constructor.

- [ ] **Step 2: Add email fan-out to `onSessionCancelled`**

After the existing in-app notification loop for cancelled students, add:
```java
// Email fan-out for CLASS_SESSION_CHANGE
String changeKind = "CANCELLED";
String startsAt = e.sessionDate().toString() + " " + e.startTime().toString();
for (UUID studentId : e.affectedStudentIds()) {
    Optional<UUID> resolvedUserId = studentUserIdPort.findUserIdByStudentId(e.tenantId(), studentId);
    if (resolvedUserId.isEmpty()) continue;
    studentEmailPort.findEmail(studentId, e.tenantId()).ifPresent(email -> {
        emailService.send(
                com.klasio.email.application.EmailType.CLASS_SESSION_CHANGE,
                new com.klasio.email.application.EmailRecipient(email, email),
                e.tenantId(),
                java.util.Map.of(
                        "studentName", email,
                        "className", className,
                        "startsAt", startsAt,
                        "changeKind", changeKind,
                        "reason", e.reason() != null ? e.reason() : ""));
    });
}
```

- [ ] **Step 3: Add email fan-out to `fanOutAlertLike`** (handles both `onSessionAlertRaised` and `onSessionAlertUpdated`)

After the existing in-app notification loop, add within `fanOutAlertLike`:
```java
// Email fan-out for alert events
String startsAt = /* needs sessionDate+startTime — pass these into fanOutAlertLike params */;
for (AttendanceRegistration reg : regs) {
    studentEmailPort.findEmail(reg.getStudentId(), tenantId).ifPresent(email -> {
        emailService.send(
                com.klasio.email.application.EmailType.CLASS_SESSION_CHANGE,
                new com.klasio.email.application.EmailRecipient(email, email),
                tenantId,
                java.util.Map.of(
                        "studentName", email,
                        "className", title,
                        "startsAt", startsAt,
                        "changeKind", "ALERTED",
                        "reason", body));
    });
}
```

**Note:** `fanOutAlertLike` currently receives `title` and `body` — these can serve as `className` and `reason`. Extend the method signature if needed to pass `sessionDate`+`startTime` for the `startsAt` field.

- [ ] **Step 4: Compile**

```bash
cd api && mvn compile -q 2>&1 | grep "ERROR" | head -10
```

- [ ] **Step 5: Run attendance tests**

```bash
cd api && mvn test -pl . -Dtest="SessionEventsNotificationListenerTest,CancelSessionServiceTest" -q 2>&1 | tail -10
```

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/attendance/infrastructure/notification/SessionEventsNotificationListener.java
git commit -m "feat(email): add CLASS_SESSION_CHANGE email fan-out to SessionEventsNotificationListener"
```

---

## Phase 5 — Cleanup & final wiring

### Task 18: Remove MailHog, `spring.mail.*`, `spring-boot-starter-mail`; update `functional-requirements.md`

**Files:**
- Modify: `docker/docker-compose.yml`
- Modify: `functional-requirements.md`

- [ ] **Step 1: Remove `mailhog` service from `docker/docker-compose.yml`**

Open `docker/docker-compose.yml` and delete the `mailhog` service block (typically looks like):
```yaml
  mailhog:
    image: mailhog/mailhog
    ports:
      - "1025:1025"
      - "8025:8025"
```

- [ ] **Step 2: Run full test suite**

```bash
cd api && mvn test -q 2>&1 | tail -15
```

Expected: `BUILD SUCCESS` with all tests passing.

- [ ] **Step 3: Smoke test on local profile**

Start the application locally:
```bash
cd api && mvn spring-boot:run -Dspring-boot.run.profiles=local -q
```

Trigger a student registration via the API or frontend, then check:
```bash
ls local/email-previews/
```

Expected: an `.html` file exists. Open it in a browser to verify the template renders correctly.

- [ ] **Step 4: Update `functional-requirements.md`**

Find the RF-32 row and mark it complete:
```markdown
| RF-32 | Transactional Email (Postmark) | ✅ (Brevo HTTP API; auth verification, password reset, professor invitation, payment proof uploaded/rejected, membership activated/expiry warning/depleted, class session change; attendance-confirmation email deferred to RF-23/RF-25) |
```

Also update the `003-professor-management` row:
```markdown
| `merged/003-professor-management` | RF-08 | ✅ (email invite now wired via RF-32) |
```

- [ ] **Step 5: Final full test suite run**

```bash
cd api && mvn verify -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add docker/docker-compose.yml functional-requirements.md
git commit -m "chore(email): remove MailHog, update RF-32 to complete in functional-requirements.md"
```

---

## Self-review against spec

| Spec requirement | Task that implements it |
|---|---|
| 9 email types, hybrid strategy | Task 2 (`EmailType`), Task 9 (templates) |
| Single `EmailService.send()` port | Task 2 |
| `EmailDispatcherService` with Caffeine cache | Task 3 |
| Thymeleaf dual-engine (HTML + TEXT) | Task 4 |
| `JpaTenantContextAdapter` | Task 5 |
| InMemory, Logging, Brevo transports | Tasks 6-8 |
| `@ConditionalOnProperty` transport selection | Tasks 6-8 |
| WireMock wire-format IT | Task 8 |
| In-repo templates (5 types) | Task 9 |
| `FrontendUrlBuilder` subdomain routing | Task 10 |
| Auth event extension + `VerificationEmailResendRequested` | Task 11 |
| Migrate auth use cases, `AuthEmailListener`, delete old infra | Task 12 |
| `StudentEmailPort` + `TenantAdminEmailPort` adapters | Task 13 |
| `MembershipActivated`/`ExpiryWarning` event enrichment | Task 14 |
| `MembershipNotificationListener` (3 types) | Task 14 |
| `PaymentProofNotificationListener` (2 types) | Task 15 |
| `ProfessorCreated` event + invitation listener | Task 16 |
| `SessionEventsNotificationListener` email fan-out | Task 17 |
| Remove MailHog/`spring.mail.*`/`spring-boot-starter-mail` | Tasks 1 + 18 |
| `application-test.yml` with `transport: inmemory` | Task 6 |
| `application-local.yml` with `transport: logging` | Task 1 |
| YAML config block (`klasio.email.*`, `klasio.frontend.*`) | Task 1 |
| Spring Retry 3x exponential backoff | Task 8 |
| `emailListenerExecutor` thread pool | Task 7 |
| `@Async("emailListenerExecutor") @TransactionalEventListener(AFTER_COMMIT)` on all listeners | Tasks 12-17 |
| MDC enrichment + Micrometer counters | Task 3 (`EmailDispatcherService`) |
| `functional-requirements.md` updated | Task 18 |
