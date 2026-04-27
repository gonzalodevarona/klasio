# Email Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace all 9 Klasio transactional email templates with a DM Sans design system — dark header, accent bar, tinted info panels, and fully i18n panel labels.

**Architecture:** Each HTML template is self-contained (no shared Thymeleaf layout fragment). New `label.*` i18n keys are added to both `.properties` files so panel labels are fully translated. No Java source changes. The existing `ThymeleafTemplateRenderer` test class gets one new test method per template to drive the TDD cycle.

**Tech Stack:** Thymeleaf 3 (Spring Boot 3.4.3), JUnit 5 + AssertJ, Maven (`cd api && mvn test -Dtest="ThymeleafTemplateRendererTest"`)

---

## Files Modified

| File | Change |
|------|--------|
| `api/src/main/resources/email/i18n/messages_en.properties` | Add `label.*` + `accountSetup.tempLink` keys |
| `api/src/main/resources/email/i18n/messages_es.properties` | Same keys in Spanish |
| `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java` | Add one test method per template |
| `api/src/main/resources/email-templates/account-setup.html` | Full rewrite |
| `api/src/main/resources/email-templates/professor-invitation.html` | Full rewrite |
| `api/src/main/resources/email-templates/password-recovery.html` | Full rewrite |
| `api/src/main/resources/email-templates/payment-proof-uploaded.html` | Full rewrite |
| `api/src/main/resources/email-templates/payment-rejected.html` | Full rewrite |
| `api/src/main/resources/email-templates/membership-activated.html` | Full rewrite |
| `api/src/main/resources/email-templates/membership-expiry-warning.html` | Full rewrite |
| `api/src/main/resources/email-templates/membership-depleted.html` | Full rewrite |
| `api/src/main/resources/email-templates/class-session-change.html` | Full rewrite |
| `api/src/main/resources/email-templates/missing-template-fallback.html` | Full rewrite |

**Do NOT touch:** any `.txt` file, any `.java` source file, `layouts/base.html`.

---

## Task 1: Add i18n panel label keys

**Files:**
- Modify: `api/src/main/resources/email/i18n/messages_en.properties`
- Modify: `api/src/main/resources/email/i18n/messages_es.properties`

- [ ] **Step 1: Append new keys to messages_en.properties**

At the end of the file add:

```properties

# Panel labels (shared across templates)
label.student=Student
label.program=Program
label.plan=Plan
label.availableHours=Available hours
label.expiresOn=Expires on
label.remainingHours=Remaining hours
label.changeType=Change type
label.reason=Reason
accountSetup.tempLink=⏱ Temporary link
```

- [ ] **Step 2: Append same keys (Spanish) to messages_es.properties**

At the end of the file add:

```properties

# Panel labels (shared across templates)
label.student=Estudiante
label.program=Programa
label.plan=Plan
label.availableHours=Horas disponibles
label.expiresOn=Expira el
label.remainingHours=Horas restantes
label.changeType=Tipo de cambio
label.reason=Motivo
accountSetup.tempLink=⏱ Enlace temporal
```

- [ ] **Step 3: Verify existing tests still pass (no regression)**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest" -q
```

Expected: `BUILD SUCCESS` — all 4 existing tests green.

- [ ] **Step 4: Commit**

```bash
git add api/src/main/resources/email/i18n/messages_en.properties \
        api/src/main/resources/email/i18n/messages_es.properties
git commit -m "feat(email): add i18n label keys for redesigned info panels"
```

---

## Task 2: account-setup.html

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`
- Modify: `api/src/main/resources/email-templates/account-setup.html`

- [ ] **Step 1: Add failing test**

Add this method to `ThymeleafTemplateRendererTest`:

```java
@Test
void accountSetup_usesNewDesignAndResolvesAllKeys() {
    RenderedTemplate result = renderer.render("account-setup", Locale.ENGLISH, Map.of(
            "recipientName", "María García",
            "role", "student",
            "setupUrl", "https://test.klasio.com/setup?token=abc",
            "expiresAt", "2026-05-01 09:00",
            "tenantName", "Test League",
            "tenantSlug", "test-league",
            "loginUrl", "http://localhost:3000"));

    assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
    assertThat(result.htmlBody()).contains("DM Sans");
    assertThat(result.htmlBody()).contains("#CAFF4D");
    assertThat(result.htmlBody()).contains("María García");
    assertThat(result.htmlBody()).contains("https://test.klasio.com/setup?token=abc");
    assertThat(result.htmlBody()).contains("Temporary link");
}
```

- [ ] **Step 2: Run test — verify it FAILS**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest#accountSetup_usesNewDesignAndResolvesAllKeys" -q
```

Expected: FAIL — `expected: to contain: "DM Sans"` (old template uses Arial).

- [ ] **Step 3: Rewrite account-setup.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <th:block th:fragment="subject" th:text="#{accountSetup.subject(${tenantName})}"></th:block>
  <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
</head>
<body style="margin:0;padding:0;background:#F4F4F2;font-family:'DM Sans',-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#F4F4F2;padding:40px 20px;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 2px 24px rgba(0,0,0,0.08);">
          <tr>
            <td style="background:#0A0A0A;padding:28px 40px;">
              <table role="presentation" cellpadding="0" cellspacing="0" style="display:inline-table;">
                <tr>
                  <td style="vertical-align:middle;">
                    <div style="width:32px;height:32px;background:#CAFF4D;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;">
                      <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" fill="#0A0A0A"/></svg>
                    </div>
                  </td>
                  <td style="vertical-align:middle;padding-left:10px;">
                    <span style="color:#FFFFFF;font-family:'DM Sans',-apple-system,sans-serif;font-size:18px;font-weight:700;letter-spacing:-0.02em;">Klasio</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="height:3px;background:linear-gradient(90deg,#CAFF4D 0%,rgba(202,255,77,0.5) 60%,transparent 100%);font-size:0;line-height:0;">&nbsp;</td>
          </tr>
          <tr>
            <td style="padding:32px 40px;">
              <p style="margin:0 0 12px;font-size:16px;color:#1A1A1A;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;">
                <span th:text="#{accountSetup.hi}">Hi</span>,
                <strong style="color:#0A0A0A;" th:text="${recipientName}">User</strong>
              </p>
              <p style="margin:0 0 20px;font-size:15px;color:#3A3A38;line-height:1.7;font-family:'DM Sans',-apple-system,sans-serif;"
                 th:text="#{accountSetup.body(${role}, ${tenantName})}">Your account is ready.</p>
              <div style="margin:28px 0;">
                <a th:href="${setupUrl}"
                   style="display:inline-block;background:#0A0A0A;color:#CAFF4D;padding:13px 28px;text-decoration:none;border-radius:8px;font-size:14px;font-weight:700;font-family:'DM Sans',-apple-system,sans-serif;letter-spacing:0.01em;"
                   th:text="#{accountSetup.cta}">Set Up My Account</a>
              </div>
              <div style="background:#FFF8F0;border:1px solid #FFD9A8;border-radius:8px;padding:14px 18px;">
                <span style="display:block;font-size:12px;font-weight:700;color:#7A4000;letter-spacing:0.08em;text-transform:uppercase;font-family:'DM Mono',monospace;margin-bottom:6px;"
                      th:text="#{accountSetup.tempLink}">⏱ Temporary link</span>
                <p style="margin:0;font-size:14px;color:#5A3000;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;"
                   th:text="#{accountSetup.expiry(${expiresAt})}">This link expires soon.</p>
              </div>
            </td>
          </tr>
          <tr>
            <td style="border-top:1px solid #F0F0EE;padding:20px 40px 28px;">
              <p style="margin:0 0 4px;font-family:'DM Sans',-apple-system,sans-serif;font-size:12px;color:#AAAAAA;line-height:1.6;"
                 th:text="#{layout.footer(${tenantName})}">Sent by Academy via Klasio.</p>
              <p style="margin:0;font-family:'DM Mono',monospace;font-size:11px;color:#C8C8C6;">© 2026 Klasio · Todos los derechos reservados</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

- [ ] **Step 4: Run test — verify it PASSES**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest" -q
```

