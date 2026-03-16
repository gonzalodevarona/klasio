package com.klasio.tenant.application.dto;

import java.io.InputStream;
import java.util.UUID;

public record CreateTenantCommand(
        String name,
        String sportDiscipline,
        String slug,
        String contactEmail,
        String contactPhone,
        String contactAddress,
        InputStream logoData,
        String logoContentType,
        long logoSize,
        UUID createdBy
) {
}
