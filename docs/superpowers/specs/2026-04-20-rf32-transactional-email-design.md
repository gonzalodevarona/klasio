# RF-32 — Transactional Email Service (Brevo)

**Status:** Design approved · 2026-04-20
**Owner:** Gonzalo de Varona
**Branch (planned):** `feature/011-transactional-email`
**Replaces:** existing `AuthEmailSender` / `PostmarkAuthEmailSender` (auth-only, synchronous, hardcoded URLs)
**Implements:** RF-32 (and unblocks the listener stubs added by RF-15, RF-16, RF-19, RF-20, RF-27, RF-28, plus auth flows RF-01 / RF-03 and the professor invitation hook)

---

## 1. Problem

The platform must send 9 distinct transactional emails (auth verification, password recovery, professor invitation, membership activated/expiring/depleted, payment-proof uploaded/rejected, class session change). Today:

- Only auth verification + password reset are wired, and they go through `JavaMailSender` synchronously inside the use case — Brevo (or any SMTP outage) would block the user-facing HTTP request, contradicting RF-32's fire-and-forget rule.
- 7 listener stubs exist with `TODO (RF-32)` markers; no port, no adapter, no templates.
- All current emails hardcode `http://localhost:3000` for click-through links and carry no tenant identity.

We need a single platform-level email infrastructure that is reliable, fire-and-forget, tenant-aware, and renderable both from in-repo templates and from Brevo-hosted templates.

## 2. Decisions (locked during brainstorming)

| # | Decision | Rationale |
|---|---|---|
| 1 | **Transport: Brevo HTTP API** (official `brevo` Java SDK, not SMTP) | Lets us use Brevo-hosted templates, dynamic params, future delivery webhooks |
| 2 | **Template strategy: hybrid** — Brevo-hosted for marketing-ish, in-repo Thymeleaf for system-critical | Versioned + testable for security-relevant copy; non-eng-editable for informational copy |
| 3 | **Tenant context resolution: adapter-side**, listeners pass only `tenantId` | Branding is presentation, not domain; one cached resolver serves all email types |
| 4 | **Migrate `AuthEmailSender` to the new port** (full migration, not façade) | One port, one mental model; auth gets fire-and-forget like the rest of the codebase |
| 5 | **Reliability: in-process retry**, Spring Retry, 3 attempts (1s / 5s / 25s), retryable on 5xx + 429 + IOException | Handles ~99% of transient Brevo blips with ~30 lines; outbox deferred to v2 |
| 6 | **Local profile: log adapter + browser preview** (rendered HTML to `local/email-previews/`) | Matches RF-32 literal ("log the email content"), plus visual QA of templates |
| 7 | **Tenant branding: name only** (`From: "<tenantName> via Klasio" <noreply@klasio.app>`) | Reply-to / logo / colors deferred to a future tenant-branding story |
| 8 | **Port API: `EmailType` enum registry** + single `send(type, recipient, tenantId, params)` method | Hides storage decision; param contract validatable at runtime; new types don't change the port |

## 3. Architecture

A new platform module **`com.klasio.email`** owns one inbound port and three outbound ports. Listeners stay in the module that owns the triggering domain event.

```
┌──────────────────────── com.klasio.email ────────────────────────┐
│ application/                                                     │
│   EmailService          (inbound port)                           │
│   EmailType             (enum: 9 entries)                        │
│   EmailDispatcherService (impl)                                  │
│ domain/port/                                                     │
│   EmailTransport         (outbound)                              │
│   TemplateRenderer       (outbound)                              │
│   TenantContextPort      (outbound)                              │
│ infrastructure/                                                  │
│   transport/                                                     │
│     BrevoEmailTransport    (@Profile dev,staging,prod)           │
│     LoggingEmailTransport  (@Profile local)                      │
│     InMemoryEmailTransport (@Profile test)                       │
│   template/ ThymeleafTemplateRenderer                            │
│   tenant/   JpaTenantContextAdapter                              │
│   config/   BrevoProperties, EmailProperties, EmailRetryConfig,  │
│             EmailExecutorConfig                                  │
└──────────────────────────────────────────────────────────────────┘

events ─► Module listener ─► EmailService.send(EmailType.X, recipient, tenantId, params)
         (auth, membership,
          payment-proof,
          attendance/sessions)
```

