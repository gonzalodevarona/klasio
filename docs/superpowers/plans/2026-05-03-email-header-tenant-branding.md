# Email Header Tenant Branding + Subject Leak Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix two email rendering bugs — (1) subject text leaking into the visible body across all 9 templates, and (2) hardcoded Klasio branding in the header so tenant emails show the tenant's logo + name and only system-level emails show Klasio.

**Architecture:** TDD across three slices. (1) Replace each template's `<th:block th:fragment="subject">` with a `<title>`-wrapped block so the orphan text node lives inside `<title>` (invisible). (2) Extract the email header into a reusable Thymeleaf fragment with three rendering states driven by `${tenantLogoUrl}` and `${tenantName}`. (3) Plumb a stable public S3 URL (via `S3Utilities.getUrl`) for the tenant logo through `LogoStorage` → `JpaTenantContextAdapter` → `TenantContext` → `EmailDispatcherService`. No DB schema changes; bucket policy update is an ops task scheduled after merge.

**Tech Stack:** Java 21, Spring Boot 3.4, Thymeleaf, AWS SDK v2 (S3), JUnit 5 + Mockito + AssertJ, Testcontainers (LocalStack), Jsoup (parsing in tests).

**Spec:** `docs/superpowers/specs/2026-05-03-email-header-tenant-branding-design.md`

---

## Pre-flight check

Verify clean working tree on the working branch (or current branch) and that backend tests currently pass.

- [ ] **Step 1: Verify clean tree**

```bash
cd /Users/gonzalodevarona/Documents/klasio
git status
```

Expected: clean tree, on a feature branch (or `main` if working directly per project flow).

- [ ] **Step 2: Run baseline backend tests**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -DskipTests=false test
```

Expected: all tests pass. Note the count to confirm later.

---

## Task 1: Add Jsoup test dependency (if missing)

We need Jsoup to parse rendered email HTML in tests for precise assertions about `<head>` content.

**Files:**
- Modify: `api/pom.xml`

- [ ] **Step 1: Check if Jsoup already on classpath**

Run:
```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q dependency:list 2>/dev/null | grep -i jsoup
```

If a result appears (any version), skip the rest of this task. If empty, continue.

- [ ] **Step 2: Add Jsoup to `api/pom.xml`** (test scope)

Find the `<dependencies>` block in `api/pom.xml`. Add:

```xml
<dependency>
    <groupId>org.jsoup</groupId>
    <artifactId>jsoup</artifactId>
    <version>1.17.2</version>
    <scope>test</scope>
</dependency>
```

- [ ] **Step 3: Verify Maven resolves it**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q dependency:resolve 2>&1 | tail -20
```

Expected: no errors.

- [ ] **Step 4: Commit**

```bash
git add api/pom.xml
git commit -m "chore(email): add jsoup test dependency for html assertions"
```

---

## Task 2: Add a renderer test that pins the subject-leak bug

Write a failing test that proves the bug — the rendered HTML has a bare text node inside `<head>` that's not inside `<title>`. Run it red before fixing templates.

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`

- [ ] **Step 1: Write the failing test**

Append a new `@Test` method at the end of `ThymeleafTemplateRendererTest`:

```java
@Test
void allTemplates_haveNoBareTextNodeInHeadOutsideTitle() {
    java.util.Map<String, java.util.Map<String, Object>> samples = java.util.Map.of(
        "account-setup", java.util.Map.of(
            "recipientName", "X", "role", "student",
            "setupUrl", "http://e", "expiresAt", "x",
            "tenantName", "T", "tenantSlug", "t", "loginUrl", "http://e"),
        "professor-invitation", java.util.Map.of(
            "professorName", "X", "activationUrl", "http://e",
            "expiresAt", "x", "tenantName", "T",
            "tenantSlug", "t", "loginUrl", "http://e"),
        "password-recovery", java.util.Map.of(
            "resetUrl", "http://e", "expiresAt", "x",
            "tenantName", "T", "tenantSlug", "t", "loginUrl", "http://e"),
        "payment-proof-uploaded", java.util.Map.of(
            "studentName", "X", "programName", "P",
            "reviewUrl", "http://e", "tenantName", "T",
            "tenantSlug", "t", "loginUrl", "http://e"),
        "payment-rejected", java.util.Map.of(
            "studentName", "X", "programName", "P", "reason", "r",
            "retryUrl", "http://e", "tenantName", "T",
            "tenantSlug", "t", "loginUrl", "http://e"),
        "membership-activated", java.util.Map.of(
            "studentName", "X", "programName", "P", "planName", "PL",
            "totalHours", 1, "expiresAt", "2026-05-31",
            "tenantName", "T", "tenantSlug", "t", "loginUrl", "http://e"),
        "membership-expiry-warning", java.util.Map.of(
            "studentName", "X", "programName", "P", "remainingHours", 1,
            "expiresAt", "2026-05-31", "tenantName", "T",
            "tenantSlug", "t", "loginUrl", "http://e"),
        "membership-depleted", java.util.Map.of(
            "studentName", "X", "programName", "P", "tenantName", "T",
            "tenantSlug", "t", "loginUrl", "http://e"),
        "class-session-change", java.util.Map.of(
            "studentName", "X", "className", "C", "startsAt", "x",
            "changeKind", "ALERTED", "reason", "r",
            "tenantName", "T", "tenantSlug", "t", "loginUrl", "http://e"));

    samples.forEach((tpl, model) -> {
        RenderedTemplate r = renderer.render(tpl, Locale.ENGLISH, model);
        org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(r.htmlBody());
        org.jsoup.nodes.Element head = doc.head();
        for (org.jsoup.nodes.Node child : head.childNodes()) {
            if (child instanceof org.jsoup.nodes.TextNode tn) {
                assertThat(tn.text().trim())
                    .as("template %s leaks text in <head>: '%s'", tpl, tn.text())
                    .isEmpty();
            }
        }
    });
}
```

- [ ] **Step 2: Run the test — confirm it FAILS**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='ThymeleafTemplateRendererTest#allTemplates_haveNoBareTextNodeInHeadOutsideTitle' test
```