Expected: `BUILD SUCCESS` — all 5 tests green.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/email-templates/account-setup.html \
        api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java
git commit -m "feat(email): redesign account-setup template"
```

---

## Task 3: professor-invitation.html

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`
- Modify: `api/src/main/resources/email-templates/professor-invitation.html`

- [ ] **Step 1: Add failing test**

```java
@Test
void professorInvitation_rendersCorrectly() {
    RenderedTemplate result = renderer.render("professor-invitation", Locale.ENGLISH, Map.of(
            "professorName", "Carlos Ruiz",
            "activationUrl", "https://test.klasio.com/setup?token=inv123",
            "expiresAt", "2026-05-05 18:00",
            "tenantName", "Test League",
            "tenantSlug", "test-league",
            "loginUrl", "http://localhost:3000"));

    assertThat(result.subject()).isEqualTo("You have been invited to join Test League as a professor");
    assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
    assertThat(result.htmlBody()).contains("DM Sans");
    assertThat(result.htmlBody()).contains("#CAFF4D");
    assertThat(result.htmlBody()).contains("Carlos Ruiz");
    assertThat(result.htmlBody()).contains("https://test.klasio.com/setup?token=inv123");
}
```

- [ ] **Step 2: Run test — verify it FAILS**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest#professorInvitation_rendersCorrectly" -q
```

Expected: FAIL — `expected: to contain: "DM Sans"`.

- [ ] **Step 3: Rewrite professor-invitation.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <th:block th:fragment="subject" th:text="#{professorInvitation.subject(${tenantName})}"></th:block>
  <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
</head>
<body style="margin:0;padding:0;background:#F4F4F2;font-family:'DM Sans',-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#F4F4F2;padding:40px 20px;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 2px 24px rgba(0,0,0,0.08);">
          <tr>
            <td style="background:#0A0A0A;padding:28px 40px;">
              <table role="presentation" cellpadding="0" cellspacing="0" style="display:inline-table;">
                <tr>
                  <td style="vertical-align:middle;">
                    <div style="width:32px;height:32px;background:#CAFF4D;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;">
                      <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" fill="#0A0A0A"/></svg>
                    </div>
                  </td>
                  <td style="vertical-align:middle;padding-left:10px;">
                    <span style="color:#FFFFFF;font-family:'DM Sans',-apple-system,sans-serif;font-size:18px;font-weight:700;letter-spacing:-0.02em;">Klasio</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="height:3px;background:linear-gradient(90deg,#CAFF4D 0%,rgba(202,255,77,0.5) 60%,transparent 100%);font-size:0;line-height:0;">&nbsp;</td>
          </tr>
          <tr>
            <td style="padding:32px 40px;">
              <p style="margin:0 0 12px;font-size:16px;color:#1A1A1A;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;">
                <span th:text="#{professorInvitation.hi}">Hi</span>,
                <strong style="color:#0A0A0A;" th:text="${professorName}">Professor</strong>
              </p>
              <p style="margin:0 0 20px;font-size:15px;color:#3A3A38;line-height:1.7;font-family:'DM Sans',-apple-system,sans-serif;"
                 th:text="#{professorInvitation.body(${tenantName})}">You have been invited.</p>
              <div style="margin:28px 0;">
                <a th:href="${activationUrl}"
                   style="display:inline-block;background:#0A0A0A;color:#CAFF4D;padding:13px 28px;text-decoration:none;border-radius:8px;font-size:14px;font-weight:700;font-family:'DM Sans',-apple-system,sans-serif;letter-spacing:0.01em;"
                   th:text="#{professorInvitation.cta}">Accept Invitation</a>
              </div>
              <p style="margin:12px 0 0;font-size:13px;color:#8A8A88;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;"
                 th:text="#{professorInvitation.expiry(${expiresAt})}">This invitation expires soon.</p>
            </td>
          </tr>
          <tr>
            <td style="border-top:1px solid #F0F0EE;padding:20px 40px 28px;">
              <p style="margin:0 0 4px;font-family:'DM Sans',-apple-system,sans-serif;font-size:12px;color:#AAAAAA;line-height:1.6;"
                 th:text="#{layout.footer(${tenantName})}">Sent by Academy via Klasio.</p>
              <p style="margin:0;font-family:'DM Mono',monospace;font-size:11px;color:#C8C8C6;">© 2026 Klasio · Todos los derechos reservados</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

- [ ] **Step 4: Run test — verify it PASSES**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest" -q
```

Expected: `BUILD SUCCESS` — all 6 tests green.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/email-templates/professor-invitation.html \
        api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java
git commit -m "feat(email): redesign professor-invitation template"
```

---

## Task 4: password-recovery.html

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`
- Modify: `api/src/main/resources/email-templates/password-recovery.html`

- [ ] **Step 1: Add failing test**

```java
@Test
void passwordRecovery_usesNewDesign() {
    RenderedTemplate result = renderer.render("password-recovery", Locale.ENGLISH, Map.of(
            "resetUrl", "https://test.klasio.com/reset?token=xyz",
            "expiresAt", "30 minutes",
            "tenantName", "Test League",
            "tenantSlug", "test-league",
            "loginUrl", "http://localhost:3000"));

    assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
    assertThat(result.htmlBody()).contains("DM Sans");
    assertThat(result.htmlBody()).contains("#CAFF4D");
    assertThat(result.htmlBody()).contains("https://test.klasio.com/reset?token=xyz");
}
```