All listeners use `@Async("emailListenerExecutor") @TransactionalEventListener(phase = AFTER_COMMIT)`. The async boundary is at the *listener*, so the business HTTP request returns immediately on commit; the retry loop runs on the email executor thread, never blocking the user.

## 4. Components & contracts

### 4.1 `EmailType` (registry — single source of truth)

```java
public enum EmailType {
    PROFESSOR_INVITATION       (Source.IN_REPO,      "professor-invitation",                       Set.of("activationUrl", "expiresAt", "professorName")),
    STUDENT_VERIFICATION       (Source.IN_REPO,      "student-verification",                       Set.of("verificationUrl", "expiresAt", "studentName")),
    PASSWORD_RECOVERY          (Source.IN_REPO,      "password-recovery",                          Set.of("resetUrl", "expiresAt")),
    PAYMENT_PROOF_UPLOADED     (Source.IN_REPO,      "payment-proof-uploaded",                     Set.of("studentName", "programName", "amount", "reviewUrl")),
    PAYMENT_REJECTED           (Source.IN_REPO,      "payment-rejected",                           Set.of("studentName", "programName", "reason", "retryUrl")),
    MEMBERSHIP_ACTIVATED       (Source.BREVO_HOSTED, "brevo.template.membership-activated",        Set.of("studentName", "programName", "planName", "totalHours", "expiresAt")),
    MEMBERSHIP_EXPIRY_WARNING  (Source.BREVO_HOSTED, "brevo.template.membership-expiry-warning",   Set.of("studentName", "programName", "expiresAt", "remainingHours")),
    MEMBERSHIP_DEPLETED        (Source.BREVO_HOSTED, "brevo.template.membership-depleted",         Set.of("studentName", "programName")),
    CLASS_SESSION_CHANGE       (Source.BREVO_HOSTED, "brevo.template.class-session-change",        Set.of("studentName", "className", "startsAt", "changeKind", "reason"));

    enum Source { IN_REPO, BREVO_HOSTED }
    // + getters: source(), templateRef(), requiredKeys()
}
```

`templateRef` is the Thymeleaf path (without extension) when `IN_REPO`, and the YAML config key when `BREVO_HOSTED`.

### 4.2 Inbound port

```java
public interface EmailService {
    void send(EmailType type, EmailRecipient to, UUID tenantId, Map<String, Object> params);
}

public record EmailRecipient(String email, String displayName) {}
```

Validates `params.keySet().containsAll(type.requiredKeys())` — `IllegalArgumentException` on missing keys (fail fast in tests).

### 4.3 Outbound ports

```java
public interface EmailTransport {
    void send(OutboundEmail email);
}

public record OutboundEmail(
    EmailRecipient to,
    EmailSender from,                  // {email, displayName}
    String subject,                    // null when brevoTemplateId set
    String htmlBody, String textBody,  // null when brevoTemplateId set
    Long brevoTemplateId,              // null when html/text set
    Map<String, Object> brevoParams,   // includes tenantName, baseUrl, etc.
    String idempotencyKey
) {}

public interface TemplateRenderer {
    RenderedTemplate render(String templatePath, Map<String, Object> model);
}
public record RenderedTemplate(String subject, String htmlBody, String textBody) {}

public interface TenantContextPort {
    TenantContext findById(UUID tenantId);
}
public record TenantContext(UUID id, String slug, String name) {}
```

Subject of in-repo templates lives in the `.html` template via `<th:block th:fragment="subject">...</th:block>` — single file per template, single source of truth.

### 4.4 `EmailDispatcherService` (orchestration)

Steps per `send(...)`:
1. Validate required-keys.
2. `tenantContextCache.get(tenantId)` (Caffeine, 5-min TTL, max 500 entries).
3. Build `EmailSender("noreply@klasio.app", tenantName + " via Klasio")`.
4. If `IN_REPO` → render via `TemplateRenderer` and assemble `OutboundEmail` with html/text/subject.
   If `BREVO_HOSTED` → look up template ID from `BrevoProperties`; if 0/missing → fall back to `MISSING_TEMPLATE_FALLBACK` in-repo template (logs WARN once per type, memoized).
5. Generate `idempotencyKey = UUID.randomUUID().toString()` — created once per `EmailService.send(...)` call and reused across all retry attempts of that same send. Different logical sends always get distinct keys; retries of a single send dedupe at Brevo. Listeners need no awareness.
6. `emailTransport.send(outboundEmail)` — retry loop lives inside the transport, sees the same idempotency key on every attempt.