Expected: FAIL. Each template will report a non-empty bare text node in `<head>` (the leaked subject string).

- [ ] **Step 3: Commit failing test**

```bash
git add api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java
git commit -m "test(email): pin subject-leak bug across all 9 templates"
```

---

## Task 3: Fix subject leak in all 9 templates

Wrap the existing `<th:block th:fragment="subject">` inside a real `<title>` element so the orphan text node ends up inside `<title>` (invisible).

**Files:**
- Modify: `api/src/main/resources/email-templates/account-setup.html`
- Modify: `api/src/main/resources/email-templates/professor-invitation.html`
- Modify: `api/src/main/resources/email-templates/password-recovery.html`
- Modify: `api/src/main/resources/email-templates/payment-proof-uploaded.html`
- Modify: `api/src/main/resources/email-templates/payment-rejected.html`
- Modify: `api/src/main/resources/email-templates/membership-activated.html`
- Modify: `api/src/main/resources/email-templates/membership-expiry-warning.html`
- Modify: `api/src/main/resources/email-templates/membership-depleted.html`
- Modify: `api/src/main/resources/email-templates/class-session-change.html`
- Modify: `api/src/main/resources/email-templates/missing-template-fallback.html`

- [ ] **Step 1: Edit each of the 10 HTML templates**

For each template file, find this line:

```html
<th:block th:fragment="subject" th:text="#{XXX.subject}"></th:block>
```

(where `XXX` is the template-specific i18n key, e.g. `accountSetup`, `passwordRecovery`, `membershipActivated`, etc.)

Replace with:

```html
<title><th:block th:fragment="subject" th:text="#{XXX.subject}"></th:block></title>
```

Keep the i18n key exactly as it was. If a template already has another `<title>` element in its `<head>`, remove the original (we now have a `<title>` from the fragment).

The 10 i18n keys to preserve (one per template):
- `account-setup.html` → `#{accountSetup.subject}`
- `professor-invitation.html` → `#{professorInvitation.subject}`
- `password-recovery.html` → `#{passwordRecovery.subject}`
- `payment-proof-uploaded.html` → `#{paymentProofUploaded.subject}`
- `payment-rejected.html` → `#{paymentRejected.subject}`
- `membership-activated.html` → `#{membershipActivated.subject}`
- `membership-expiry-warning.html` → `#{membershipExpiryWarning.subject}`
- `membership-depleted.html` → `#{membershipDepleted.subject}`
- `class-session-change.html` → `#{classSessionChange.subject}`
- `missing-template-fallback.html` → `#{missingTemplate.subject}`

(If any of these key names differ in a given file, preserve the file's existing key — don't rename.)

- [ ] **Step 2: Run the leak test — confirm it PASSES**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='ThymeleafTemplateRendererTest#allTemplates_haveNoBareTextNodeInHeadOutsideTitle' test
```

Expected: PASS.

- [ ] **Step 3: Run the full renderer test class — confirm everything else still passes**

```bash
mvn -q -Dtest='ThymeleafTemplateRendererTest' test
```

Expected: PASS. Subject extraction (line 30 of `ThymeleafTemplateRenderer`) still returns the correct subject because the fragment scope is the inner `<th:block>`, not the wrapping `<title>`.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/resources/email-templates/
git commit -m "fix(email): wrap subject fragment in title element to stop body leak"
```

---

## Task 4: Add `getPublicUrl` to `LogoStorage` port

Extend the port with a method that returns a stable public S3 URL.

**Files:**
- Modify: `api/src/main/java/com/klasio/tenant/domain/port/LogoStorage.java`

- [ ] **Step 1: Add method to interface**

Open `api/src/main/java/com/klasio/tenant/domain/port/LogoStorage.java` and add a new method below `generatePresignedUrl`:

