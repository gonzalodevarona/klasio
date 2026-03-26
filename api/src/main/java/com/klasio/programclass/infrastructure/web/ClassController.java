package com.klasio.programclass.infrastructure.web;

import com.klasio.programclass.application.dto.AssignProfessorCommand;
import com.klasio.programclass.application.dto.ClassDetail;
import com.klasio.programclass.application.dto.ClassSummary;
import com.klasio.programclass.application.dto.CreateClassCommand;
import com.klasio.programclass.application.dto.UpdateClassCommand;
import com.klasio.programclass.application.port.input.AssignProfessorUseCase;
import com.klasio.programclass.application.port.input.CreateClassUseCase;
import com.klasio.programclass.application.port.input.DeactivateClassUseCase;
import com.klasio.programclass.application.port.input.GetClassDetailUseCase;
import com.klasio.programclass.application.port.input.ListClassesUseCase;
import com.klasio.programclass.application.port.input.ReactivateClassUseCase;
import com.klasio.programclass.application.port.input.RemoveProfessorUseCase;
import com.klasio.programclass.application.port.input.UpdateClassUseCase;
import com.klasio.programclass.domain.model.ClassLevel;
import com.klasio.programclass.domain.model.ClassScheduleEntry;
import com.klasio.programclass.domain.model.ClassStatus;
import com.klasio.programclass.domain.model.ClassType;
import com.klasio.programclass.domain.model.ProgramClass;
import com.klasio.shared.infrastructure.persistence.TenantContextInterceptor;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/programs/{programId}/classes")
public class ClassController {

    private final CreateClassUseCase createClassUseCase;
    private final ListClassesUseCase listClassesUseCase;
    private final GetClassDetailUseCase getClassDetailUseCase;
    private final UpdateClassUseCase updateClassUseCase;
    private final DeactivateClassUseCase deactivateClassUseCase;
    private final ReactivateClassUseCase reactivateClassUseCase;
    private final AssignProfessorUseCase assignProfessorUseCase;
    private final RemoveProfessorUseCase removeProfessorUseCase;