- [ ] **Step 2: Run test — verify it FAILS**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest#passwordRecovery_usesNewDesign" -q
```

Expected: FAIL — `expected: to contain: "DM Sans"`.

- [ ] **Step 3: Rewrite password-recovery.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <th:block th:fragment="subject" th:text="#{passwordRecovery.subject(${tenantName})}"></th:block>
  <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
</head>
<body style="margin:0;padding:0;background:#F4F4F2;font-family:'DM Sans',-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#F4F4F2;padding:40px 20px;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 2px 24px rgba(0,0,0,0.08);">
          <tr>
            <td style="background:#0A0A0A;padding:28px 40px;">
              <table role="presentation" cellpadding="0" cellspacing="0" style="display:inline-table;">
                <tr>
                  <td style="vertical-align:middle;">
                    <div style="width:32px;height:32px;background:#CAFF4D;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;">
                      <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" fill="#0A0A0A"/></svg>
                    </div>
                  </td>
                  <td style="vertical-align:middle;padding-left:10px;">
                    <span style="color:#FFFFFF;font-family:'DM Sans',-apple-system,sans-serif;font-size:18px;font-weight:700;letter-spacing:-0.02em;">Klasio</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="height:3px;background:linear-gradient(90deg,#CAFF4D 0%,rgba(202,255,77,0.5) 60%,transparent 100%);font-size:0;line-height:0;">&nbsp;</td>
          </tr>
          <tr>
            <td style="padding:32px 40px;">
              <p style="margin:0 0 20px;font-size:15px;color:#3A3A38;line-height:1.7;font-family:'DM Sans',-apple-system,sans-serif;"
                 th:text="#{passwordRecovery.intro}">You requested a password reset.</p>
              <div style="margin:28px 0;">
                <a th:href="${resetUrl}"
                   style="display:inline-block;background:#0A0A0A;color:#CAFF4D;padding:13px 28px;text-decoration:none;border-radius:8px;font-size:14px;font-weight:700;font-family:'DM Sans',-apple-system,sans-serif;letter-spacing:0.01em;"
                   th:text="#{passwordRecovery.cta}">Reset Password</a>
              </div>
              <p style="margin:12px 0 0;font-size:13px;color:#8A8A88;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;"
                 th:text="#{passwordRecovery.expiry(${expiresAt})}">This link expires soon.</p>
              <p style="margin:8px 0 0;font-size:13px;color:#8A8A88;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;"
                 th:text="#{passwordRecovery.noRequest}">If you didn't request this, ignore it.</p>
            </td>
          </tr>
          <tr>
            <td style="border-top:1px solid #F0F0EE;padding:20px 40px 28px;">
              <p style="margin:0 0 4px;font-family:'DM Sans',-apple-system,sans-serif;font-size:12px;color:#AAAAAA;line-height:1.6;"
                 th:text="#{layout.footer(${tenantName})}">Sent by Academy via Klasio.</p>
              <p style="margin:0;font-family:'DM Mono',monospace;font-size:11px;color:#C8C8C6;">© 2026 Klasio · Todos los derechos reservados</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

- [ ] **Step 4: Run test — verify it PASSES**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest" -q
```

Expected: `BUILD SUCCESS` — all 7 tests green.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/email-templates/password-recovery.html \
        api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java
git commit -m "feat(email): redesign password-recovery template"
```

---

## Task 5: payment-proof-uploaded.html

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`
- Modify: `api/src/main/resources/email-templates/payment-proof-uploaded.html`

- [ ] **Step 1: Add failing test**

```java
@Test
void paymentProofUploaded_rendersInfoPanel() {
    RenderedTemplate result = renderer.render("payment-proof-uploaded", Locale.ENGLISH, Map.of(
            "studentName", "Laura Torres",
            "programName", "Boxing Beginners",
            "reviewUrl", "https://test.klasio.com/payment-proofs/42",
            "tenantName", "Test League",
            "tenantSlug", "test-league",
            "loginUrl", "http://localhost:3000"));

    assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
    assertThat(result.htmlBody()).contains("DM Sans");
    assertThat(result.htmlBody()).contains("Laura Torres");
    assertThat(result.htmlBody()).contains("Boxing Beginners");
    assertThat(result.htmlBody()).contains("Student");
    assertThat(result.htmlBody()).contains("Program");
    assertThat(result.htmlBody()).contains("https://test.klasio.com/payment-proofs/42");
}
```

- [ ] **Step 2: Run test — verify it FAILS**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest#paymentProofUploaded_rendersInfoPanel" -q
```

Expected: FAIL — `expected: to contain: "DM Sans"`.

- [ ] **Step 3: Rewrite payment-proof-uploaded.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <th:block th:fragment="subject" th:text="#{paymentProofUploaded.subject(${studentName})}"></th:block>
  <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
</head>
<body style="margin:0;padding:0;background:#F4F4F2;font-family:'DM Sans',-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#F4F4F2;padding:40px 20px;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 2px 24px rgba(0,0,0,0.08);">
          <tr>
            <td style="background:#0A0A0A;padding:28px 40px;">
              <table role="presentation" cellpadding="0" cellspacing="0" style="display:inline-table;">
                <tr>
                  <td style="vertical-align:middle;">
                    <div style="width:32px;height:32px;background:#CAFF4D;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;">
                      <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" fill="#0A0A0A"/></svg>
                    </div>
                  </td>
                  <td style="vertical-align:middle;padding-left:10px;">
                    <span style="color:#FFFFFF;font-family:'DM Sans',-apple-system,sans-serif;font-size:18px;font-weight:700;letter-spacing:-0.02em;">Klasio</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="height:3px;background:linear-gradient(90deg,#CAFF4D 0%,rgba(202,255,77,0.5) 60%,transparent 100%);font-size:0;line-height:0;">&nbsp;</td>
          </tr>
          <tr>
            <td style="padding:32px 40px;">
              <p style="margin:0 0 20px;font-size:15px;color:#3A3A38;line-height:1.7;font-family:'DM Sans',-apple-system,sans-serif;"
                 th:text="#{paymentProofUploaded.body(${studentName}, ${programName})}">A student submitted a proof.</p>
              <div style="background:#F8F8F6;border:1px solid #E8E8E6;border-radius:10px;padding:4px 0;margin-bottom:20px;">
                <div style="display:flex;gap:8px;padding:10px 18px;border-bottom:1px solid #F0F0EE;font-size:14px;font-family:'DM Sans',-apple-system,sans-serif;">
                  <span style="color:#8A8A88;min-width:130px;" th:text="#{label.student}">Student</span>
                  <span style="color:#1A1A1A;font-weight:500;" th:text="${studentName}">Name</span>
                </div>
                <div style="display:flex;gap:8px;padding:10px 18px;font-size:14px;font-family:'DM Sans',-apple-system,sans-serif;">
                  <span style="color:#8A8A88;min-width:130px;" th:text="#{label.program}">Program</span>
                  <span style="color:#1A1A1A;font-weight:500;" th:text="${programName}">Program</span>
                </div>
              </div>
              <div style="margin:28px 0;">
                <a th:href="${reviewUrl}"
                   style="display:inline-block;background:#0A0A0A;color:#CAFF4D;padding:13px 28px;text-decoration:none;border-radius:8px;font-size:14px;font-weight:700;font-family:'DM Sans',-apple-system,sans-serif;letter-spacing:0.01em;"
                   th:text="#{paymentProofUploaded.cta}">Review Proof</a>
              </div>
            </td>
          </tr>
          <tr>
            <td style="border-top:1px solid #F0F0EE;padding:20px 40px 28px;">
              <p style="margin:0 0 4px;font-family:'DM Sans',-apple-system,sans-serif;font-size:12px;color:#AAAAAA;line-height:1.6;"
                 th:text="#{layout.footer(${tenantName})}">Sent by Academy via Klasio.</p>
              <p style="margin:0;font-family:'DM Mono',monospace;font-size:11px;color:#C8C8C6;">© 2026 Klasio · Todos los derechos reservados</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

- [ ] **Step 4: Run test — verify it PASSES**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest" -q
```