```java
/**
 * Stable public URL for a logo object. Returns null when {@code logoKey} is null.
 * The bucket policy must grant public read on the {@code logos/*} prefix for the
 * URL to be reachable; this method does not sign the request.
 */
String getPublicUrl(String logoKey);
```

The full interface afterwards:

```java
package com.klasio.tenant.domain.port;

import java.io.InputStream;
import java.util.UUID;

public interface LogoStorage {

    String upload(UUID tenantId, InputStream data, String contentType, long size);

    void delete(String logoKey);

    String generatePresignedUrl(String logoKey);

    String getPublicUrl(String logoKey);
}
```

- [ ] **Step 2: Compile to surface broken implementers**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -DskipTests compile
```

Expected: FAIL on `S3LogoStorage` (does not implement `getPublicUrl`). This is desired — we'll fix it in Task 5.

(Do not commit yet — leave the port and impl change as one logical commit.)

---

## Task 5: Implement `getPublicUrl` in `S3LogoStorage` (TDD)

Write a failing integration test that asserts a stable public URL is returned for an existing key, then implement.

**Files:**
- Modify: `api/src/test/java/com/klasio/tenant/infrastructure/storage/S3LogoStorageIntegrationTest.java`
- Modify: `api/src/main/java/com/klasio/tenant/infrastructure/storage/S3LogoStorage.java`

- [ ] **Step 1: Write the failing tests**

Append the two tests to `S3LogoStorageIntegrationTest`:

```java
@Test
@DisplayName("getPublicUrl returns stable URL containing bucket and key for an existing logo")
void getPublicUrl_existingKey_returnsStableUrl() {
    byte[] pngHeader = createMinimalPng();
    InputStream data = new ByteArrayInputStream(pngHeader);
    UUID tenantId = UUID.randomUUID();

    String key = logoStorage.upload(tenantId, data, "image/png", pngHeader.length);

    String url = logoStorage.getPublicUrl(key);

    assertThat(url).isNotBlank();
    assertThat(url).contains(BUCKET_NAME);
    assertThat(url).contains(key);
    // Stable URL must not carry signed-request query params.
    assertThat(url).doesNotContain("X-Amz-Signature");
    assertThat(url).doesNotContain("X-Amz-Credential");
}

@Test
@DisplayName("getPublicUrl returns null when logo key is null")
void getPublicUrl_nullKey_returnsNull() {
    assertThat(logoStorage.getPublicUrl(null)).isNull();
}
```

- [ ] **Step 2: Run the new tests — confirm they FAIL (compilation or assertion)**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='S3LogoStorageIntegrationTest#getPublicUrl_existingKey_returnsStableUrl+S3LogoStorageIntegrationTest#getPublicUrl_nullKey_returnsNull' test
```

Expected: FAIL — compilation error because `S3LogoStorage` does not declare `getPublicUrl`.

- [ ] **Step 3: Implement `getPublicUrl` in `S3LogoStorage`**

Add the import at the top of `api/src/main/java/com/klasio/tenant/infrastructure/storage/S3LogoStorage.java`:

```java
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
```

Add the method (place it after `generatePresignedUrl`):

```java
@Override
public String getPublicUrl(String logoKey) {
    if (logoKey == null) {
        return null;
    }
    return s3Client.utilities().getUrl(GetUrlRequest.builder()
            .bucket(bucket)
            .key(logoKey)
            .build()).toString();
}
```

- [ ] **Step 4: Run the new tests — confirm they PASS**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='S3LogoStorageIntegrationTest' test
```

Expected: all tests in the class pass.

- [ ] **Step 5: Verify no other implementer is broken**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -DskipTests compile
```

Expected: clean compile. If a stub `LogoStorage` mock exists elsewhere (search with `grep -rn "implements LogoStorage" api/src`), add a no-op `getPublicUrl(String) { return null; }` override there.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/tenant/domain/port/LogoStorage.java \
        api/src/main/java/com/klasio/tenant/infrastructure/storage/S3LogoStorage.java \
        api/src/test/java/com/klasio/tenant/infrastructure/storage/S3LogoStorageIntegrationTest.java
git commit -m "feat(tenant): add LogoStorage.getPublicUrl returning stable s3 url"
```

---

## Task 6: Extend `TenantContext` with `logoUrl`

Add a nullable `logoUrl` field to the email module's `TenantContext` record.

**Files:**
- Modify: `api/src/main/java/com/klasio/email/domain/model/TenantContext.java`

- [ ] **Step 1: Update record signature**

Replace the file contents with:

```java
package com.klasio.email.domain.model;

import java.util.UUID;

public record TenantContext(
        UUID id,
        String slug,
        String name,
        String language,
        String timezone,
        String logoUrl
) {}
```

- [ ] **Step 2: Compile to surface callers**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -DskipTests compile
```

Expected: FAIL on `JpaTenantContextAdapter` (constructs a 5-arg `TenantContext`). This is desired — fixed in Task 7.

(Do not commit yet — bundle with Task 7.)

---

## Task 7: Plumb `logoUrl` through `JpaTenantContextAdapter` (TDD)

