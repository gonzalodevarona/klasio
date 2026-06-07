package com.klasio.tenant.application.dto;

import java.io.InputStream;
import java.util.UUID;

public record CreateTenantCommand(
        String name,
        String discipline,
        String language,
        String timezone,
        String slug,
        String contactEmail,
        String contactPhone,
        String contactPhoneIndicator,
        String contactStreet,
        String contactCity,
        String contactState,
        String contactCountry,
        InputStream logoData,
        String logoContentType,
        long logoSize,
        boolean selfRegistrationEnabled,
        UUID createdBy
) {
}