Expected: `BUILD SUCCESS` — all 8 tests green.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/email-templates/payment-proof-uploaded.html \
        api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java
git commit -m "feat(email): redesign payment-proof-uploaded template"
```

---

## Task 6: payment-rejected.html

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`
- Modify: `api/src/main/resources/email-templates/payment-rejected.html`

- [ ] **Step 1: Add failing test**

```java
@Test
void paymentRejected_rendersReasonCallout() {
    RenderedTemplate result = renderer.render("payment-rejected", Locale.ENGLISH, Map.of(
            "studentName", "Laura Torres",
            "programName", "Boxing Beginners",
            "reason", "Image is blurry and amount is not visible.",
            "retryUrl", "https://test.klasio.com/memberships/99/upload",
            "tenantName", "Test League",
            "tenantSlug", "test-league",
            "loginUrl", "http://localhost:3000"));

    assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
    assertThat(result.htmlBody()).contains("DM Sans");
    assertThat(result.htmlBody()).contains("Laura Torres");
    assertThat(result.htmlBody()).contains("Image is blurry and amount is not visible.");
    assertThat(result.htmlBody()).contains("https://test.klasio.com/memberships/99/upload");
    assertThat(result.htmlBody()).contains("#FFF5F5");
}
```

- [ ] **Step 2: Run test — verify it FAILS**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest#paymentRejected_rendersReasonCallout" -q
```

Expected: FAIL.

- [ ] **Step 3: Rewrite payment-rejected.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <th:block th:fragment="subject" th:text="#{paymentRejected.subject(${programName})}"></th:block>
  <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
</head>
<body style="margin:0;padding:0;background:#F4F4F2;font-family:'DM Sans',-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#F4F4F2;padding:40px 20px;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 2px 24px rgba(0,0,0,0.08);">
          <tr>
            <td style="background:#0A0A0A;padding:28px 40px;">
              <table role="presentation" cellpadding="0" cellspacing="0" style="display:inline-table;">
                <tr>
                  <td style="vertical-align:middle;">
                    <div style="width:32px;height:32px;background:#CAFF4D;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;">
                      <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" fill="#0A0A0A"/></svg>
                    </div>
                  </td>
                  <td style="vertical-align:middle;padding-left:10px;">
                    <span style="color:#FFFFFF;font-family:'DM Sans',-apple-system,sans-serif;font-size:18px;font-weight:700;letter-spacing:-0.02em;">Klasio</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="height:3px;background:linear-gradient(90deg,#CAFF4D 0%,rgba(202,255,77,0.5) 60%,transparent 100%);font-size:0;line-height:0;">&nbsp;</td>
          </tr>
          <tr>
            <td style="padding:32px 40px;">
              <p style="margin:0 0 12px;font-size:16px;color:#1A1A1A;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;">
                <span th:text="#{paymentRejected.hi}">Hi</span>,
                <strong style="color:#0A0A0A;" th:text="${studentName}">Student</strong>
              </p>
              <p style="margin:0 0 20px;font-size:15px;color:#3A3A38;line-height:1.7;font-family:'DM Sans',-apple-system,sans-serif;"
                 th:text="#{paymentRejected.body(${programName})}">Your proof was not accepted.</p>
              <div style="background:#FFF5F5;border:1px solid #FFD1D1;border-radius:8px;padding:14px 18px;margin-bottom:20px;">
                <span style="display:block;font-size:12px;font-weight:700;color:#C43030;letter-spacing:0.08em;text-transform:uppercase;font-family:'DM Mono',monospace;margin-bottom:6px;"
                      th:text="#{paymentRejected.reason}">Reason:</span>
                <p style="margin:0;font-size:14px;color:#5A1A1A;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;"
                   th:text="${reason}">Rejection reason.</p>
              </div>
              <div style="margin:28px 0;">
                <a th:href="${retryUrl}"
                   style="display:inline-block;background:#0A0A0A;color:#CAFF4D;padding:13px 28px;text-decoration:none;border-radius:8px;font-size:14px;font-weight:700;font-family:'DM Sans',-apple-system,sans-serif;letter-spacing:0.01em;"
                   th:text="#{paymentRejected.cta}">Submit Again</a>
              </div>
            </td>
          </tr>
          <tr>
            <td style="border-top:1px solid #F0F0EE;padding:20px 40px 28px;">
              <p style="margin:0 0 4px;font-family:'DM Sans',-apple-system,sans-serif;font-size:12px;color:#AAAAAA;line-height:1.6;"
                 th:text="#{layout.footer(${tenantName})}">Sent by Academy via Klasio.</p>
              <p style="margin:0;font-family:'DM Mono',monospace;font-size:11px;color:#C8C8C6;">© 2026 Klasio · Todos los derechos reservados</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

- [ ] **Step 4: Run test — verify it PASSES**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest" -q
```

Expected: `BUILD SUCCESS` — all 9 tests green.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/email-templates/payment-rejected.html \
        api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java
git commit -m "feat(email): redesign payment-rejected template"
```

---

## Task 7: membership-activated.html

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`
- Modify: `api/src/main/resources/email-templates/membership-activated.html`

- [ ] **Step 1: Add failing test**