Update the adapter to fetch `logo_key` and resolve it via `LogoStorage.getPublicUrl`. Update the existing unit test first.

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/tenant/JpaTenantContextAdapterTest.java`
- Modify: `api/src/main/java/com/klasio/email/infrastructure/tenant/JpaTenantContextAdapter.java`

- [ ] **Step 1: Update the existing test class to assert `logoUrl`**

Replace the contents of `JpaTenantContextAdapterTest.java` with:

```java
package com.klasio.email.infrastructure.tenant;

import com.klasio.email.domain.model.TenantContext;
import com.klasio.tenant.domain.port.LogoStorage;
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
    private final LogoStorage logoStorage = mock(LogoStorage.class);
    private final JpaTenantContextAdapter adapter =
            new JpaTenantContextAdapter(em, logoStorage);

    @Test
    void findById_withLogoKey_returnsLogoUrl() {
        UUID id = UUID.randomUUID();
        Query query = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(new Object[]{
                id, "test-slug", "Test League", "en",
                "America/Bogota", "logos/abc/img.png"}));
        when(logoStorage.getPublicUrl("logos/abc/img.png"))
                .thenReturn("https://klasio.s3.us-east-1.amazonaws.com/logos/abc/img.png");

        TenantContext ctx = adapter.findById(id);

        assertThat(ctx.id()).isEqualTo(id);
        assertThat(ctx.slug()).isEqualTo("test-slug");
        assertThat(ctx.name()).isEqualTo("Test League");
        assertThat(ctx.language()).isEqualTo("en");
        assertThat(ctx.timezone()).isEqualTo("America/Bogota");
        assertThat(ctx.logoUrl())
                .isEqualTo("https://klasio.s3.us-east-1.amazonaws.com/logos/abc/img.png");
    }

    @Test
    void findById_withoutLogoKey_returnsNullLogoUrl() {
        UUID id = UUID.randomUUID();
        Query query = mock(Query.class);
        when(em.createQuery(anyString())).thenReturn(query);
        when(query.setParameter(anyString(), any())).thenReturn(query);
        when(query.getResultList()).thenReturn(List.of(new Object[]{
                id, "test-slug", "Test League", "en", "America/Bogota", null}));
        when(logoStorage.getPublicUrl(null)).thenReturn(null);

        TenantContext ctx = adapter.findById(id);

        assertThat(ctx.logoUrl()).isNull();
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

- [ ] **Step 2: Run the tests — confirm they FAIL (compilation)**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='JpaTenantContextAdapterTest' test
```

Expected: FAIL — `JpaTenantContextAdapter` constructor takes only `EntityManager`.

- [ ] **Step 3: Update the adapter**

Replace the contents of `api/src/main/java/com/klasio/email/infrastructure/tenant/JpaTenantContextAdapter.java` with:

```java
package com.klasio.email.infrastructure.tenant;

import com.klasio.email.domain.model.TenantContext;
import com.klasio.email.domain.port.TenantContextPort;
import com.klasio.tenant.domain.port.LogoStorage;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Component
public class JpaTenantContextAdapter implements TenantContextPort {

    private final EntityManager em;
    private final LogoStorage logoStorage;

    public JpaTenantContextAdapter(EntityManager em, LogoStorage logoStorage) {
        this.em = em;
        this.logoStorage = logoStorage;
    }

    @Override
    @Transactional(readOnly = true)
    public TenantContext findById(UUID tenantId) {
        @SuppressWarnings("unchecked")
        List<Object> rows = em.createQuery(
                        "SELECT t.id, t.slug, t.name, t.language, t.timezone, t.logoKey " +
                                "FROM TenantJpaEntity t WHERE t.id = :id")
                .setParameter("id", tenantId)
                .getResultList();
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("Tenant not found: " + tenantId);
        }
        Object first = rows.get(0);
        Object[] r = (first instanceof Object[]) ? (Object[]) first : rows.toArray();
        String logoKey = (String) r[5];
        String logoUrl = logoStorage.getPublicUrl(logoKey);
        return new TenantContext(
                (UUID) r[0],
                (String) r[1],
                (String) r[2],
                (String) r[3],
                (String) r[4],
                logoUrl
        );
    }
}
```

- [ ] **Step 4: Run the tests — confirm they PASS**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='JpaTenantContextAdapterTest' test
```

Expected: PASS.

- [ ] **Step 5: Compile the whole module**

```bash
mvn -q -DskipTests compile
```

Expected: clean compile.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/email/domain/model/TenantContext.java \
        api/src/main/java/com/klasio/email/infrastructure/tenant/JpaTenantContextAdapter.java \
        api/src/test/java/com/klasio/email/infrastructure/tenant/JpaTenantContextAdapterTest.java
git commit -m "feat(email): expose tenant logoUrl via TenantContext"
```

---

## Task 8: Pass `tenantLogoUrl` into the template model (TDD)

Update `EmailDispatcherService` to put `tenantLogoUrl` in the model so templates can branch on it. Update the dispatcher's existing test to cover the new key.

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/service/EmailDispatcherServiceTest.java`
- Modify: `api/src/main/java/com/klasio/email/infrastructure/service/EmailDispatcherService.java`

