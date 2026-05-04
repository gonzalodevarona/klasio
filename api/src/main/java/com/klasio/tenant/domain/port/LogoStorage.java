package com.klasio.tenant.domain.port;

import java.io.InputStream;
import java.util.UUID;

public interface LogoStorage {

    String upload(UUID tenantId, InputStream data, String contentType, long size);

    void delete(String logoKey);

    String generatePresignedUrl(String logoKey);

    /**
     * Stable public URL for a logo object. Returns null when {@code logoKey} is null.
     * The bucket policy must grant public read on the {@code logos/*} prefix for the
     * URL to be reachable; this method does not sign the request.
     */
    String getPublicUrl(String logoKey);
}