```java
@Test
void membershipActivated_rendersGreenPanelWithNewDesign() {
    RenderedTemplate result = renderer.render("membership-activated", Locale.ENGLISH, Map.of(
            "studentName", "Ana",
            "programName", "Tennis",
            "planName", "Monthly 20h",
            "totalHours", 20,
            "expiresAt", "2026-05-31",
            "tenantName", "Tennis Club",
            "tenantSlug", "tennis-club",
            "loginUrl", "http://localhost:3000"));

    assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
    assertThat(result.htmlBody()).contains("DM Sans");
    assertThat(result.htmlBody()).contains("#F4FAF6");
    assertThat(result.htmlBody()).contains("Monthly 20h");
    assertThat(result.htmlBody()).contains("Available hours");
    assertThat(result.htmlBody()).contains("Expires on");
    assertThat(result.htmlBody()).contains("http://localhost:3000");
}
```

- [ ] **Step 2: Run test — verify it FAILS**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest#membershipActivated_rendersGreenPanelWithNewDesign" -q
```

Expected: FAIL.

- [ ] **Step 3: Rewrite membership-activated.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <th:block th:fragment="subject" th:text="#{membershipActivated.subject}"></th:block>
  <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
</head>
<body style="margin:0;padding:0;background:#F4F4F2;font-family:'DM Sans',-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#F4F4F2;padding:40px 20px;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 2px 24px rgba(0,0,0,0.08);">
          <tr>
            <td style="background:#0A0A0A;padding:28px 40px;">
              <table role="presentation" cellpadding="0" cellspacing="0" style="display:inline-table;">
                <tr>
                  <td style="vertical-align:middle;">
                    <div style="width:32px;height:32px;background:#CAFF4D;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;">
                      <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" fill="#0A0A0A"/></svg>
                    </div>
                  </td>
                  <td style="vertical-align:middle;padding-left:10px;">
                    <span style="color:#FFFFFF;font-family:'DM Sans',-apple-system,sans-serif;font-size:18px;font-weight:700;letter-spacing:-0.02em;">Klasio</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="height:3px;background:linear-gradient(90deg,#CAFF4D 0%,rgba(202,255,77,0.5) 60%,transparent 100%);font-size:0;line-height:0;">&nbsp;</td>
          </tr>
          <tr>
            <td style="padding:32px 40px;">
              <p style="margin:0 0 12px;font-size:16px;color:#1A1A1A;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;">
                <span th:text="#{membershipActivated.hi}">Hi</span>,
                <strong style="color:#0A0A0A;" th:text="${studentName}">Student</strong>
              </p>
              <p style="margin:0 0 20px;font-size:15px;color:#3A3A38;line-height:1.7;font-family:'DM Sans',-apple-system,sans-serif;"
                 th:text="#{membershipActivated.body(${programName})}">Your membership has been activated.</p>
              <div style="background:#F4FAF6;border:1px solid #BDE8CB;border-radius:10px;padding:4px 0;margin-bottom:20px;">
                <div style="display:flex;gap:8px;padding:10px 18px;border-bottom:1px solid #F0F0EE;font-size:14px;font-family:'DM Sans',-apple-system,sans-serif;">
                  <span style="color:#8A8A88;min-width:130px;" th:text="#{label.plan}">Plan</span>
                  <span style="color:#1A1A1A;font-weight:500;" th:text="${planName}">Plan name</span>
                </div>
                <div style="display:flex;gap:8px;padding:10px 18px;border-bottom:1px solid #F0F0EE;font-size:14px;font-family:'DM Sans',-apple-system,sans-serif;">
                  <span style="color:#8A8A88;min-width:130px;" th:text="#{label.availableHours}">Available hours</span>
                  <span style="color:#1A1A1A;font-weight:500;" th:text="${totalHours}">20</span>
                </div>
                <div style="display:flex;gap:8px;padding:10px 18px;font-size:14px;font-family:'DM Sans',-apple-system,sans-serif;">
                  <span style="color:#8A8A88;min-width:130px;" th:text="#{label.expiresOn}">Expires on</span>
                  <span style="color:#1A1A1A;font-weight:500;" th:text="${expiresAt}">Date</span>
                </div>
              </div>
              <div style="margin:28px 0;">
                <a th:href="${loginUrl}"
                   style="display:inline-block;background:#0A0A0A;color:#CAFF4D;padding:13px 28px;text-decoration:none;border-radius:8px;font-size:14px;font-weight:700;font-family:'DM Sans',-apple-system,sans-serif;letter-spacing:0.01em;"
                   th:text="#{membershipActivated.cta}">Go to My Account</a>
              </div>
            </td>
          </tr>
          <tr>
            <td style="border-top:1px solid #F0F0EE;padding:20px 40px 28px;">
              <p style="margin:0 0 4px;font-family:'DM Sans',-apple-system,sans-serif;font-size:12px;color:#AAAAAA;line-height:1.6;"
                 th:text="#{layout.footer(${tenantName})}">Sent by Academy via Klasio.</p>
              <p style="margin:0;font-family:'DM Mono',monospace;font-size:11px;color:#C8C8C6;">© 2026 Klasio · Todos los derechos reservados</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

- [ ] **Step 4: Run test — verify it PASSES**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest" -q
```

Expected: `BUILD SUCCESS` — all 10 tests green.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/email-templates/membership-activated.html \
        api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java
git commit -m "feat(email): redesign membership-activated template"
```

---

## Task 8: membership-expiry-warning.html

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`
- Modify: `api/src/main/resources/email-templates/membership-expiry-warning.html`

- [ ] **Step 1: Add failing test**

```java
@Test
void membershipExpiryWarning_rendersAmberPanel() {
    RenderedTemplate result = renderer.render("membership-expiry-warning", Locale.ENGLISH, Map.of(
            "studentName", "Daniela Ríos",
            "programName", "Yoga Adults",
            "remainingHours", 2,
            "expiresAt", "2026-04-30",
            "tenantName", "Test League",
            "tenantSlug", "test-league",
            "loginUrl", "http://localhost:3000"));

    assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
    assertThat(result.htmlBody()).contains("DM Sans");
    assertThat(result.htmlBody()).contains("#FFFAF0");
    assertThat(result.htmlBody()).contains("Remaining hours");
    assertThat(result.htmlBody()).contains("Expires on");
    assertThat(result.htmlBody()).contains("Daniela Ríos");
}
```