- [ ] **Step 1: Read the existing dispatcher test to find the right injection point**

Open `api/src/test/java/com/klasio/email/infrastructure/service/EmailDispatcherServiceTest.java` and locate a test that asserts the model contents passed to the renderer (search for `verify(renderer)` or `ArgumentCaptor`).

If no such test exists, write a new one in this step. Use a `Mockito.ArgumentCaptor<Map<String, Object>>` to capture the `model` argument passed to `renderer.render(...)`.

- [ ] **Step 2: Add the failing test**

Append this test (adapt mock setup names to match what the existing class uses — keep variable names consistent with the existing file):

```java
@Test
void send_putsTenantLogoUrlInModel() {
    UUID tenantId = UUID.randomUUID();
    when(tenantContextPort.findById(tenantId)).thenReturn(new TenantContext(
            tenantId, "test-slug", "Test League", "en", "America/Bogota",
            "https://klasio.s3.us-east-1.amazonaws.com/logos/abc/img.png"));
    when(renderer.render(anyString(), any(Locale.class), anyMap()))
            .thenReturn(new RenderedTemplate("subj", "<html/>", "txt"));

    Map<String, Object> params = new HashMap<>(Map.of(
            "resetUrl", "https://x", "expiresAt", "30 minutes"));
    dispatcher.send(EmailType.PASSWORD_RECOVERY,
            new EmailRecipient("u@x", "User"), tenantId, params);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(renderer).render(eq("password-recovery"), any(Locale.class), captor.capture());
    Map<String, Object> model = captor.getValue();
    assertThat(model).containsEntry("tenantLogoUrl",
            "https://klasio.s3.us-east-1.amazonaws.com/logos/abc/img.png");
}

@Test
void send_putsNullTenantLogoUrlInModel_whenTenantHasNoLogo() {
    UUID tenantId = UUID.randomUUID();
    when(tenantContextPort.findById(tenantId)).thenReturn(new TenantContext(
            tenantId, "test-slug", "Test League", "en", "America/Bogota", null));
    when(renderer.render(anyString(), any(Locale.class), anyMap()))
            .thenReturn(new RenderedTemplate("subj", "<html/>", "txt"));

    Map<String, Object> params = new HashMap<>(Map.of(
            "resetUrl", "https://x", "expiresAt", "30 minutes"));
    dispatcher.send(EmailType.PASSWORD_RECOVERY,
            new EmailRecipient("u@x", "User"), tenantId, params);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
    verify(renderer).render(eq("password-recovery"), any(Locale.class), captor.capture());
    Map<String, Object> model = captor.getValue();
    assertThat(model).containsKey("tenantLogoUrl");
    assertThat(model.get("tenantLogoUrl")).isNull();
}
```

If the existing dispatcher test does not yet wire a `tenantContextPort` mock through the dispatcher (the production code uses a `LoadingCache`), you may need to add a test-only constructor or expose the loader. Inspect the file first; if the existing tests already pass `TenantContext` through the cache, follow that pattern.

- [ ] **Step 3: Add the necessary imports** to the test file

```java
import com.klasio.email.domain.model.RenderedTemplate;
import com.klasio.email.domain.model.TenantContext;
import org.mockito.ArgumentCaptor;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
```

- [ ] **Step 4: Run new tests — confirm they FAIL**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='EmailDispatcherServiceTest#send_putsTenantLogoUrlInModel+EmailDispatcherServiceTest#send_putsNullTenantLogoUrlInModel_whenTenantHasNoLogo' test
```

Expected: FAIL — model has no `tenantLogoUrl` key.

- [ ] **Step 5: Update `EmailDispatcherService` to populate the key**

In `api/src/main/java/com/klasio/email/infrastructure/service/EmailDispatcherService.java`, find this block (currently lines 72–73):

```java
            model.put("tenantName", tenant.name());
            model.put("tenantSlug", tenant.slug());
```

Add immediately below it:

```java
            model.put("tenantLogoUrl", tenant.logoUrl());
```

- [ ] **Step 6: Run new tests — confirm they PASS**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='EmailDispatcherServiceTest' test
```

Expected: all tests in the class pass.

- [ ] **Step 7: Commit**

```bash
git add api/src/main/java/com/klasio/email/infrastructure/service/EmailDispatcherService.java \
        api/src/test/java/com/klasio/email/infrastructure/service/EmailDispatcherServiceTest.java
git commit -m "feat(email): pass tenantLogoUrl into template model"
```

---

## Task 9: Add a renderer test that pins the header-state behavior

