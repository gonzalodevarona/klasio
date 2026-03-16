package com.klasio.tenant.infrastructure.storage;

import com.klasio.shared.infrastructure.config.S3Properties;
import com.klasio.shared.infrastructure.exception.InvalidFileTypeException;
import com.klasio.tenant.domain.port.LogoStorage;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
public class S3LogoStorage implements LogoStorage {

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of("image/jpeg", "image/png");
    private static final Duration PRESIGNED_URL_TTL = Duration.ofHours(1);
    private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png"
    );

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final Tika tika;

    public S3LogoStorage(S3Client s3Client,
                         S3Presigner s3Presigner,
                         S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = s3Properties.bucket();
        this.tika = new Tika();
    }

    @Override
    public String upload(UUID tenantId, InputStream data, String contentType, long size) {
        byte[] bytes;
        try {
            bytes = data.readAllBytes();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read upload data", ex);
        }

        String detectedMimeType = detectMimeType(bytes, contentType);

        if (!ALLOWED_MIME_TYPES.contains(detectedMimeType)) {
            throw new InvalidFileTypeException(
                    "Invalid file type '%s'. Allowed types: %s".formatted(detectedMimeType, ALLOWED_MIME_TYPES)
            );
        }

        String extension = MIME_TO_EXTENSION.get(detectedMimeType);
        String key = "logos/%s/%s.%s".formatted(tenantId, UUID.randomUUID(), extension);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(detectedMimeType)
                .contentLength((long) bytes.length)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));

        return key;
    }

    @Override
    public void delete(String logoKey) {
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(logoKey)
                .build();

        s3Client.deleteObject(deleteRequest);
    }

    @Override
    public String generatePresignedUrl(String logoKey) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(logoKey)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRESIGNED_URL_TTL)
                .getObjectRequest(getRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String detectMimeType(byte[] data, String contentType) {
        String detected = tika.detect(data);
        if (detected != null && !detected.equals("application/octet-stream")) {
            return detected;
        }
        return contentType;
    }
}