- [ ] **Step 2: Run test — verify it FAILS**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest#membershipExpiryWarning_rendersAmberPanel" -q
```

Expected: FAIL.

- [ ] **Step 3: Rewrite membership-expiry-warning.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <th:block th:fragment="subject" th:text="#{membershipExpiryWarning.subject}"></th:block>
  <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
</head>
<body style="margin:0;padding:0;background:#F4F4F2;font-family:'DM Sans',-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#F4F4F2;padding:40px 20px;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 2px 24px rgba(0,0,0,0.08);">
          <tr>
            <td style="background:#0A0A0A;padding:28px 40px;">
              <table role="presentation" cellpadding="0" cellspacing="0" style="display:inline-table;">
                <tr>
                  <td style="vertical-align:middle;">
                    <div style="width:32px;height:32px;background:#CAFF4D;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;">
                      <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" fill="#0A0A0A"/></svg>
                    </div>
                  </td>
                  <td style="vertical-align:middle;padding-left:10px;">
                    <span style="color:#FFFFFF;font-family:'DM Sans',-apple-system,sans-serif;font-size:18px;font-weight:700;letter-spacing:-0.02em;">Klasio</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="height:3px;background:linear-gradient(90deg,#CAFF4D 0%,rgba(202,255,77,0.5) 60%,transparent 100%);font-size:0;line-height:0;">&nbsp;</td>
          </tr>
          <tr>
            <td style="padding:32px 40px;">
              <p style="margin:0 0 12px;font-size:16px;color:#1A1A1A;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;">
                <span th:text="#{membershipExpiryWarning.hi}">Hi</span>,
                <strong style="color:#0A0A0A;" th:text="${studentName}">Student</strong>
              </p>
              <p style="margin:0 0 20px;font-size:15px;color:#3A3A38;line-height:1.7;font-family:'DM Sans',-apple-system,sans-serif;"
                 th:text="#{membershipExpiryWarning.body(${programName})}">Your membership is expiring soon.</p>
              <div style="background:#FFFAF0;border:1px solid #FFE4A8;border-radius:10px;padding:4px 0;margin-bottom:20px;">
                <div style="display:flex;gap:8px;padding:10px 18px;border-bottom:1px solid #F0F0EE;font-size:14px;font-family:'DM Sans',-apple-system,sans-serif;">
                  <span style="color:#8A8A88;min-width:130px;" th:text="#{label.remainingHours}">Remaining hours</span>
                  <span style="color:#1A1A1A;font-weight:500;" th:text="${remainingHours}">2</span>
                </div>
                <div style="display:flex;gap:8px;padding:10px 18px;font-size:14px;font-family:'DM Sans',-apple-system,sans-serif;">
                  <span style="color:#8A8A88;min-width:130px;" th:text="#{label.expiresOn}">Expires on</span>
                  <span style="color:#1A1A1A;font-weight:500;" th:text="${expiresAt}">Date</span>
                </div>
              </div>
              <div style="margin:28px 0;">
                <a th:href="${loginUrl}"
                   style="display:inline-block;background:#0A0A0A;color:#CAFF4D;padding:13px 28px;text-decoration:none;border-radius:8px;font-size:14px;font-weight:700;font-family:'DM Sans',-apple-system,sans-serif;letter-spacing:0.01em;"
                   th:text="#{membershipExpiryWarning.cta}">Renew Now</a>
              </div>
            </td>
          </tr>
          <tr>
            <td style="border-top:1px solid #F0F0EE;padding:20px 40px 28px;">
              <p style="margin:0 0 4px;font-family:'DM Sans',-apple-system,sans-serif;font-size:12px;color:#AAAAAA;line-height:1.6;"
                 th:text="#{layout.footer(${tenantName})}">Sent by Academy via Klasio.</p>
              <p style="margin:0;font-family:'DM Mono',monospace;font-size:11px;color:#C8C8C6;">© 2026 Klasio · Todos los derechos reservados</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

- [ ] **Step 4: Run test — verify it PASSES**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest" -q
```

Expected: `BUILD SUCCESS` — all 11 tests green.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/email-templates/membership-expiry-warning.html \
        api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java
git commit -m "feat(email): redesign membership-expiry-warning template"
```

---

## Task 9: membership-depleted.html

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`
- Modify: `api/src/main/resources/email-templates/membership-depleted.html`

- [ ] **Step 1: Add failing test**

```java
@Test
void membershipDepleted_rendersCorrectly() {
    RenderedTemplate result = renderer.render("membership-depleted", Locale.ENGLISH, Map.of(
            "studentName", "Pedro Sánchez",
            "programName", "Karate Kids",
            "tenantName", "Test League",
            "tenantSlug", "test-league",
            "loginUrl", "http://localhost:3000"));

    assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
    assertThat(result.htmlBody()).contains("DM Sans");
    assertThat(result.htmlBody()).contains("Pedro Sánchez");
    assertThat(result.htmlBody()).contains("Karate Kids");
    assertThat(result.htmlBody()).contains("http://localhost:3000");
}
```

- [ ] **Step 2: Run test — verify it FAILS**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest#membershipDepleted_rendersCorrectly" -q
```

Expected: FAIL.

- [ ] **Step 3: Rewrite membership-depleted.html**

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <th:block th:fragment="subject" th:text="#{membershipDepleted.subject}"></th:block>
  <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
</head>
<body style="margin:0;padding:0;background:#F4F4F2;font-family:'DM Sans',-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#F4F4F2;padding:40px 20px;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 2px 24px rgba(0,0,0,0.08);">
          <tr>
            <td style="background:#0A0A0A;padding:28px 40px;">
              <table role="presentation" cellpadding="0" cellspacing="0" style="display:inline-table;">
                <tr>
                  <td style="vertical-align:middle;">
                    <div style="width:32px;height:32px;background:#CAFF4D;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;">
                      <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" fill="#0A0A0A"/></svg>
                    </div>
                  </td>
                  <td style="vertical-align:middle;padding-left:10px;">
                    <span style="color:#FFFFFF;font-family:'DM Sans',-apple-system,sans-serif;font-size:18px;font-weight:700;letter-spacing:-0.02em;">Klasio</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="height:3px;background:linear-gradient(90deg,#CAFF4D 0%,rgba(202,255,77,0.5) 60%,transparent 100%);font-size:0;line-height:0;">&nbsp;</td>
          </tr>
          <tr>
            <td style="padding:32px 40px;">
              <p style="margin:0 0 12px;font-size:16px;color:#1A1A1A;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;">
                <span th:text="#{membershipDepleted.hi}">Hi</span>,
                <strong style="color:#0A0A0A;" th:text="${studentName}">Student</strong>
              </p>
              <p style="margin:0 0 20px;font-size:15px;color:#3A3A38;line-height:1.7;font-family:'DM Sans',-apple-system,sans-serif;"
                 th:text="#{membershipDepleted.body(${programName})}">Your membership hours are depleted.</p>
              <div style="margin:28px 0;">
                <a th:href="${loginUrl}"
                   style="display:inline-block;background:#0A0A0A;color:#CAFF4D;padding:13px 28px;text-decoration:none;border-radius:8px;font-size:14px;font-weight:700;font-family:'DM Sans',-apple-system,sans-serif;letter-spacing:0.01em;"
                   th:text="#{membershipDepleted.cta}">Contact your academy</a>
              </div>
            </td>
          </tr>
          <tr>
            <td style="border-top:1px solid #F0F0EE;padding:20px 40px 28px;">
              <p style="margin:0 0 4px;font-family:'DM Sans',-apple-system,sans-serif;font-size:12px;color:#AAAAAA;line-height:1.6;"
                 th:text="#{layout.footer(${tenantName})}">Sent by Academy via Klasio.</p>
              <p style="margin:0;font-family:'DM Mono',monospace;font-size:11px;color:#C8C8C6;">© 2026 Klasio · Todos los derechos reservados</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

- [ ] **Step 4: Run test — verify it PASSES**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest" -q
```

