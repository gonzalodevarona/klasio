package com.klasio.membership.infrastructure.storage;

import com.klasio.membership.domain.port.PaymentProofStorage;
import com.klasio.shared.infrastructure.config.S3Properties;
import com.klasio.shared.infrastructure.exception.InvalidFileTypeException;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
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
public class S3PaymentProofStorage implements PaymentProofStorage {

    private static final long MAX_FILE_SIZE_BYTES = 5_242_880L;
    private static final Set<String> ALLOWED_MIME_TYPES =
            Set.of("application/pdf", "image/jpeg", "image/png");
    private static final Map<String, String> MIME_TO_EXTENSION = Map.of(
            "application/pdf", "pdf",
            "image/jpeg", "jpg",
            "image/png", "png"
    );
    private static final Duration PRESIGNED_URL_TTL = Duration.ofMinutes(15);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final Tika tika;

    public S3PaymentProofStorage(S3Client s3Client,
                                  S3Presigner s3Presigner,
                                  S3Properties s3Properties) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucket = s3Properties.bucket();
        this.tika = new Tika();
    }

    @Override
    public String store(UUID tenantId, UUID membershipId, InputStream content,
                        String contentType, long sizeBytes) {
        byte[] bytes;
        try {
            bytes = content.readAllBytes();
        } catch (IOException ex) {
            throw new RuntimeException("Failed to read upload data", ex);
        }

        if (bytes.length > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "File size %d exceeds maximum allowed %d bytes".formatted(bytes.length, MAX_FILE_SIZE_BYTES));
        }

        String detectedMime = detectMimeType(bytes, contentType);
        if (!ALLOWED_MIME_TYPES.contains(detectedMime)) {
            throw new InvalidFileTypeException(
                    "Invalid file type '%s'. Allowed types: %s".formatted(detectedMime, ALLOWED_MIME_TYPES));
        }

        String extension = MIME_TO_EXTENSION.get(detectedMime);
        String key = "proofs/%s/%s/%s.%s".formatted(tenantId, membershipId, UUID.randomUUID(), extension);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(detectedMime)
                .contentLength((long) bytes.length)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));
        return key;
    }

    @Override
    public String generateDownloadUrl(String fileKey) {
        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileKey)
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