Any unexpected `RuntimeException` is caught and logged ERROR — the listener thread always completes normally.

## 5. Data flow (worked example: `MembershipActivated`)

```
1. ActivateMembershipService.activate(...)
     ├── membership.activate()
     ├── membershipRepository.save(membership)
     └── eventPublisher.publish(MembershipActivated(...))    # within current TX

2. [TX commits]

3. MembershipNotificationListener.onActivated(event)
   @Async("emailListenerExecutor") @TransactionalEventListener(AFTER_COMMIT)
     ├── studentEmail = studentEmailPort.findById(event.studentId())
     ├── studentName  = studentNamePort.findById(event.studentId())
     ├── programName  = programNamePort.findById(event.programId())
     └── emailService.send(
           EmailType.MEMBERSHIP_ACTIVATED,
           new EmailRecipient(studentEmail, studentName),
           event.tenantId(),
           Map.of("studentName", studentName,
                  "programName", programName,
                  "planName",    event.planName(),
                  "totalHours",  event.totalHours(),
                  "expiresAt",   event.expirationDate()));

4. EmailDispatcherService.send(...)  →  EmailTransport.send(OutboundEmail{...})

5. BrevoEmailTransport.send(outbound)   @Retryable(3x exp backoff on transient errors)
     ├── Brevo SDK call: TransactionalEmailsApi.sendTransacEmail(payload)
     ├── on 2xx:        log INFO (type, brevoMessageId, recipientHash)
     └── on final fail: log ERROR (type, attempts, lastError) — no rethrow
```

Cross-module port additions (besides existing `StudentNamePort` / `ProgramNamePort` / `ProgramPlanPort`):
- **`StudentEmailPort`** in `com.klasio.student` (returns email by studentId; needed because student email is not currently exposed across modules).

## 6. Failure handling, retry & observability

### Retry config (`klasio.email.retry.*`)
```yaml
klasio.email.retry:
  max-attempts: 3
  initial-delay-ms: 1000
  multiplier: 5            # 1s → 5s → 25s
  max-delay-ms: 30000
```

**Retryable** on: `HttpServerErrorException` (5xx), `HttpClientErrorException.TooManyRequests` (429, honors `Retry-After`), `IOException`, `ResourceAccessException`, `BrevoApiException` with code in `{server_error, rate_limited}`.

**Non-retryable** (single attempt, ERROR log): 4xx other than 429 (config bugs — bad sender, bad API key, IP block).

### Failure containment

- `BrevoEmailTransport.send` catches its final exception and logs — does not throw.
- `EmailDispatcherService.send` wraps the whole flow in try/catch — any unexpected `RuntimeException` (template render error, missing param) is logged but never propagated to the listener.
- Listener thread therefore always completes normally — no path where email failure surfaces back to the business operation.

### Observability

- `MDC` tags per listener invocation: `tenantId`, `emailType`, `recipientEmailHash` (SHA-256, first 8 chars).
- Log levels: INFO on success (with `brevoMessageId`), WARN on retry, ERROR on final failure (full stack at DEBUG).
- Micrometer counters: `klasio.email.sent{type,tenantId}`, `klasio.email.failed{type,tenantId,reason}`, `klasio.email.retries{type}`.

## 7. Profiles, local dev & template authoring

| Profile | `EmailTransport` | Behavior |
|---|---|---|
| `local` | `LoggingEmailTransport` | Logs envelope at INFO; writes rendered HTML to `./local/email-previews/<ISO>-<type>-<hash>.html`. Auto-prunes files older than 7 days on startup. |
| `test` | `InMemoryEmailTransport` | Records every `OutboundEmail` in a thread-safe list; provides `assertSent(...)` helpers. |
| `dev`, `staging`, `prod` | `BrevoEmailTransport` | Real HTTPS calls via Brevo SDK. |

Selection via `klasio.email.transport=logging|inmemory|brevo` and `@ConditionalOnProperty` on each transport bean (matchIfMissing=false on Brevo — defaults safe).

### Template authoring (`api/src/main/resources/email-templates/`)

```
email-templates/
  layouts/base.html
  professor-invitation.html / .txt
  student-verification.html / .txt
  password-recovery.html   / .txt
  payment-proof-uploaded.html / .txt
  payment-rejected.html    / .txt
  missing-template-fallback.html / .txt
```