    public ClassController(CreateClassUseCase createClassUseCase,
                           ListClassesUseCase listClassesUseCase,
                           GetClassDetailUseCase getClassDetailUseCase,
                           UpdateClassUseCase updateClassUseCase,
                           DeactivateClassUseCase deactivateClassUseCase,
                           ReactivateClassUseCase reactivateClassUseCase,
                           AssignProfessorUseCase assignProfessorUseCase,
                           RemoveProfessorUseCase removeProfessorUseCase) {
        this.createClassUseCase = createClassUseCase;
        this.listClassesUseCase = listClassesUseCase;
        this.getClassDetailUseCase = getClassDetailUseCase;
        this.updateClassUseCase = updateClassUseCase;
        this.deactivateClassUseCase = deactivateClassUseCase;
        this.reactivateClassUseCase = reactivateClassUseCase;
        this.assignProfessorUseCase = assignProfessorUseCase;
        this.removeProfessorUseCase = removeProfessorUseCase;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ClassResponseDto.ClassDetailResponse> createClass(
            @PathVariable UUID programId,
            @Valid @RequestBody ClassRequestDto.CreateClassRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        List<ClassScheduleEntry> scheduleEntries = toScheduleEntries(request.scheduleEntries());

        CreateClassCommand command = new CreateClassCommand(
                tenantId, programId, request.name(),
                ClassLevel.valueOf(request.level()),
                ClassType.valueOf(request.type()),
                scheduleEntries, request.professorId(),
                request.maxStudents(), userId);

        ProgramClass programClass = createClassUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ClassResponseDto.ClassDetailResponse.fromDomain(programClass));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<Page<ClassResponseDto.ClassSummaryResponse>> listClasses(
            @PathVariable UUID programId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String status) {

        UUID tenantId = extractTenantId();
        Pageable pageable = PageRequest.of(page, size);

        ClassLevel classLevel = level != null ? ClassLevel.valueOf(level) : null;
        ClassStatus classStatus = status != null ? ClassStatus.valueOf(status) : null;

        Page<ClassSummary> summaries = listClassesUseCase.execute(tenantId, programId, classLevel, classStatus, pageable);

        return ResponseEntity.ok(summaries.map(ClassResponseDto.ClassSummaryResponse::fromSummary));
    }

    @GetMapping("/{classId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ClassResponseDto.ClassDetailResponse> getClassDetail(
            @PathVariable UUID programId,
            @PathVariable UUID classId) {

        UUID tenantId = extractTenantId();
        ClassDetail detail = getClassDetailUseCase.execute(tenantId, classId);

        return ResponseEntity.ok(ClassResponseDto.ClassDetailResponse.fromDetail(detail));
    }

    @PutMapping("/{classId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ClassResponseDto.ClassDetailResponse> updateClass(
            @PathVariable UUID programId,
            @PathVariable UUID classId,
            @Valid @RequestBody ClassRequestDto.UpdateClassRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        List<ClassScheduleEntry> scheduleEntries = toScheduleEntries(request.scheduleEntries());

        UpdateClassCommand command = new UpdateClassCommand(
                tenantId, programId, classId, request.name(),
                ClassLevel.valueOf(request.level()),
                scheduleEntries, request.maxStudents(), userId);

        ProgramClass programClass = updateClassUseCase.execute(command);

        return ResponseEntity.ok(ClassResponseDto.ClassDetailResponse.fromDomain(programClass));
    }

    @PostMapping("/{classId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ClassResponseDto.ClassDetailResponse> deactivateClass(
            @PathVariable UUID programId,
            @PathVariable UUID classId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        ProgramClass programClass = deactivateClassUseCase.execute(tenantId, classId, userId);

        return ResponseEntity.ok(ClassResponseDto.ClassDetailResponse.fromDomain(programClass));
    }

    @PostMapping("/{classId}/reactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ClassResponseDto.ClassDetailResponse> reactivateClass(
            @PathVariable UUID programId,
            @PathVariable UUID classId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        ProgramClass programClass = reactivateClassUseCase.execute(tenantId, classId, userId);

        return ResponseEntity.ok(ClassResponseDto.ClassDetailResponse.fromDomain(programClass));
    }

    @PutMapping("/{classId}/professor")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ClassResponseDto.ClassDetailResponse> assignProfessor(
            @PathVariable UUID programId,
            @PathVariable UUID classId,
            @Valid @RequestBody ClassRequestDto.AssignProfessorRequest request) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        AssignProfessorCommand command = new AssignProfessorCommand(
                tenantId, classId, request.professorId(), userId);

        ProgramClass programClass = assignProfessorUseCase.execute(command);

        return ResponseEntity.ok(ClassResponseDto.ClassDetailResponse.fromDomain(programClass));
    }

    @DeleteMapping("/{classId}/professor")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERADMIN', 'MANAGER')")
    public ResponseEntity<ClassResponseDto.ClassDetailResponse> removeProfessor(
            @PathVariable UUID programId,
            @PathVariable UUID classId) {

        UUID userId = extractUserId();
        UUID tenantId = extractTenantId();

        ProgramClass programClass = removeProfessorUseCase.execute(tenantId, classId, userId);

        return ResponseEntity.ok(ClassResponseDto.ClassDetailResponse.fromDomain(programClass));
    }

    private List<ClassScheduleEntry> toScheduleEntries(List<ClassRequestDto.ScheduleEntryRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            return Collections.emptyList();
        }
        return requests.stream()
                .map(r -> new ClassScheduleEntry(
                        r.dayOfWeek() != null ? DayOfWeek.valueOf(r.dayOfWeek()) : null,
                        r.specificDate() != null ? LocalDate.parse(r.specificDate()) : null,
                        LocalTime.parse(r.startTime()),
                        LocalTime.parse(r.endTime())))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private UUID extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        return UUID.fromString((String) details.get("userId"));
    }

    @SuppressWarnings("unchecked")
    private UUID extractTenantId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        String tenantId = (String) details.get("tenantId");

        if (tenantId == null) {
            tenantId = TenantContextInterceptor.getCurrentTenant();
        }

        if (tenantId == null) {
            throw new IllegalStateException(
                    "No tenant context available. Provide tenant_id in JWT or X-Tenant-Id header.");
        }

        return UUID.fromString(tenantId);
    }
}