Write a test that asserts the rendered HTML header for each of the three states. Run red before implementing the header fragment.

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`

- [ ] **Step 1: Append the new test**

```java
@Test
void header_state1_tenantWithLogo_rendersImgWithLogoUrl() {
    RenderedTemplate r = renderer.render("password-recovery", Locale.ENGLISH, Map.of(
            "resetUrl", "http://e", "expiresAt", "x",
            "tenantName", "Acme League", "tenantSlug", "acme",
            "tenantLogoUrl", "https://cdn.example.com/logos/acme.png",
            "loginUrl", "http://e"));

    org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(r.htmlBody());
    org.jsoup.select.Elements imgs = doc.select(
            "img[src='https://cdn.example.com/logos/acme.png']");
    assertThat(imgs).as("expected tenant logo <img>").isNotEmpty();
    assertThat(r.htmlBody()).contains("Acme League");
}

@Test
void header_state2_tenantWithoutLogo_rendersKlasioBadgeAndTenantName() {
    RenderedTemplate r = renderer.render("password-recovery", Locale.ENGLISH, Map.of(
            "resetUrl", "http://e", "expiresAt", "x",
            "tenantName", "Acme League", "tenantSlug", "acme",
            "loginUrl", "http://e"));
    // tenantLogoUrl intentionally absent (treated as null).

    assertThat(r.htmlBody())
            .as("Klasio badge SVG path must be present")
            .contains("M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z");
    assertThat(r.htmlBody()).contains("Acme League");
    assertThat(r.htmlBody()).doesNotContain(">Klasio<");
}

@Test
void header_state3_noTenant_rendersKlasioBadgeAndKlasioWordmark() {
    RenderedTemplate r = renderer.render("password-recovery", Locale.ENGLISH, Map.of(
            "resetUrl", "http://e", "expiresAt", "x",
            "tenantSlug", "system",
            "loginUrl", "http://e"));
    // Both tenantName and tenantLogoUrl absent.

    assertThat(r.htmlBody()).contains("M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z");
    assertThat(r.htmlBody()).contains(">Klasio<");
}
```

- [ ] **Step 2: Run the tests — confirm they FAIL**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='ThymeleafTemplateRendererTest#header_state1_tenantWithLogo_rendersImgWithLogoUrl+ThymeleafTemplateRendererTest#header_state2_tenantWithoutLogo_rendersKlasioBadgeAndTenantName+ThymeleafTemplateRendererTest#header_state3_noTenant_rendersKlasioBadgeAndKlasioWordmark' test
```

Expected:
- `header_state1` FAILS — `<img>` not present (template still hardcodes badge).
- `header_state2` PASSES (current header already matches: badge + the inlined word "Klasio" hardcoded).
  - Actually it FAILS the `doesNotContain(">Klasio<")` assertion because the wordmark is currently hardcoded.
- `header_state3` PASSES on the badge, but the test design assumes only the wordmark, which is currently always present — so the assertion `>Klasio<` already holds. May pass.

The exact failure mix isn't critical — what matters is at least one test fails, locking in the state-driven expectation. Note which fail.

- [ ] **Step 3: Commit failing tests**

```bash
git add api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java
git commit -m "test(email): pin three-state header rendering"
```

---

## Task 10: Create the reusable header fragment

Add a Thymeleaf fragment that renders the three states. Wire it into one template first to validate, then propagate.

**Files:**
- Create: `api/src/main/resources/email-templates/layouts/header.html`

- [ ] **Step 1: Create the fragment file**

Create `api/src/main/resources/email-templates/layouts/header.html` with:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org">
<body>
<table th:fragment="header" role="presentation" cellpadding="0" cellspacing="0" width="100%" style="width:100%;">
  <tr>
    <td style="background:#0A0A0A;padding:28px 40px;">
      <table role="presentation" cellpadding="0" cellspacing="0" style="display:inline-table;">
        <tr>
          <td th:if="${tenantLogoUrl}" style="vertical-align:middle;">
            <img th:src="${tenantLogoUrl}" width="32" height="32"
                 style="display:block;width:32px;height:32px;border-radius:8px;object-fit:cover;"
                 alt=""/>
          </td>
          <td th:unless="${tenantLogoUrl}" style="vertical-align:middle;">
            <div style="width:32px;height:32px;background:#CAFF4D;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;">
              <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" fill="#0A0A0A"/>
              </svg>
            </div>
          </td>
          <td style="vertical-align:middle;padding-left:10px;">
            <span th:text="${tenantName != null ? tenantName : 'Klasio'}"
                  style="color:#FFFFFF;font-family:'DM Sans',-apple-system,sans-serif;font-size:18px;font-weight:700;letter-spacing:-0.02em;">Klasio</span>
          </td>
        </tr>
      </table>
    </td>
  </tr>
</table>
</body>
</html>
```

(The outer `<html>` / `<body>` are required by Thymeleaf for fragment files but are not emitted when included.)

- [ ] **Step 2: Wire the fragment into `password-recovery.html`** (validation template)

Open `api/src/main/resources/email-templates/password-recovery.html`. Locate the existing header block — the `<tr>` that contains the `<td style="background:#0A0A0A;padding:28px 40px;">` with the inlined badge + "Klasio" wordmark (current lines 14–32 of `membership-activated.html`; `password-recovery.html` will mirror that structure).

Replace the entire `<tr>...</tr>` row (the dark-header row) with:

```html
<tr>
  <td>
    <th:block th:replace="~{layouts/header :: header}"></th:block>
  </td>
