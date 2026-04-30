# Admin Dashboard Implementation Plan (RF-31)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement a live admin dashboard backed by a single `GET /api/v1/admin/dashboard` endpoint — four KPI stat cards and a filterable student attendance-control table, all labels via next-intl i18n.

**Architecture:** New `com.klasio.admin.dashboard` package (backend) with a thin hexagonal slice: one use case, one output port, one EntityManager-based adapter handling all cross-module native SQL. Frontend is a client component calling the backend directly via `api.get()` (no proxy route).

**Tech Stack:** Java 21 · Spring Boot 3.4 · JPA EntityManager native SQL · JUnit 5 + Mockito + MockMvc · Next.js 15 · React 19 · next-intl · TypeScript 5.9 · Tailwind CSS 3.4

---

## File Map

**New — backend:**
```
api/src/main/java/com/klasio/admin/dashboard/
  application/dto/DashboardStudentDto.java
  application/dto/AdminDashboardDto.java
  application/port/GetAdminDashboardUseCase.java
  application/port/AdminDashboardRepository.java
  application/service/GetAdminDashboardService.java
  infrastructure/persistence/AdminDashboardAdapter.java
  infrastructure/web/AdminDashboardController.java

api/src/test/java/com/klasio/admin/dashboard/
  application/service/GetAdminDashboardServiceTest.java
  infrastructure/web/AdminDashboardControllerTest.java
```

**New — frontend:**
```
web/src/hooks/useAdminDashboard.ts
```

**Modified — frontend:**
```
web/messages/en.json           (replace adminDashboard namespace)
web/messages/es.json           (replace adminDashboard namespace)
web/src/app/(dashboard)/admin/dashboard/page.tsx  (full replacement)
```

---

## Task 1: Domain contracts (DTOs + ports)

**Files:**
- Create: `api/src/main/java/com/klasio/admin/dashboard/application/dto/DashboardStudentDto.java`
- Create: `api/src/main/java/com/klasio/admin/dashboard/application/dto/AdminDashboardDto.java`
- Create: `api/src/main/java/com/klasio/admin/dashboard/application/port/GetAdminDashboardUseCase.java`
- Create: `api/src/main/java/com/klasio/admin/dashboard/application/port/AdminDashboardRepository.java`

- [ ] **Step 1: Create DashboardStudentDto**

```java
package com.klasio.admin.dashboard.application.dto;

import java.util.UUID;

public record DashboardStudentDto(
        UUID id,
        String name,
        String programName,
        String membershipStatus,
        Integer availableHours,
        Integer purchasedHours
) {}
```

- [ ] **Step 2: Create AdminDashboardDto**

```java
package com.klasio.admin.dashboard.application.dto;

import java.util.List;

public record AdminDashboardDto(
        long studentCount,
        long newStudentsThisMonth,
        long totalHoursConsumed,
        long pendingPaymentProofs,
        long activeProgramCount,
        List<DashboardStudentDto> students
) {}
```

- [ ] **Step 3: Create GetAdminDashboardUseCase**

```java
package com.klasio.admin.dashboard.application.port;

import com.klasio.admin.dashboard.application.dto.AdminDashboardDto;

import java.util.UUID;

public interface GetAdminDashboardUseCase {
    AdminDashboardDto execute(UUID tenantId);
}
```

- [ ] **Step 4: Create AdminDashboardRepository (output port)**

```java
package com.klasio.admin.dashboard.application.port;

import com.klasio.admin.dashboard.application.dto.DashboardStudentDto;

import java.util.List;
import java.util.UUID;

public interface AdminDashboardRepository {
    long countStudents(UUID tenantId);
    long countNewStudentsThisMonth(UUID tenantId);
    long sumConsumedHours(UUID tenantId);
    long countPendingProofs(UUID tenantId);
    long countActivePrograms(UUID tenantId);
    List<DashboardStudentDto> findStudentSummaries(UUID tenantId);
}
```

- [ ] **Step 5: Compile to verify no errors**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api && mvn compile -q
```

Expected: BUILD SUCCESS with no errors.

- [ ] **Step 6: Commit**

```bash
git add api/src/main/java/com/klasio/admin/dashboard/
git commit -m "feat(dashboard): add admin dashboard domain contracts"
```

---

## Task 2: Service test (TDD — write failing tests first)

**Files:**
- Create: `api/src/test/java/com/klasio/admin/dashboard/application/service/GetAdminDashboardServiceTest.java`

- [ ] **Step 1: Create the test file**

```java
package com.klasio.admin.dashboard.application.service;

