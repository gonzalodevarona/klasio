# Email i18n + Thymeleaf Migration Design

## Goal

Migrate all 9 transactional email types to in-repo Thymeleaf templates with full i18n (English + Spanish) via Spring `MessageSource`. Eliminate the `BREVO_HOSTED` code path entirely. All emails render per the tenant's configured language.

---

## 1. Architecture & Data Flow

### TenantContext language field

`TenantContext` gains a `language` field (ISO 639-1 tag, e.g. `"en"`, `"es"`). The `JpaTenantContextAdapter` fetches it from `tenant_jpa_entities.language` — this column exists since V060, no migration needed.

```java
// Before
record TenantContext(UUID id, String slug, String name)

// After
record TenantContext(UUID id, String slug, String name, String language)
```

### Locale resolution

`EmailDispatcherService` resolves locale after fetching tenant context:

```java
Locale locale = Locale.forLanguageTag(tenant.language()); // "en" → Locale.ENGLISH
```

### TemplateRenderer port signature change

```java
// Before
String render(String templatePath, Map<String, Object> model);

// After
String render(String templatePath, Locale locale, Map<String, Object> model);
```

`ThymeleafTemplateRenderer` is injected with a `ResourceBundleMessageSource` (bean defined in `EmailConfig`) pointing at `classpath:email/i18n/messages`. Thymeleaf resolves `#{key}` from that bundle per locale and `${var}` from the model map.

### Full call stack

```
DomainEventListener
  → EmailDispatcherService
      → JpaTenantContextAdapter.load(tenantId)   // fetches language
      → locale = Locale.forLanguageTag(language)
      → renderer.render(templatePath, locale, model)
      → BrevoEmailTransport.send(html, text, subject)
```

---

## 2. EmailType Simplification

`BREVO_HOSTED` is deleted. All 9 types become `IN_REPO`.

```java
// Before
enum Source { IN_REPO, BREVO_HOSTED }

// After
// Source enum removed entirely — all emails are in-repo
```

Affected cleanup:
- `EmailType.Source` enum — deleted
- `EmailType` entries — remove `source` field
- `EmailDispatcherService` — delete `if (source == BREVO_HOSTED)` branch
- `BrevoProperties.templateIds()` — removed from config record and `application.yml`

Current BREVO_HOSTED types being migrated:
- `MEMBERSHIP_ACTIVATED`
- `MEMBERSHIP_EXPIRY_WARNING`
- `MEMBERSHIP_DEPLETED`
- `CLASS_SESSION_CHANGE` (already has HTML template, migrate strings to `#{key}`)

---

## 3. Template File Structure

```
resources/
  email-templates/
    base-layout.html                      (existing — extract footer/copyright to #{})
    account-setup.html / .txt             (existing — migrate hardcoded strings)
    password-recovery.html / .txt         (existing — migrate hardcoded strings)
    payment-proof-uploaded.html / .txt    (existing — migrate hardcoded strings)
    payment-rejected.html / .txt          (existing — migrate hardcoded strings)
    professor-invitation.html / .txt      (existing — migrate hardcoded strings)
    class-session-change.html / .txt      (existing — migrate hardcoded strings)
    membership-activated.html / .txt      (NEW)
    membership-expiry-warning.html / .txt (NEW)
    membership-depleted.html / .txt       (NEW)
  email/i18n/
    messages_en.properties
    messages_es.properties
```

### Key naming convention

`<camelCaseTemplate>.<key>` — e.g.:
- `accountSetup.subject`
- `accountSetup.greeting`
- `membershipActivated.subject`
- `layout.footer`

---

## 4. New Template Models

### membership-activated

| Key | Value (EN) |
|---|---|
| `membershipActivated.subject` | `Your membership is now active` |
| `membershipActivated.greeting` | `Hello, {0}!` |
| `membershipActivated.body` | `Your membership for {0} has been activated.` |
| `membershipActivated.hours` | `Available hours: {0}` |
| `membershipActivated.expires` | `Expires on: {0}` |
| `membershipActivated.cta` | `Go to my account` |

Model variables: `studentName`, `programName`, `availableHours`, `expiresOn`, `loginUrl`

### membership-expiry-warning

| Key | Value (EN) |
|---|---|
| `membershipExpiryWarning.subject` | `Your membership expires in {0} days` |
| `membershipExpiryWarning.greeting` | `Hello, {0}!` |
| `membershipExpiryWarning.body` | `Your membership for {0} expires in {1} day(s).` |
| `membershipExpiryWarning.hours` | `Remaining hours: {0}` |
| `membershipExpiryWarning.expires` | `Expiration date: {0}` |
| `membershipExpiryWarning.cta` | `Renew now` |

Model variables: `studentName`, `programName`, `availableHours`, `expiresOn`, `daysLeft`, `loginUrl`

### membership-depleted

| Key | Value (EN) |
|---|---|
| `membershipDepleted.subject` | `Your membership hours have been used up` |
| `membershipDepleted.greeting` | `Hello, {0}!` |
| `membershipDepleted.body` | `All hours in your {0} membership have been consumed.` |
| `membershipDepleted.expires` | `The membership expires on: {0}` |
| `membershipDepleted.cta` | `Contact your academy` |

Model variables: `studentName`, `programName`, `expiresOn`, `loginUrl`

---

## 5. MessageSource Bean

Registered in a new `EmailConfig @Configuration` class (shared between `ThymeleafTemplateRenderer` and `EmailDispatcherService`, both of which need it):

```java
@Bean
public MessageSource emailMessageSource() {
    ResourceBundleMessageSource source = new ResourceBundleMessageSource();
    source.setBasename("email/i18n/messages");
    source.setDefaultEncoding("UTF-8");
    source.setFallbackToSystemLocale(false);
    return source;
}
```

Thymeleaf `TemplateEngine` must be configured with a `SpringTemplateEngine` that has this `MessageSource` set via `setTemplateEngineMessageSource(emailMessageSource)`.

---

## 6. Existing Template Migration Rules

For each of the 5 existing templates:
1. Replace every hardcoded English sentence/label with a `#{key}` expression.
2. Keep all `${var}` model interpolations unchanged.
3. Add matching keys to both `messages_en.properties` and `messages_es.properties`.
4. The `.txt` variant mirrors the `.html` variant keys — both files updated in the same task.

Subject lines: currently set in Java (e.g. `"Your account is ready"`). These move to `#{accountSetup.subject}` evaluated server-side and passed as the `subject` field to `BrevoEmailTransport`.

---

## 7. Subject Line Handling

Subjects are currently hardcoded strings in Java listeners/services. After migration:
- Subject = `renderer.render("email-templates/<name>-subject", locale, model)` — a tiny one-line template, **or**
- Subject = `messageSource.getMessage("<name>.subject", args, locale)` — direct lookup without a template file.

**Chosen approach: direct `MessageSource` lookup** in `EmailDispatcherService` (simpler, avoids extra template files). `EmailDispatcherService` calls `messageSource.getMessage(emailType.subjectKey(), subjectArgs, locale)` and passes the resolved string to the transport.

`EmailType` gains a `subjectKey()` method returning the i18n key for that type's subject line.

---

## 8. Out of Scope

- New email types beyond the 9 existing ones.
- HTML redesign of existing templates (content only, no layout changes).
- Locale detection from request headers (tenant language is the only source).
- Fallback locale logic beyond Spring's default (missing key → key name displayed, acceptable for dev).
- Adding new languages beyond `en` and `es`.