</tr>
```

- [ ] **Step 3: Run the header tests — confirm all three PASS for `password-recovery`**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='ThymeleafTemplateRendererTest#header_state1_tenantWithLogo_rendersImgWithLogoUrl+ThymeleafTemplateRendererTest#header_state2_tenantWithoutLogo_rendersKlasioBadgeAndTenantName+ThymeleafTemplateRendererTest#header_state3_noTenant_rendersKlasioBadgeAndKlasioWordmark' test
```

Expected: PASS.

- [ ] **Step 4: Run full renderer test class**

```bash
mvn -q -Dtest='ThymeleafTemplateRendererTest' test
```

Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/email-templates/layouts/header.html \
        api/src/main/resources/email-templates/password-recovery.html
git commit -m "feat(email): add three-state header fragment, wire password-recovery"
```

---

## Task 11: Wire the header fragment into the remaining 9 templates

Replicate the Task 10 substitution across the other templates.

**Files:**
- Modify: `api/src/main/resources/email-templates/account-setup.html`
- Modify: `api/src/main/resources/email-templates/professor-invitation.html`
- Modify: `api/src/main/resources/email-templates/payment-proof-uploaded.html`
- Modify: `api/src/main/resources/email-templates/payment-rejected.html`
- Modify: `api/src/main/resources/email-templates/membership-activated.html`
- Modify: `api/src/main/resources/email-templates/membership-expiry-warning.html`
- Modify: `api/src/main/resources/email-templates/membership-depleted.html`
- Modify: `api/src/main/resources/email-templates/class-session-change.html`
- Modify: `api/src/main/resources/email-templates/missing-template-fallback.html`

- [ ] **Step 1: For each of the 9 files, replace the inline dark header block**

In each file, find the `<tr>` row whose `<td>` has `style="background:#0A0A0A;padding:28px 40px;"` along with the inlined Klasio badge + wordmark. Replace the entire `<tr>...</tr>` with:

```html
<tr>
  <td>
    <th:block th:replace="~{layouts/header :: header}"></th:block>
  </td>
</tr>
```

Do not touch the lime-green divider row (`<td style="height:3px;background:linear-gradient(...);">`) that follows — keep it as-is.

- [ ] **Step 2: Run the full renderer test class**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='ThymeleafTemplateRendererTest' test
```

Expected: PASS.

- [ ] **Step 3: Manually verify there are no leftover hardcoded headers**

```bash
grep -rln "padding:28px 40px;" api/src/main/resources/email-templates/ | grep -v layouts/
```

Expected: empty output (the dark-header style now lives only in the fragment).

```bash
grep -rln "M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" api/src/main/resources/email-templates/ | grep -v layouts/
```

Expected: empty (the SVG path lives only in the fragment).

- [ ] **Step 4: Commit**

```bash
git add api/src/main/resources/email-templates/
git commit -m "refactor(email): replace inline headers with shared fragment in 9 templates"
```

---

## Task 12: Render local previews and inspect

Confirm visually that subject text is gone and headers render correctly across states.

**Files:**
- (no code changes)

- [ ] **Step 1: Render previews for each template via the test harness**

The renderer test class already exercises every template; rendered HTML is asserted on but not saved to disk. To save preview snapshots, append this temporary test:

```java
@Test
void writePreviews() throws Exception {
    java.nio.file.Path out = java.nio.file.Path.of("local/email-previews");
    java.nio.file.Files.createDirectories(out);
    java.util.Map<String, java.util.Map<String, Object>> samples = java.util.Map.of(
        "account-setup-with-logo", java.util.Map.of(
            "templateRef", "account-setup",
            "model", java.util.Map.of(
                "recipientName", "Carlos", "role", "student",
                "setupUrl", "http://e", "expiresAt", "x",
                "tenantName", "Acme League", "tenantSlug", "acme",
                "tenantLogoUrl", "https://placehold.co/64x64/CAFF4D/0A0A0A.png",
                "loginUrl", "http://e")),
        "account-setup-no-logo", java.util.Map.of(
            "templateRef", "account-setup",
            "model", java.util.Map.of(
                "recipientName", "Carlos", "role", "student",
                "setupUrl", "http://e", "expiresAt", "x",
                "tenantName", "Acme League", "tenantSlug", "acme",
                "loginUrl", "http://e")),
        "account-setup-system", java.util.Map.of(
            "templateRef", "account-setup",
            "model", java.util.Map.of(
                "recipientName", "Carlos", "role", "student",
                "setupUrl", "http://e", "expiresAt", "x",
                "tenantSlug", "system", "loginUrl", "http://e")));
    samples.forEach((name, spec) -> {
        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> model = (java.util.Map<String, Object>) spec.get("model");
        RenderedTemplate r = renderer.render((String) spec.get("templateRef"),
                Locale.ENGLISH, model);
        try {
            java.nio.file.Files.writeString(out.resolve(name + ".html"), r.htmlBody());
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
    });
}
```