import com.klasio.admin.dashboard.application.dto.AdminDashboardDto;
import com.klasio.admin.dashboard.application.dto.DashboardStudentDto;
import com.klasio.admin.dashboard.application.port.AdminDashboardRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetAdminDashboardServiceTest {

    @Mock
    private AdminDashboardRepository repository;

    private GetAdminDashboardService service;

    private static final UUID TENANT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        service = new GetAdminDashboardService(repository);
    }

    @Test
    @DisplayName("maps all KPI counts from repository")
    void mapsAllKpiFields() {
        when(repository.countStudents(TENANT_ID)).thenReturn(248L);
        when(repository.countNewStudentsThisMonth(TENANT_ID)).thenReturn(12L);
        when(repository.sumConsumedHours(TENANT_ID)).thenReturn(1840L);
        when(repository.countPendingProofs(TENANT_ID)).thenReturn(14L);
        when(repository.countActivePrograms(TENANT_ID)).thenReturn(7L);
        when(repository.findStudentSummaries(TENANT_ID)).thenReturn(List.of());

        AdminDashboardDto result = service.execute(TENANT_ID);

        assertThat(result.studentCount()).isEqualTo(248);
        assertThat(result.newStudentsThisMonth()).isEqualTo(12);
        assertThat(result.totalHoursConsumed()).isEqualTo(1840);
        assertThat(result.pendingPaymentProofs()).isEqualTo(14);
        assertThat(result.activeProgramCount()).isEqualTo(7);
        assertThat(result.students()).isEmpty();
    }

    @Test
    @DisplayName("passes tenantId to every repository call")
    void passesTenantIdToEveryCall() {
        when(repository.countStudents(TENANT_ID)).thenReturn(0L);
        when(repository.countNewStudentsThisMonth(TENANT_ID)).thenReturn(0L);
        when(repository.sumConsumedHours(TENANT_ID)).thenReturn(0L);
        when(repository.countPendingProofs(TENANT_ID)).thenReturn(0L);
        when(repository.countActivePrograms(TENANT_ID)).thenReturn(0L);
        when(repository.findStudentSummaries(TENANT_ID)).thenReturn(List.of());

        service.execute(TENANT_ID);

        verify(repository).countStudents(TENANT_ID);
        verify(repository).countNewStudentsThisMonth(TENANT_ID);
        verify(repository).sumConsumedHours(TENANT_ID);
        verify(repository).countPendingProofs(TENANT_ID);
        verify(repository).countActivePrograms(TENANT_ID);
        verify(repository).findStudentSummaries(TENANT_ID);
    }

    @Test
    @DisplayName("includes student summaries in result")
    void includesStudentSummaries() {
        DashboardStudentDto student = new DashboardStudentDto(
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "Carlos Rodriguez",
                "Swimming Advanced",
                "ACTIVE",
                4,
                24
        );
        when(repository.countStudents(TENANT_ID)).thenReturn(1L);
        when(repository.countNewStudentsThisMonth(TENANT_ID)).thenReturn(0L);
        when(repository.sumConsumedHours(TENANT_ID)).thenReturn(20L);
        when(repository.countPendingProofs(TENANT_ID)).thenReturn(0L);
        when(repository.countActivePrograms(TENANT_ID)).thenReturn(1L);
        when(repository.findStudentSummaries(TENANT_ID)).thenReturn(List.of(student));

        AdminDashboardDto result = service.execute(TENANT_ID);

        assertThat(result.students()).hasSize(1);
        assertThat(result.students().get(0).name()).isEqualTo("Carlos Rodriguez");
        assertThat(result.students().get(0).membershipStatus()).isEqualTo("ACTIVE");
        assertThat(result.students().get(0).availableHours()).isEqualTo(4);
        assertThat(result.students().get(0).purchasedHours()).isEqualTo(24);
    }
}
```

- [ ] **Step 2: Run test — verify it fails (class not found)**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api && mvn test -Dtest=GetAdminDashboardServiceTest -q 2>&1 | tail -20
```

Expected: COMPILATION ERROR — `GetAdminDashboardService` does not exist yet.

---

## Task 3: Service implementation

**Files:**
- Create: `api/src/main/java/com/klasio/admin/dashboard/application/service/GetAdminDashboardService.java`

- [ ] **Step 1: Create the service**

```java
package com.klasio.admin.dashboard.application.service;

import com.klasio.admin.dashboard.application.dto.AdminDashboardDto;
import com.klasio.admin.dashboard.application.port.AdminDashboardRepository;
import com.klasio.admin.dashboard.application.port.GetAdminDashboardUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAdminDashboardService implements GetAdminDashboardUseCase {

    private final AdminDashboardRepository dashboardRepository;

    public GetAdminDashboardService(AdminDashboardRepository dashboardRepository) {
        this.dashboardRepository = dashboardRepository;
    }

    @Override
    public AdminDashboardDto execute(UUID tenantId) {
        return new AdminDashboardDto(
                dashboardRepository.countStudents(tenantId),
                dashboardRepository.countNewStudentsThisMonth(tenantId),
                dashboardRepository.sumConsumedHours(tenantId),
                dashboardRepository.countPendingProofs(tenantId),
                dashboardRepository.countActivePrograms(tenantId),
                dashboardRepository.findStudentSummaries(tenantId)
        );
    }
}
```