Expected: `BUILD SUCCESS` — all 12 tests green.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/email-templates/membership-depleted.html \
        api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java
git commit -m "feat(email): redesign membership-depleted template"
```

---

## Task 10: class-session-change.html

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`
- Modify: `api/src/main/resources/email-templates/class-session-change.html`

- [ ] **Step 1: Add two failing tests (with and without reason)**

```java
@Test
void classSessionChange_withReason_rendersBothPanelRows() {
    RenderedTemplate result = renderer.render("class-session-change", Locale.ENGLISH, Map.of(
            "studentName", "Sebastián Vargas",
            "className", "Jiu-Jitsu Advanced",
            "startsAt", "Monday April 28 at 7:00 PM",
            "changeKind", "CANCELLED",
            "reason", "Professor is ill.",
            "tenantName", "Test League",
            "tenantSlug", "test-league",
            "loginUrl", "http://localhost:3000"));

    assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
    assertThat(result.htmlBody()).contains("DM Sans");
    assertThat(result.htmlBody()).contains("CANCELLED");
    assertThat(result.htmlBody()).contains("Professor is ill.");
    assertThat(result.htmlBody()).contains("Change type");
    assertThat(result.htmlBody()).contains("Reason");
}

@Test
void classSessionChange_withoutReason_omitsReasonRow() {
    RenderedTemplate result = renderer.render("class-session-change", Locale.ENGLISH, Map.of(
            "studentName", "Sebastián Vargas",
            "className", "Jiu-Jitsu Advanced",
            "startsAt", "Monday April 28 at 7:00 PM",
            "changeKind", "ALERTED",
            "reason", "",
            "tenantName", "Test League",
            "tenantSlug", "test-league",
            "loginUrl", "http://localhost:3000"));

    assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
    assertThat(result.htmlBody()).contains("ALERTED");
    assertThat(result.htmlBody()).doesNotContain("Reason");
}
```

- [ ] **Step 2: Run tests — verify they FAIL**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest#classSessionChange_withReason_rendersBothPanelRows+classSessionChange_withoutReason_omitsReasonRow" -q
```

Expected: FAIL.

- [ ] **Step 3: Rewrite class-session-change.html**

The `changeKind` row uses two mutually exclusive `div` elements — with `border-bottom` when reason follows, without when reason is absent:

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <th:block th:fragment="subject" th:text="#{classSessionChange.subject(${className})}"></th:block>
  <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
</head>
<body style="margin:0;padding:0;background:#F4F4F2;font-family:'DM Sans',-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#F4F4F2;padding:40px 20px;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 2px 24px rgba(0,0,0,0.08);">
          <tr>
            <td style="background:#0A0A0A;padding:28px 40px;">
              <table role="presentation" cellpadding="0" cellspacing="0" style="display:inline-table;">
                <tr>
                  <td style="vertical-align:middle;">
                    <div style="width:32px;height:32px;background:#CAFF4D;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;">
                      <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" fill="#0A0A0A"/></svg>
                    </div>
                  </td>
                  <td style="vertical-align:middle;padding-left:10px;">
                    <span style="color:#FFFFFF;font-family:'DM Sans',-apple-system,sans-serif;font-size:18px;font-weight:700;letter-spacing:-0.02em;">Klasio</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="height:3px;background:linear-gradient(90deg,#CAFF4D 0%,rgba(202,255,77,0.5) 60%,transparent 100%);font-size:0;line-height:0;">&nbsp;</td>
          </tr>
          <tr>
            <td style="padding:32px 40px;">
              <p style="margin:0 0 12px;font-size:16px;color:#1A1A1A;line-height:1.6;font-family:'DM Sans',-apple-system,sans-serif;">
                <span th:text="#{classSessionChange.hi}">Hi</span>,
                <strong style="color:#0A0A0A;" th:text="${studentName}">Student</strong>
              </p>
              <p style="margin:0 0 20px;font-size:15px;color:#3A3A38;line-height:1.7;font-family:'DM Sans',-apple-system,sans-serif;"
                 th:text="#{classSessionChange.body(${className}, ${startsAt})}">There is an update for your class.</p>
              <div style="background:#F8F5FF;border:1px solid #DBC8FF;border-radius:10px;padding:4px 0;margin-bottom:20px;">
                <div th:if="${reason == null or reason.isEmpty()}"
                     style="display:flex;gap:8px;padding:10px 18px;font-size:14px;font-family:'DM Sans',-apple-system,sans-serif;">
                  <span style="color:#8A8A88;min-width:130px;" th:text="#{label.changeType}">Change type</span>
                  <span style="color:#1A1A1A;font-weight:500;" th:text="${changeKind}">CANCELLED</span>
                </div>
                <div th:if="${reason != null and !reason.isEmpty()}"
                     style="display:flex;gap:8px;padding:10px 18px;border-bottom:1px solid #F0F0EE;font-size:14px;font-family:'DM Sans',-apple-system,sans-serif;">
                  <span style="color:#8A8A88;min-width:130px;" th:text="#{label.changeType}">Change type</span>
                  <span style="color:#1A1A1A;font-weight:500;" th:text="${changeKind}">CANCELLED</span>
                </div>
                <div th:if="${reason != null and !reason.isEmpty()}"
                     style="display:flex;gap:8px;padding:10px 18px;font-size:14px;font-family:'DM Sans',-apple-system,sans-serif;">
                  <span style="color:#8A8A88;min-width:130px;" th:text="#{label.reason}">Reason</span>
                  <span style="color:#1A1A1A;font-weight:500;" th:text="${reason}">Reason text</span>
                </div>
              </div>
              <div style="margin:28px 0;">
                <a th:href="${loginUrl}"
                   style="display:inline-block;background:#0A0A0A;color:#CAFF4D;padding:13px 28px;text-decoration:none;border-radius:8px;font-size:14px;font-weight:700;font-family:'DM Sans',-apple-system,sans-serif;letter-spacing:0.01em;"
                   th:text="#{classSessionChange.cta}">View My Registrations</a>
              </div>
            </td>
          </tr>
          <tr>
            <td style="border-top:1px solid #F0F0EE;padding:20px 40px 28px;">
              <p style="margin:0 0 4px;font-family:'DM Sans',-apple-system,sans-serif;font-size:12px;color:#AAAAAA;line-height:1.6;"
                 th:text="#{layout.footer(${tenantName})}">Sent by Academy via Klasio.</p>
              <p style="margin:0;font-family:'DM Mono',monospace;font-size:11px;color:#C8C8C6;">© 2026 Klasio · Todos los derechos reservados</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

- [ ] **Step 4: Run test — verify it PASSES**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest" -q
```

