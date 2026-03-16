package com.klasio.tenant.infrastructure.web;

import com.klasio.tenant.application.dto.CreateTenantCommand;
import com.klasio.tenant.application.dto.DeactivateTenantCommand;
import com.klasio.tenant.application.dto.TenantDetail;
import com.klasio.tenant.application.dto.TenantSummary;
import com.klasio.tenant.application.port.input.CreateTenantUseCase;
import com.klasio.tenant.application.port.input.DeactivateTenantUseCase;
import com.klasio.tenant.application.port.input.GetTenantDetailUseCase;
import com.klasio.tenant.application.port.input.ListTenantsUseCase;
import com.klasio.tenant.domain.model.Tenant;
import com.klasio.tenant.domain.port.LogoStorage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final CreateTenantUseCase createTenantUseCase;
    private final ListTenantsUseCase listTenantsUseCase;
    private final GetTenantDetailUseCase getTenantDetailUseCase;
    private final DeactivateTenantUseCase deactivateTenantUseCase;
    private final LogoStorage logoStorage;

    public TenantController(CreateTenantUseCase createTenantUseCase,
                            ListTenantsUseCase listTenantsUseCase,
                            GetTenantDetailUseCase getTenantDetailUseCase,
                            DeactivateTenantUseCase deactivateTenantUseCase,
                            LogoStorage logoStorage) {
        this.createTenantUseCase = createTenantUseCase;
        this.listTenantsUseCase = listTenantsUseCase;
        this.getTenantDetailUseCase = getTenantDetailUseCase;
        this.deactivateTenantUseCase = deactivateTenantUseCase;
        this.logoStorage = logoStorage;
    }

    @GetMapping
    public ResponseEntity<Page<TenantSummary>> listTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "createdAt,desc") String sort) {

        String[] sortParts = sort.split(",");
        String sortField = sortParts[0];
        Sort.Direction direction = sortParts.length > 1 && sortParts[1].equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortField));
        Page<TenantSummary> result = listTenantsUseCase.execute(pageable, status);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{slug}")
    public ResponseEntity<TenantDetail> getTenantDetail(@PathVariable String slug) {
        TenantDetail detail = getTenantDetailUseCase.execute(slug);
        return ResponseEntity.ok(detail);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<TenantResponseDto.TenantDetailResponse> createTenant(
            @RequestParam("name") String name,
            @RequestParam("sportDiscipline") String sportDiscipline,
            @RequestParam("contactEmail") String contactEmail,
            @RequestParam(value = "slug", required = false) String slug,
            @RequestParam(value = "contactPhone", required = false) String contactPhone,
            @RequestParam(value = "contactAddress", required = false) String contactAddress,
            @RequestParam(value = "logo", required = false) MultipartFile logo) throws IOException {

        UUID userId = extractUserId();

        InputStream logoData = null;
        String logoContentType = null;
        long logoSize = 0;

        if (logo != null && !logo.isEmpty()) {
            logoData = logo.getInputStream();
            logoContentType = logo.getContentType();
            logoSize = logo.getSize();
        }

        CreateTenantCommand command = new CreateTenantCommand(
                name,
                sportDiscipline,
                slug,
                contactEmail,
                contactPhone,
                contactAddress,
                logoData,
                logoContentType,
                logoSize,
                userId
        );

        Tenant tenant = createTenantUseCase.execute(command);

        String logoUrl = null;
        if (tenant.getLogoKey() != null) {
            logoUrl = logoStorage.generatePresignedUrl(tenant.getLogoKey());
        }

        TenantResponseDto.TenantDetailResponse response =
                TenantResponseDto.TenantDetailResponse.fromDomainWithLogoUrl(tenant, logoUrl);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{slug}/deactivate")
    public ResponseEntity<TenantResponseDto.TenantDetailResponse> deactivateTenant(
            @PathVariable String slug) {

        UUID userId = extractUserId();

        DeactivateTenantCommand command = new DeactivateTenantCommand(slug, userId);
        Tenant tenant = deactivateTenantUseCase.execute(command);

        String logoUrl = null;
        if (tenant.getLogoKey() != null) {
            logoUrl = logoStorage.generatePresignedUrl(tenant.getLogoKey());
        }

        TenantResponseDto.TenantDetailResponse response =
                TenantResponseDto.TenantDetailResponse.fromDomainWithLogoUrl(tenant, logoUrl);

        return ResponseEntity.ok(response);
    }

    @SuppressWarnings("unchecked")
    private UUID extractUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> details = (Map<String, Object>) authentication.getDetails();
        return UUID.fromString((String) details.get("userId"));
    }
}