- [ ] **Step 2: Run the preview generator**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='ThymeleafTemplateRendererTest#writePreviews' test
```

- [ ] **Step 3: Inspect the three previews**

Open in a browser:
```
api/local/email-previews/account-setup-with-logo.html
api/local/email-previews/account-setup-no-logo.html
api/local/email-previews/account-setup-system.html
```

Verify:
- No subject text appears above the content card in any preview.
- `with-logo`: tenant logo image (32×32 lime placeholder) on the left, "Acme League" text to the right.
- `no-logo`: green lime badge with Klasio K mark, "Acme League" text to the right.
- `system`: green lime badge with Klasio K mark, "Klasio" text to the right.

- [ ] **Step 4: Remove the preview-generator test**

```bash
# Delete the writePreviews method from the test class.
```

- [ ] **Step 5: Confirm tests still pass**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q -Dtest='ThymeleafTemplateRendererTest' test
```

Expected: PASS.

- [ ] **Step 6: Commit cleanup (only if anything changed)**

```bash
git status
# If only preview HTML files changed under local/email-previews, no commit needed (gitignored).
# If the test file was edited and reverted cleanly, no commit needed.
```

---

## Task 13: Full backend test suite

Run the complete test suite to catch regressions in modules that consume the email path or `TenantContext`.

- [ ] **Step 1: Run all backend tests**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api
mvn -q test
```

Expected: PASS. Match or exceed the baseline count from the pre-flight check.

- [ ] **Step 2: If failures, fix and re-run**

If any unrelated test fails because it constructs a `TenantContext` directly, update the constructor call to pass `null` for the new `logoUrl` field. Search:

```bash
grep -rln "new TenantContext(" api/src/test/
```

Update each call site to include the 6th argument (`null` is fine for tests not specifically about the logo).

- [ ] **Step 3: Commit any fixes**

```bash
git add api/src/test/
git commit -m "test: update TenantContext constructor calls for new logoUrl field"
```

(Skip if Step 2 found nothing.)

---

## Task 14: Documentation — record the bucket policy task

The bucket policy that grants public read on `logos/*` is an ops change. Record it so it is not forgotten.

**Files:**
- Modify: `docs/superpowers/specs/2026-05-03-email-header-tenant-branding-design.md` (already covers it)
- Create: `docs/operations/2026-05-03-public-logos-bucket-policy.md`

- [ ] **Step 1: Create the ops note**

Create `docs/operations/2026-05-03-public-logos-bucket-policy.md` with:

```markdown
# Ops task: public read on klasio S3 `logos/*` prefix

**Date:** 2026-05-03
**Owner:** Ops
**Triggered by:** Email header tenant-branding feature (spec `docs/superpowers/specs/2026-05-03-email-header-tenant-branding-design.md`).

## What

Grant `s3:GetObject` public read on the `logos/*` prefix of the klasio S3 bucket so transactional emails can render tenant logos via stable public URLs that survive past the 1-hour presigned URL TTL.

## Why

Email clients open messages days, weeks, or months later. Presigned URLs expire and would render as broken images. Tenant logos are brand assets meant for public display, so a scoped public-read policy is acceptable.

## Bucket policy snippet

Append to the bucket policy (replace `<bucket>` with the production bucket name):

```json
{
  "Sid": "PublicReadLogos",
  "Effect": "Allow",
  "Principal": "*",
  "Action": "s3:GetObject",
  "Resource": "arn:aws:s3:::<bucket>/logos/*"
}
```

## Scope guard

The policy must apply ONLY to the `logos/*` prefix. Do not broaden the `Resource` to `arn:aws:s3:::<bucket>/*` — payment proofs and other private objects must remain private.

## Verification

After applying:

```bash
# From an unauthenticated shell
curl -I "https://<bucket>.s3.<region>.amazonaws.com/logos/<known-tenant-id>/<known-key>.png"
```

Expected: `HTTP/1.1 200 OK`.

```bash
curl -I "https://<bucket>.s3.<region>.amazonaws.com/payment-proofs/<known-key>.pdf"
```

Expected: `HTTP/1.1 403 Forbidden`.
```

- [ ] **Step 2: Commit**

```bash
mkdir -p docs/operations
git add docs/operations/2026-05-03-public-logos-bucket-policy.md
git commit -m "docs(ops): record public-read s3 policy task for tenant logos"
```

---

## Done criteria

- All renderer tests in `ThymeleafTemplateRendererTest` pass, including the new leak test and three header-state tests.
- `S3LogoStorageIntegrationTest` passes including the two new `getPublicUrl` tests.
- `JpaTenantContextAdapterTest` passes including the new `logoUrl` tests.
- `EmailDispatcherServiceTest` passes including the `tenantLogoUrl`-in-model tests.
- Full `mvn test` passes with no regressions.
- Manual preview inspection (Task 12) shows correct rendering for all three header states and no subject leak.
- Ops bucket-policy task is recorded.
