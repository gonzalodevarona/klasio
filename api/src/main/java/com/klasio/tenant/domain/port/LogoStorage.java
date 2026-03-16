package com.klasio.tenant.domain.port;

import java.io.InputStream;
import java.util.UUID;

public interface LogoStorage {

    String upload(UUID tenantId, InputStream data, String contentType, long size);

    void delete(String logoKey);

    String generatePresignedUrl(String logoKey);
}