Expected: `BUILD SUCCESS` — all 14 tests green.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/email-templates/class-session-change.html \
        api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java
git commit -m "feat(email): redesign class-session-change template"
```

---

## Task 11: missing-template-fallback.html

**Files:**
- Modify: `api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java`
- Modify: `api/src/main/resources/email-templates/missing-template-fallback.html`

- [ ] **Step 1: Add failing test**

```java
@Test
void missingTemplateFallback_showsTypeNameForDebugging() {
    RenderedTemplate result = renderer.render("missing-template-fallback", Locale.ENGLISH, Map.of(
            "tenantName", "Test League",
            "emailTypeName", "SOME_UNREGISTERED_TYPE",
            "tenantSlug", "test-league",
            "loginUrl", "http://localhost:3000"));

    assertThat(result.htmlBody()).doesNotContain("${").doesNotContain("#{");
    assertThat(result.htmlBody()).contains("DM Sans");
    assertThat(result.htmlBody()).contains("Test League");
    assertThat(result.htmlBody()).contains("SOME_UNREGISTERED_TYPE");
}
```

- [ ] **Step 2: Run test — verify it FAILS**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest#missingTemplateFallback_showsTypeNameForDebugging" -q
```

Expected: FAIL.

- [ ] **Step 3: Rewrite missing-template-fallback.html**

Note: subject fragment uses literal expression `|Notification from ${tenantName}|` — preserve exactly.

```html
<!DOCTYPE html>
<html xmlns:th="http://www.thymeleaf.org" lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <th:block th:fragment="subject" th:text="|Notification from ${tenantName}|"></th:block>
  <link href="https://fonts.googleapis.com/css2?family=DM+Sans:ital,opsz,wght@0,9..40,400;0,9..40,500;0,9..40,600;0,9..40,700&family=DM+Mono:wght@400;500&display=swap" rel="stylesheet">
</head>
<body style="margin:0;padding:0;background:#F4F4F2;font-family:'DM Sans',-apple-system,BlinkMacSystemFont,'Segoe UI',Arial,sans-serif;-webkit-font-smoothing:antialiased;">
  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#F4F4F2;padding:40px 20px;">
    <tr>
      <td align="center">
        <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="max-width:600px;width:100%;background:#FFFFFF;border-radius:16px;overflow:hidden;box-shadow:0 2px 24px rgba(0,0,0,0.08);">
          <tr>
            <td style="background:#0A0A0A;padding:28px 40px;">
              <table role="presentation" cellpadding="0" cellspacing="0" style="display:inline-table;">
                <tr>
                  <td style="vertical-align:middle;">
                    <div style="width:32px;height:32px;background:#CAFF4D;border-radius:8px;display:inline-flex;align-items:center;justify-content:center;">
                      <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg"><path d="M9 2L15.5 6V12L9 16L2.5 12V6L9 2Z" fill="#0A0A0A"/></svg>
                    </div>
                  </td>
                  <td style="vertical-align:middle;padding-left:10px;">
                    <span style="color:#FFFFFF;font-family:'DM Sans',-apple-system,sans-serif;font-size:18px;font-weight:700;letter-spacing:-0.02em;">Klasio</span>
                  </td>
                </tr>
              </table>
            </td>
          </tr>
          <tr>
            <td style="height:3px;background:linear-gradient(90deg,#CAFF4D 0%,rgba(202,255,77,0.5) 60%,transparent 100%);font-size:0;line-height:0;">&nbsp;</td>
          </tr>
          <tr>
            <td style="padding:32px 40px;">
              <p style="margin:0 0 12px;font-size:15px;color:#3A3A38;line-height:1.7;font-family:'DM Sans',-apple-system,sans-serif;">
                You have a new notification from <strong style="color:#0A0A0A;" th:text="${tenantName}">Academy</strong>.
              </p>
              <p style="margin:0;font-family:'DM Mono',monospace;font-size:12px;color:#8A8A88;line-height:1.6;">
                (Template not configured: <span th:text="${emailTypeName}"></span>)
              </p>
            </td>
          </tr>
          <tr>
            <td style="border-top:1px solid #F0F0EE;padding:20px 40px 28px;">
              <p style="margin:0 0 4px;font-family:'DM Sans',-apple-system,sans-serif;font-size:12px;color:#AAAAAA;line-height:1.6;"
                 th:text="#{layout.footer(${tenantName})}">Sent by Academy via Klasio.</p>
              <p style="margin:0;font-family:'DM Mono',monospace;font-size:11px;color:#C8C8C6;">© 2026 Klasio · Todos los derechos reservados</p>
            </td>
          </tr>
        </table>
      </td>
    </tr>
  </table>
</body>
</html>
```

- [ ] **Step 4: Run test — verify it PASSES**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest" -q
```

Expected: `BUILD SUCCESS` — all 15 tests green.

- [ ] **Step 5: Commit**

```bash
git add api/src/main/resources/email-templates/missing-template-fallback.html \
        api/src/test/java/com/klasio/email/infrastructure/template/ThymeleafTemplateRendererTest.java
git commit -m "feat(email): redesign missing-template-fallback template"
```

---

## Task 12: Final verification

- [ ] **Step 1: Run full email test suite**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest,EmailTypeTest,EmailDispatcherServiceTest" -q
```

Expected: `BUILD SUCCESS` — all tests green. Count should be 15 renderer tests + existing EmailType and dispatcher tests.

- [ ] **Step 2: Spanish locale smoke test — run the full renderer test with Spanish assertions**

```bash
cd api && mvn test -Dtest="ThymeleafTemplateRendererTest#passwordRecovery_rendersInSpanish" -q
```

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Verify no .txt files were touched**

```bash
git diff --name-only HEAD~11 HEAD | grep "\.txt$"
```

Expected: no output (empty — no .txt files changed).

- [ ] **Step 4: Verify no Java source files were touched**

```bash
git diff --name-only HEAD~11 HEAD | grep "src/main/java"
```

Expected: no output (empty — no production Java files changed).
