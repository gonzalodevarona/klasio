package com.klasio.membership.domain.port;

import java.io.InputStream;
import java.util.UUID;

public interface PaymentProofStorage {
    /**
     * Validates MIME type via Apache Tika, enforces 5 MB limit,
     * stores the file to S3 under proofs/{tenantId}/{membershipId}/{uuid}.{ext},
     * and returns the S3 object key.
     */
    String store(UUID tenantId, UUID membershipId, InputStream content,
                 String contentType, long sizeBytes);

    /**
     * Generates a presigned GET URL valid for 15 minutes.
     */
    String generateDownloadUrl(String fileKey);
}