- [ ] **Step 2: Run tests — verify all 3 pass**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api && mvn test -Dtest=GetAdminDashboardServiceTest -q 2>&1 | tail -10
```

Expected:
```
Tests run: 3, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/admin/dashboard/application/service/ \
        api/src/test/java/com/klasio/admin/dashboard/
git commit -m "feat(dashboard): add GetAdminDashboardService with passing tests"
```

---

## Task 4: Controller test (TDD — write failing tests first)

**Files:**
- Create: `api/src/test/java/com/klasio/admin/dashboard/infrastructure/web/AdminDashboardControllerTest.java`

- [ ] **Step 1: Create the controller test**

```java
package com.klasio.admin.dashboard.infrastructure.web;

import com.klasio.admin.dashboard.application.dto.AdminDashboardDto;
import com.klasio.admin.dashboard.application.port.GetAdminDashboardUseCase;
import com.klasio.shared.infrastructure.config.JwtProperties;
import com.klasio.shared.infrastructure.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminDashboardController.class)
@Import({GlobalExceptionHandler.class, AdminDashboardControllerTest.TestSecurityConfig.class})
class AdminDashboardControllerTest {

    @TestConfiguration
    @EnableMethodSecurity
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestSecurityConfig {
        @Bean
        public SecurityFilterChain testSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .authorizeHttpRequests(auth -> auth.anyRequest().authenticated())
                    .build();
        }