Two `TemplateEngine` instances (HTML mode + TEXT mode) wired in `ThymeleafEmailConfig` — separate from the Spring MVC engine to avoid cross-contamination.

Subject convention per template:
```html
<th:block th:fragment="subject" th:text="|Welcome to ${tenantName}|"></th:block>
```
Renderer extracts the subject fragment first, then the body — single file, single source of truth.

## 8. Frontend URL composition (subdomain-per-tenant)

Production routes tenants by subdomain: `liga-valle-tenis.app.klasio.com`. URL builder is templated:

```yaml
klasio.frontend.url-template: ${KLASIO_FRONTEND_URL_TEMPLATE:http://localhost:3000}
# prod:    https://{tenantSlug}.app.klasio.com
# staging: https://{tenantSlug}.staging.klasio.com
# local:   http://localhost:3000   (no placeholder — single-tenant dev)
```

```java
public String build(String tenantSlug, String path) {
    String base = template.contains("{tenantSlug}")
        ? template.replace("{tenantSlug}", tenantSlug)
        : template;
    return base + (path.startsWith("/") ? path : "/" + path);
}
```

`FrontendUrlBuilder` lives in `com.klasio.shared.web` and is used by every listener that builds a click-through link.

## 9. Migration plan for existing auth email path

**Files removed:**
- `api/.../auth/application/port/AuthEmailSender.java`
- `api/.../auth/infrastructure/mail/PostmarkAuthEmailSender.java` (and the `mail/` directory)

**Use case change** — `RegisterStudentService` and `RequestPasswordResetService` stop calling `authEmailSender.*` directly; they already publish `UserRegistered` / `PasswordResetRequested` events. The events are extended to carry `rawToken`, `expiresAt`, and `displayName` (raw token must be on the event because only the hash is persisted — listener has no other way to obtain it).

**New listener** — `auth/infrastructure/notification/AuthEmailListener.java` handles both events and calls `EmailService.send(...)` with `EmailType.STUDENT_VERIFICATION` and `EmailType.PASSWORD_RECOVERY`.

**Async executor** (`com.klasio.email.infrastructure.config.EmailExecutorConfig`):
```java
@Bean("emailListenerExecutor")
TaskExecutor emailListenerExecutor() {
    var ex = new ThreadPoolTaskExecutor();
    ex.setCorePoolSize(2); ex.setMaxPoolSize(8); ex.setQueueCapacity(500);
    ex.setThreadNamePrefix("email-listener-");
    ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    ex.initialize();
    return ex;
}
```
All email-related listeners across all modules use this same executor (auth, membership, payment-proof, session-events) — single observability surface, single back-pressure point.

**Dependencies removed:** `spring-boot-starter-mail`, all `spring.mail.*` config blocks, the `mailhog` service in `docker/docker-compose.yml`.

## 10. Testing strategy

Per project TDD rule: tests first, real coverage of happy + edge + failure paths.

1. **Unit — `EmailDispatcherService`** (Mockito): IN_REPO vs BREVO_HOSTED routing, missing required-key fail-fast, From-line composition, missing template-ID fallback, idempotency-key derivation, tenant-not-found propagation.
2. **Unit — `BrevoEmailTransport`** (Spring Retry context, Brevo SDK mocked): 2xx single attempt, 5xx-then-2xx, 3x 5xx final-fail no-throw, 429 with `Retry-After`, 4xx single attempt, IOException retry, Idempotency-Key header on every attempt.
3. **Unit — `ThymeleafTemplateRenderer`** (real engine): per template, assert subject extraction, model substitution, no unresolved `${...}`, snapshot fixtures in `src/test/resources/email-fixtures/` regenerable via `-Dupdate-snapshots=true`.
4. **Listener IT** — one per email type (9 total), `@SpringBootTest` + `InMemoryEmailTransport`, publishes the real domain event inside a TX and `awaitility` asserts the recorded `OutboundEmail` shape after AFTER_COMMIT fires.
5. **Failure-isolation tests** — one per listener: `@MockBean EmailService` configured to throw → assert no exception escapes, business state intact.
6. **Wire-format IT** (one only) — WireMock + `klasio.email.transport=brevo`; sends one IN_REPO and one BREVO_HOSTED; asserts Brevo JSON shape (`sender.email`, `sender.name`, `to[].email`, `templateId` / `htmlContent`, `params`, `headers."Idempotency-Key"`). Protects against silent Brevo SDK upgrades.
7. **Auth migration regression** — old `verify(authEmailSender).*` assertions become `ApplicationEvents` recorder assertions on `UserRegistered` / `PasswordResetRequested`; email behavior covered by `AuthEmailListenerIT` (case 4).
8. **Coverage target:** ≥ 70% on `com.klasio.email` business layer (project NFR). `EmailType` and `BrevoProperties` excluded as trivial.

