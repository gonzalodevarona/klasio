package com.klasio.membership.application.port.input;

import java.io.InputStream;
import java.util.UUID;

public record UploadPaymentProofCommand(
        UUID tenantId,
        UUID membershipId,
        UUID studentId,
        InputStream fileContent,
        String originalFileName,
        String contentType,
        long fileSizeBytes,
        UUID uploadedBy
) {}