        @Bean
        public DataSource dataSource() throws Exception {
            javax.sql.DataSource ds = org.mockito.Mockito.mock(javax.sql.DataSource.class);
            java.sql.Connection conn = org.mockito.Mockito.mock(java.sql.Connection.class);
            org.mockito.Mockito.when(ds.getConnection()).thenReturn(conn);
            return ds;
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GetAdminDashboardUseCase getDashboard;

    private static final UUID TENANT_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private Authentication authAs(String role) {
        Map<String, Object> details = new HashMap<>();
        details.put("tenantId", TENANT_ID.toString());
        details.put("userId", UUID.randomUUID().toString());
        UsernamePasswordAuthenticationToken token = new UsernamePasswordAuthenticationToken(
                "user@test.com", null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
        );
        token.setDetails(details);
        return token;
    }

    @Test
    @DisplayName("returns 200 with full dashboard DTO for ADMIN")
    void returns200ForAdmin() throws Exception {
        AdminDashboardDto dto = new AdminDashboardDto(10, 2, 100, 3, 5, List.of());
        when(getDashboard.execute(any())).thenReturn(dto);

        mockMvc.perform(get("/api/v1/admin/dashboard").with(authentication(authAs("ADMIN"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentCount").value(10))
                .andExpect(jsonPath("$.newStudentsThisMonth").value(2))
                .andExpect(jsonPath("$.totalHoursConsumed").value(100))
                .andExpect(jsonPath("$.pendingPaymentProofs").value(3))
                .andExpect(jsonPath("$.activeProgramCount").value(5))
                .andExpect(jsonPath("$.students").isArray());
    }

    @Test
    @DisplayName("returns 200 for MANAGER role")
    void returns200ForManager() throws Exception {
        when(getDashboard.execute(any())).thenReturn(new AdminDashboardDto(0, 0, 0, 0, 0, List.of()));

        mockMvc.perform(get("/api/v1/admin/dashboard").with(authentication(authAs("MANAGER"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("returns 200 for SUPERADMIN role")
    void returns200ForSuperadmin() throws Exception {
        when(getDashboard.execute(any())).thenReturn(new AdminDashboardDto(0, 0, 0, 0, 0, List.of()));

        mockMvc.perform(get("/api/v1/admin/dashboard").with(authentication(authAs("SUPERADMIN"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("returns 403 for STUDENT role")
    void returns403ForStudent() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard").with(authentication(authAs("STUDENT"))))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("returns 403 for PROFESSOR role")
    void returns403ForProfessor() throws Exception {
        mockMvc.perform(get("/api/v1/admin/dashboard").with(authentication(authAs("PROFESSOR"))))
                .andExpect(status().isForbidden());
    }
}
```

- [ ] **Step 2: Run test — verify compilation error (controller missing)**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api && mvn test -Dtest=AdminDashboardControllerTest -q 2>&1 | tail -20
```

Expected: COMPILATION ERROR — `AdminDashboardController` does not exist yet.

---

## Task 5: Controller implementation

**Files:**
- Create: `api/src/main/java/com/klasio/admin/dashboard/infrastructure/web/AdminDashboardController.java`

- [ ] **Step 1: Create the controller**

```java
package com.klasio.admin.dashboard.infrastructure.web;

import com.klasio.admin.dashboard.application.dto.AdminDashboardDto;
import com.klasio.admin.dashboard.application.port.GetAdminDashboardUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminDashboardController {

    private final GetAdminDashboardUseCase getDashboard;

    public AdminDashboardController(GetAdminDashboardUseCase getDashboard) {
        this.getDashboard = getDashboard;
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<AdminDashboardDto> getDashboard() {
        return ResponseEntity.ok(getDashboard.execute(extractTenantId()));
    }

    @SuppressWarnings("unchecked")
    private UUID extractTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        String tenantId = (String) details.get("tenantId");
        return tenantId != null ? UUID.fromString(tenantId) : null;
    }
}
```

- [ ] **Step 2: Run controller tests — verify all 5 pass**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api && mvn test -Dtest=AdminDashboardControllerTest -q 2>&1 | tail -10
```

Expected:
```
Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 3: Commit**

```bash
git add api/src/main/java/com/klasio/admin/dashboard/infrastructure/web/ \
        api/src/test/java/com/klasio/admin/dashboard/infrastructure/web/
git commit -m "feat(dashboard): add AdminDashboardController with RBAC tests"
```

---

## Task 6: JPA adapter (native SQL implementation)

**Files:**
- Create: `api/src/main/java/com/klasio/admin/dashboard/infrastructure/persistence/AdminDashboardAdapter.java`

- [ ] **Step 1: Create the adapter**

```java
package com.klasio.admin.dashboard.infrastructure.persistence;

import com.klasio.admin.dashboard.application.dto.DashboardStudentDto;
import com.klasio.admin.dashboard.application.port.AdminDashboardRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class AdminDashboardAdapter implements AdminDashboardRepository {

    private final EntityManager em;

    public AdminDashboardAdapter(EntityManager em) {
        this.em = em;
    }

    @Override
    public long countStudents(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COUNT(DISTINCT u.id)
                FROM users u
                JOIN user_roles ur ON ur.user_id = u.id
                WHERE u.tenant_id = :tenantId
                  AND ur.role = 'STUDENT'
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public long countNewStudentsThisMonth(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COUNT(DISTINCT u.id)
                FROM users u
                JOIN user_roles ur ON ur.user_id = u.id
                WHERE u.tenant_id = :tenantId
                  AND ur.role = 'STUDENT'
                  AND u.created_at >= DATE_TRUNC('month', NOW() AT TIME ZONE 'UTC')
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public long sumConsumedHours(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COALESCE(SUM(m.purchased_hours - m.available_hours), 0)
                FROM memberships m
                WHERE m.tenant_id = :tenantId
                  AND m.status = 'ACTIVE'
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public long countPendingProofs(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COUNT(*)
                FROM payment_proofs pp
                WHERE pp.tenant_id = :tenantId
                  AND pp.status = 'PENDING'
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    @Override
    public long countActivePrograms(UUID tenantId) {
        Number result = (Number) em.createNativeQuery("""
                SELECT COUNT(*)
                FROM programs p
                WHERE p.tenant_id = :tenantId
                  AND p.status = 'ACTIVE'
                """)
                .setParameter("tenantId", tenantId)
                .getSingleResult();
        return result.longValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<DashboardStudentDto> findStudentSummaries(UUID tenantId) {
        List<Object[]> rows = em.createNativeQuery("""
                SELECT
                    u.id::text,
                    u.first_name || ' ' || u.last_name AS name,
                    p.name                              AS program_name,
                    m.status                            AS membership_status,
                    m.available_hours,
                    m.purchased_hours
                FROM users u
                LEFT JOIN LATERAL (
                    SELECT e.program_id
                    FROM student_enrollments e
                    WHERE e.student_id = u.id
                      AND e.tenant_id  = :tenantId
                      AND e.status     = 'ACTIVE'
                    ORDER BY e.enrollment_date DESC
                    LIMIT 1
                ) enr ON TRUE
                LEFT JOIN programs p ON p.id = enr.program_id AND p.tenant_id = :tenantId
                LEFT JOIN LATERAL (
                    SELECT m2.status, m2.available_hours, m2.purchased_hours, m2.updated_at
                    FROM memberships m2
                    WHERE m2.student_id = u.id
                      AND m2.tenant_id  = :tenantId
                      AND m2.status     = 'ACTIVE'
                    ORDER BY m2.updated_at DESC
                    LIMIT 1
                ) m ON TRUE
                WHERE u.tenant_id = :tenantId
                  AND EXISTS (
                      SELECT 1 FROM user_roles ur
                      WHERE ur.user_id = u.id AND ur.role = 'STUDENT'
                  )
                ORDER BY COALESCE(m.updated_at, u.created_at) DESC
                LIMIT 50
                """)
                .setParameter("tenantId", tenantId)
                .getResultList();

        return rows.stream().map(row -> new DashboardStudentDto(
                UUID.fromString((String) row[0]),
                (String) row[1],
                (String) row[2],
                (String) row[3],
                row[4] != null ? ((Number) row[4]).intValue() : null,
                row[5] != null ? ((Number) row[5]).intValue() : null
        )).toList();
    }
}
```

- [ ] **Step 2: Compile to verify no errors**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api && mvn compile -q 2>&1 | tail -10
```

Expected: BUILD SUCCESS

- [ ] **Step 3: Run all dashboard tests to ensure nothing broken**

```bash
cd /Users/gonzalodevarona/Documents/klasio/api && mvn test -Dtest="GetAdminDashboardServiceTest,AdminDashboardControllerTest" -q 2>&1 | tail -10
```

Expected:
```
Tests run: 8, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- [ ] **Step 4: Commit**

```bash
git add api/src/main/java/com/klasio/admin/dashboard/infrastructure/persistence/
git commit -m "feat(dashboard): add AdminDashboardAdapter with native SQL queries"
```

---

## Task 7: i18n keys

**Files:**
- Modify: `web/messages/en.json`
- Modify: `web/messages/es.json`

- [ ] **Step 1: Replace `adminDashboard` block in en.json**

Find this block (around line 1076):
```json
  "adminDashboard": {
    "title": "Admin Dashboard",
    "subtitle": "League overview",
    "statStudents": "Students",
    "statActiveMemberships": "Active memberships",
    "statPendingProofs": "Pending proofs",
    "statPrograms": "Programs"
  },
```

Replace with:
```json
  "adminDashboard": {
    "title": "Dashboard",
    "subtitle": "Current period",
    "actionValidatePayment": "Validate payment",
    "actionRegisterClass": "Register class",
    "actionViewStudents": "View students",
    "actionPrograms": "Programs",
    "kpiStudents": "Students",
    "kpiNewThisMonth": "↑ {count} new this month",
    "kpiHoursConsumed": "Hours consumed",
    "kpiHoursConsumedSub": "This period",
    "kpiPendingPayments": "Pending payments",
    "kpiPendingAction": "Action required",
    "kpiUpToDate": "Up to date",
    "kpiActivePrograms": "Active programs",
    "attendanceTitle": "Attendance control",
    "attendanceStartClass": "Start class",
    "attendanceEmpty": "No students with this status.",
    "filterAll": "All",
    "filterActive": "Active",
    "filterExpiring": "Expiring soon",
    "filterInactive": "Inactive",
    "filterExpired": "Expired",
    "filterNew": "New",
    "tableStudent": "Student",
    "tableProgram": "Program",
    "tableHours": "Hours",
    "tableStatus": "Status",
    "statusActive": "Active",
    "statusExpiring": "Expiring soon",
    "statusInactive": "Inactive",
    "statusExpired": "Expired",
    "statusNew": "New",
    "error": "Failed to load dashboard data."
  },
```

- [ ] **Step 2: Replace `adminDashboard` block in es.json**

Find this block (around line 1076):
```json
  "adminDashboard": {
    "title": "Panel de Administrador",
    "subtitle": "Resumen de la liga",
    "statStudents": "Estudiantes",
    "statActiveMemberships": "Membresías activas",
    "statPendingProofs": "Comprobantes pendientes",
    "statPrograms": "Programas"
  },
```

Replace with:
```json
  "adminDashboard": {
    "title": "Dashboard",
    "subtitle": "Período actual",
    "actionValidatePayment": "Validar pago",
    "actionRegisterClass": "Registrar clase",
    "actionViewStudents": "Ver estudiantes",
    "actionPrograms": "Programas",
    "kpiStudents": "Estudiantes",
    "kpiNewThisMonth": "↑ {count} nuevos este mes",
    "kpiHoursConsumed": "Horas consumidas",
    "kpiHoursConsumedSub": "Este período",
    "kpiPendingPayments": "Pagos pendientes",
    "kpiPendingAction": "Requieren acción",
    "kpiUpToDate": "Al día",
    "kpiActivePrograms": "Programas activos",
    "attendanceTitle": "Control de asistencia",
    "attendanceStartClass": "Iniciar clase",
    "attendanceEmpty": "No hay estudiantes con este estado.",
    "filterAll": "Todos",
    "filterActive": "Activo",
    "filterExpiring": "Por vencer",
    "filterInactive": "Inactivo",
    "filterExpired": "Vencida",
    "filterNew": "Nuevo",
    "tableStudent": "Estudiante",
    "tableProgram": "Programa",
    "tableHours": "Horas",
    "tableStatus": "Estado",
    "statusActive": "Activo",
    "statusExpiring": "Por vencer",
    "statusInactive": "Inactivo",
    "statusExpired": "Vencida",
    "statusNew": "Nuevo",
    "error": "Error al cargar el dashboard."
  },
```

- [ ] **Step 3: Verify JSON is valid**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web && node -e "require('./messages/en.json'); require('./messages/es.json'); console.log('JSON valid')"
```

Expected: `JSON valid`

- [ ] **Step 4: Commit**

```bash
git add web/messages/en.json web/messages/es.json
git commit -m "feat(dashboard): add i18n keys for admin dashboard"
```

---

## Task 8: Frontend hook

**Files:**
- Create: `web/src/hooks/useAdminDashboard.ts`

- [ ] **Step 1: Create the hook**

```typescript
"use client";

import { useCallback, useEffect, useState } from "react";
import { api } from "@/lib/api";

export interface DashboardStudent {
  id: string;
  name: string;
  programName: string | null;
  membershipStatus: string | null;
  availableHours: number | null;
  purchasedHours: number | null;
}

export interface AdminDashboardData {
  studentCount: number;
  newStudentsThisMonth: number;
  totalHoursConsumed: number;
  pendingPaymentProofs: number;
  activeProgramCount: number;
  students: DashboardStudent[];
}

export function useAdminDashboard() {
  const [data, setData] = useState<AdminDashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await api.get<AdminDashboardData>("/admin/dashboard");
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load dashboard.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return { data, loading, error, refetch: load };
}
```

- [ ] **Step 2: Type-check**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web && npx tsc --noEmit 2>&1 | head -20
```

Expected: no output (zero errors).

- [ ] **Step 3: Commit**

```bash
git add web/src/hooks/useAdminDashboard.ts
git commit -m "feat(dashboard): add useAdminDashboard hook"
```

---

## Task 9: Dashboard page (full replacement)

**Files:**
- Modify: `web/src/app/(dashboard)/admin/dashboard/page.tsx`

- [ ] **Step 1: Replace the page entirely**

```typescript
"use client";

import React, { useState } from "react";
import Link from "next/link";
import { useTranslations } from "next-intl";
import { useAdminDashboard, DashboardStudent } from "@/hooks/useAdminDashboard";

// ── HoursBar ──────────────────────────────────────────────────────────────
function HoursBar({ available, purchased }: { available: number; purchased: number }) {
  const consumed = purchased - available;
  const pct = purchased > 0 ? Math.min(100, Math.round((consumed / purchased) * 100)) : 0;
  const barColor =
    pct <= 33 ? "#CAFF4D" : pct <= 66 ? "#8AE800" : pct <= 85 ? "#FFC107" : "#CC2200";
  return (
    <div className="flex items-center gap-2">
      <div className="w-20 h-1 rounded-full overflow-hidden" style={{ background: "#EBEBEA" }}>
        <div className="h-full rounded-full" style={{ width: `${pct}%`, background: barColor }} />
      </div>
      <span className="text-[11px]" style={{ fontFamily: "var(--font-mono)", color: "#4A4A48" }}>
        {available}/{purchased}h
      </span>
    </div>
  );
}

// ── Status badge ──────────────────────────────────────────────────────────
const STATUS_STYLE: Record<string, { bg: string; color: string; border?: string }> = {
  ACTIVE:   { bg: "#CAFF4D", color: "#2A4A00" },
  EXPIRING: { bg: "#FFF0C2", color: "#8A5A00" },
  INACTIVE: { bg: "#F4F4F2", color: "#6A6A68", border: "#DDDDD8" },
  NEW:      { bg: "#E8F4FF", color: "#0066BB" },
  EXPIRED:  { bg: "#FFE8E8", color: "#CC2200" },
};

function StatusBadge({ status, label }: { status: string | null; label: string }) {
  if (!status) return <span style={{ color: "#9A9A98", fontSize: 12 }}>—</span>;
  const s = STATUS_STYLE[status.toUpperCase()] ?? { bg: "#F4F4F2", color: "#6A6A68" };
  return (
    <span
      className="inline-flex items-center rounded-full text-[11px] font-semibold px-2.5 py-0.5 whitespace-nowrap"
      style={{
        background: s.bg,
        color: s.color,
        border: s.border ? `1px solid ${s.border}` : undefined,
      }}
    >
      {label}
    </span>
  );
}

// ── Stat card ─────────────────────────────────────────────────────────────
function StatCard({
  label,
  value,
  sub,
  subColor,
  dark,
}: {
  label: string;
  value: string | number;
  sub?: string;
  subColor?: string;
  dark?: boolean;
}) {
  return (
    <div
      className="rounded-[16px] px-7 py-6 flex flex-col gap-1.5"
      style={{
        background: dark ? "#0A0A0A" : "#FAFAF8",
        border: dark ? "none" : "1.5px solid #DDDDD8",
      }}
    >
      <span
        className="text-[10px] uppercase tracking-[0.1em]"
        style={{ fontFamily: "var(--font-mono)", color: dark ? "#666" : "#9A9A98" }}
      >
        {label}
      </span>
      <span
        className="text-[40px] font-extrabold tracking-[-0.03em] leading-none"
        style={{ color: dark ? "#CAFF4D" : "#0A0A0A" }}
      >
        {value}
      </span>
      {sub && (
        <span className="text-xs font-medium" style={{ color: subColor ?? (dark ? "#CAFF4D" : "#9A9A98") }}>
          {sub}
        </span>
      )}
    </div>
  );
}

// ── Status label lookup ───────────────────────────────────────────────────
const STATUS_KEY_MAP: Record<string, string> = {
  ACTIVE:   "statusActive",
  INACTIVE: "statusInactive",
  EXPIRED:  "statusExpired",
  EXPIRING: "statusExpiring",
  NEW:      "statusNew",
};

// ── Page ──────────────────────────────────────────────────────────────────
export default function AdminDashboard() {
  const t = useTranslations("adminDashboard");
  const { data, loading, error } = useAdminDashboard();
  const [activeFilter, setActiveFilter] = useState("all");

  const FILTERS = [
    { id: "all",      label: t("filterAll"),      bg: "#0A0A0A", color: "#FAFAF8" },
    { id: "ACTIVE",   label: t("filterActive"),   bg: "#CAFF4D", color: "#2A4A00" },
    { id: "EXPIRING", label: t("filterExpiring"), bg: "#FFF0C2", color: "#8A5A00" },
    { id: "INACTIVE", label: t("filterInactive"), bg: "#F4F4F2", color: "#6A6A68" },
    { id: "EXPIRED",  label: t("filterExpired"),  bg: "#FFE8E8", color: "#CC2200" },
    { id: "NEW",      label: t("filterNew"),      bg: "#E8F4FF", color: "#0066BB" },
  ];

  const filteredStudents: DashboardStudent[] = !data
    ? []
    : activeFilter === "all"
    ? data.students
    : data.students.filter((s) => s.membershipStatus?.toUpperCase() === activeFilter);

  return (
    <div>
      {/* Header */}
      <div className="mb-7">
        <h1 className="text-[26px] font-extrabold tracking-[-0.02em]" style={{ color: "#0A0A0A" }}>
          {t("title")}
        </h1>
        <p className="text-xs mt-1" style={{ fontFamily: "var(--font-mono)", color: "#9A9A98" }}>
          {t("subtitle")}
        </p>
      </div>

      {/* Quick actions */}
      <div className="flex gap-2.5 flex-wrap mb-7">
        <Link
          href="/payment-proofs"
          className="inline-flex items-center rounded-[8px] px-4 py-2 text-sm font-semibold"
          style={{ background: "#0A0A0A", color: "#FAFAF8" }}
        >
          {t("actionValidatePayment")}
        </Link>
        <Link
          href="/classes"
          className="inline-flex items-center rounded-[8px] px-4 py-2 text-sm font-semibold"
          style={{ background: "#CAFF4D", color: "#0A0A0A" }}
        >
          {t("actionRegisterClass")}
        </Link>
        <Link
          href="/students"
          className="inline-flex items-center rounded-[8px] px-4 py-2 text-sm font-semibold border"
          style={{ background: "transparent", color: "#0A0A0A", borderColor: "#DDDDD8" }}
        >
          {t("actionViewStudents")}
        </Link>
        <Link
          href="/programs"
          className="inline-flex items-center rounded-[8px] px-4 py-2 text-sm font-semibold"
          style={{ background: "#F4F4F2", color: "#4A4A48" }}
        >
          {t("actionPrograms")}
        </Link>
      </div>

      {/* KPI cards */}
      {loading ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          {[...Array(4)].map((_, i) => (
            <div
              key={i}
              className="rounded-[16px] h-32 animate-pulse"
              style={{ background: i === 1 ? "#1A1A1A" : "#EBEBEA" }}
            />
          ))}
        </div>
      ) : error ? (
        <div
          className="rounded-[8px] p-4 text-sm mb-8"
          style={{ background: "#FFE8E8", color: "#CC2200", border: "1px solid #FFCCCC" }}
        >
          {t("error")}
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 mb-8">
          <StatCard
            label={t("kpiStudents")}
            value={data?.studentCount ?? 0}
            sub={
              (data?.newStudentsThisMonth ?? 0) > 0
                ? t("kpiNewThisMonth", { count: data!.newStudentsThisMonth })
                : undefined
            }
            subColor="#2A8A00"
          />
          <StatCard
            label={t("kpiHoursConsumed")}
            value={(data?.totalHoursConsumed ?? 0).toLocaleString()}
            sub={t("kpiHoursConsumedSub")}
            dark
          />
          <StatCard
            label={t("kpiPendingPayments")}
            value={data?.pendingPaymentProofs ?? 0}
            sub={
              (data?.pendingPaymentProofs ?? 0) > 0 ? t("kpiPendingAction") : t("kpiUpToDate")
            }
            subColor={(data?.pendingPaymentProofs ?? 0) > 0 ? "#C87000" : undefined}
          />
          <StatCard
            label={t("kpiActivePrograms")}
            value={data?.activeProgramCount ?? 0}
          />
        </div>
      )}

      {/* Attendance control table */}
      <div
        className="rounded-[16px] p-6"
        style={{ background: "#FAFAF8", border: "1.5px solid #DDDDD8" }}
      >
        <div className="flex items-center justify-between mb-5">
          <h2
            className="text-[15px] font-bold tracking-[-0.01em]"
            style={{ color: "#0A0A0A" }}
          >
            {t("attendanceTitle")}
          </h2>
          <Link
            href="/classes"
            className="inline-flex items-center rounded-[8px] px-3 py-1.5 text-xs font-semibold"
            style={{ background: "#CAFF4D", color: "#0A0A0A" }}
          >
            {t("attendanceStartClass")}
          </Link>
        </div>

        {/* Filter pills */}
        <div className="flex gap-2 flex-wrap mb-5">
          {FILTERS.map((f) => {
            const isActive = activeFilter === f.id;
            return (
              <button
                key={f.id}
                onClick={() => setActiveFilter(f.id)}
                className="text-xs font-semibold px-3.5 py-1.5 rounded-full border-none cursor-pointer"
                style={{
                  background: isActive ? f.bg : "#F4F4F2",
                  color: isActive ? f.color : "#4A4A48",
                }}
              >
                {f.label}
              </button>
            );
          })}
        </div>

        {/* Table */}
        {loading ? (
          <div className="space-y-2">
            {[...Array(5)].map((_, i) => (
              <div key={i} className="h-10 rounded animate-pulse" style={{ background: "#EBEBEA" }} />
            ))}
          </div>
        ) : filteredStudents.length === 0 ? (
          <p className="text-sm py-6 text-center" style={{ color: "#9A9A98" }}>
            {t("attendanceEmpty")}
          </p>
        ) : (
          <div className="overflow-x-auto rounded-[12px]" style={{ border: "1.5px solid #DDDDD8" }}>
            <table className="w-full border-collapse">
              <thead style={{ background: "#F4F4F2", borderBottom: "1.5px solid #DDDDD8" }}>
                <tr>
                  {(
                    [
                      t("tableStudent"),
                      t("tableProgram"),
                      t("tableHours"),
                      t("tableStatus"),
                    ] as string[]
                  ).map((h) => (
                    <th
                      key={h}
                      className="text-left px-4 py-2.5 whitespace-nowrap"
                      style={{
                        fontFamily: "var(--font-mono)",
                        fontSize: 10,
                        letterSpacing: "0.1em",
                        textTransform: "uppercase",
                        color: "#9A9A98",
                        fontWeight: 500,
                      }}
                    >
                      {h}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {filteredStudents.map((s, i) => {
                  const statusKey = s.membershipStatus
                    ? STATUS_KEY_MAP[s.membershipStatus.toUpperCase()]
                    : undefined;
                  const statusLabel = statusKey ? t(statusKey as Parameters<typeof t>[0]) : "—";
                  return (
                    <tr
                      key={s.id}
                      style={{
                        borderBottom:
                          i < filteredStudents.length - 1 ? "1px solid #EBEBEA" : "none",
                        background: "white",
                      }}
                      className="hover:bg-[#FAFAF8] transition-colors"
                    >
                      <td
                        className="px-4 py-3 text-sm font-semibold whitespace-nowrap"
                        style={{ color: "#0A0A0A" }}
                      >
                        {s.name}
                      </td>
                      <td
                        className="px-4 py-3 text-sm whitespace-nowrap"
                        style={{ color: "#9A9A98" }}
                      >
                        {s.programName ?? "—"}
                      </td>
                      <td className="px-4 py-3">
                        {s.availableHours != null && s.purchasedHours != null ? (
                          <HoursBar available={s.availableHours} purchased={s.purchasedHours} />
                        ) : (
                          <span style={{ color: "#9A9A98", fontSize: 12 }}>—</span>
                        )}
                      </td>
                      <td className="px-4 py-3">
                        <StatusBadge status={s.membershipStatus} label={statusLabel} />
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}
```

- [ ] **Step 2: Type-check**

```bash
cd /Users/gonzalodevarona/Documents/klasio/web && npx tsc --noEmit 2>&1 | head -30
```

Expected: no output (zero errors).

- [ ] **Step 3: Commit**

```bash
git add web/src/app/(dashboard)/admin/dashboard/page.tsx \
        web/src/hooks/useAdminDashboard.ts
git commit -m "feat(dashboard): implement admin dashboard page with live data and i18n"
```

---

## Verification

After implementing all tasks, verify manually with the backend running:

1. **ADMIN login** → `/admin/dashboard` → all 4 KPI cards show real numbers (not `—`)
2. **Filter pills** → click "Active" → table shows only ACTIVE students; click "All" → all students
3. **HoursBar colors** → a student with low consumption shows volt (`#CAFF4D`); high consumption shows red
4. **"Validate payment"** → navigates to `/payment-proofs`
5. **"Start class"** → navigates to `/classes`
6. **Kill the backend** → page shows error banner, does not crash or show blank screen
7. **STUDENT login** → `GET http://localhost:8080/api/v1/admin/dashboard` → 403 Forbidden