## 11. Configuration & env vars

```yaml
klasio:
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
        membership-activated:        ${BREVO_TEMPLATE_MEMBERSHIP_ACTIVATED:0}
        membership-expiry-warning:   ${BREVO_TEMPLATE_MEMBERSHIP_EXPIRY:0}
        membership-depleted:         ${BREVO_TEMPLATE_MEMBERSHIP_DEPLETED:0}
        class-session-change:        ${BREVO_TEMPLATE_CLASS_SESSION_CHANGE:0}
```

Profile overrides: `application-local.yml` → `transport: logging`; `application-test.yml` → `transport: inmemory`; `application-{dev,staging,prod}.yml` → `transport: brevo`.

| Env var | Required when | Failure mode if missing |
|---|---|---|
| `BREVO_API_KEY` | `transport=brevo` | `@Validated BrevoProperties` rejects blank → app fails to start |
| `KLASIO_EMAIL_FROM_ADDRESS` | always | Falls back to `noreply@klasio.app` (must be pre-verified in Brevo) |
| `KLASIO_FRONTEND_URL_TEMPLATE` | always | Falls back to `localhost`; startup validation rejects `localhost` when `transport=brevo` |
| `BREVO_TEMPLATE_*` | `transport=brevo` | Per-type fallback to `MISSING_TEMPLATE_FALLBACK`, WARN once |

## 12. Rollout sequence (single feature branch `feature/011-transactional-email`)

1. **Scaffold `com.klasio.email`** module: ports, `EmailType`, `EmailDispatcherService`, three transports, `ThymeleafTemplateRenderer`, `TenantContextPort` adapter, executor + properties config. Tests 1, 2, 6 of §10 pass.
2. **Add 6 in-repo Thymeleaf templates** + base layout + `.txt` counterparts + missing-template-fallback. Test 3 of §10 passes.
3. **Wire 3 Brevo-hosted templates** — built in Brevo dashboard out-of-band; IDs captured into env config. Test 6 of §10 passes for both shapes.
4. **Migrate auth** — extend `UserRegistered` + `PasswordResetRequested` events; introduce `AuthEmailListener`; delete `AuthEmailSender` + `PostmarkAuthEmailSender`; rewrite affected use case tests. Manual smoke on `local`: register → HTML preview file → click link → account activates.
5. **Implement listener bodies** for the 7 stubbed call sites: `MembershipNotificationListener` (activated, expiry warning, depleted), `PaymentProofNotificationListener` (uploaded, rejected), `SessionEventsNotificationListener` (extend existing in-app to also call email for class-session-change), `ProfessorInvitationListener`. Test 4 + 5 of §10 pass per listener.
6. **Drop dependencies & infra** — remove `spring-boot-starter-mail` from `api/pom.xml`, `spring.mail.*` from all profile yamls, `mailhog` service from `docker/docker-compose.yml`. Verify auth flows on `local`.
7. **Update `functional-requirements.md`** — RF-32 → ✅ (with note: attendance-confirmation emails in `AttendanceNotificationListener` deferred to RF-23 / RF-25 since attendance flows don't yet exist).
8. **Operational doc** — append a "How to add a new email type" runbook section to project `README.md` (or new `docs/email.md`): enum entry, template files, Brevo sender verification, env-var checklist, log inspection.

## 13. Out of scope (explicit)

- Transactional outbox / DLQ — v2; one-line config bump in §6 unlocks deeper retries first.
- Tenant logo / brand colors in HTML headers — own future story (`feat(tenant): branding`).
- Per-tenant `Reply-To` — own future story.
- Email open / click tracking via Brevo webhooks — own future story.
- Internationalization of templates — English-only per project memory.
- Attendance-confirmation emails (per-attendance) — coupled to RF-23 / RF-25; ship with them.
