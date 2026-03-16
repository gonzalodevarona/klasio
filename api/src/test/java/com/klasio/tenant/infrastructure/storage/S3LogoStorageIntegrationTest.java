package com.klasio.tenant.infrastructure.storage;

import com.klasio.shared.infrastructure.config.S3Properties;
import com.klasio.shared.infrastructure.exception.InvalidFileTypeException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Testcontainers
class S3LogoStorageIntegrationTest {

    private static final String BUCKET_NAME = "klasio-logos-test";

    @Container
    static LocalStackContainer localStack = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.4")
    ).withServices(LocalStackContainer.Service.S3);

    private S3Client s3Client;
    private S3LogoStorage logoStorage;

    @BeforeEach
    void setUp() {
        s3Client = S3Client.builder()
                .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
                .region(Region.of(localStack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .forcePathStyle(true)
                .build();

        S3Presigner presigner = S3Presigner.builder()
                .endpointOverride(localStack.getEndpointOverride(LocalStackContainer.Service.S3))
                .region(Region.of(localStack.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(localStack.getAccessKey(), localStack.getSecretKey())
                ))
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(true)
                        .build())
                .build();

        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(BUCKET_NAME).build());
        } catch (Exception ignored) {
            // bucket may already exist
        }

        S3Properties s3Properties = new S3Properties(
                localStack.getEndpointOverride(LocalStackContainer.Service.S3).toString(),
                localStack.getRegion(),
                BUCKET_NAME
        );
        logoStorage = new S3LogoStorage(s3Client, presigner, s3Properties);
    }

    @Test
    @DisplayName("should upload file and verify it exists in S3")
    void upload_validImage_existsInS3() {
        byte[] pngHeader = createMinimalPng();
        InputStream data = new ByteArrayInputStream(pngHeader);
        UUID tenantId = UUID.randomUUID();

        String key = logoStorage.upload(tenantId, data, "image/png", pngHeader.length);

        assertThat(key).startsWith("logos/" + tenantId + "/");
        assertThat(key).endsWith(".png");

        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build();
        assertThat(s3Client.headObject(headRequest)).isNotNull();
    }

    @Test
    @DisplayName("should delete file and verify it is removed from S3")
    void delete_existingFile_removedFromS3() {
        byte[] pngHeader = createMinimalPng();
        InputStream data = new ByteArrayInputStream(pngHeader);
        UUID tenantId = UUID.randomUUID();

        String key = logoStorage.upload(tenantId, data, "image/png", pngHeader.length);

        logoStorage.delete(key);

        HeadObjectRequest headRequest = HeadObjectRequest.builder()
                .bucket(BUCKET_NAME)
                .key(key)
                .build();
        assertThatThrownBy(() -> s3Client.headObject(headRequest))
                .isInstanceOf(NoSuchKeyException.class);
    }

    @Test
    @DisplayName("should throw InvalidFileTypeException for non-image MIME type")
    void upload_invalidMimeType_throwsInvalidFileTypeException() {
        byte[] textContent = "not an image".getBytes();
        InputStream data = new ByteArrayInputStream(textContent);
        UUID tenantId = UUID.randomUUID();

        assertThatThrownBy(() -> logoStorage.upload(tenantId, data, "text/plain", textContent.length))
                .isInstanceOf(InvalidFileTypeException.class);
    }

    @Test
    @DisplayName("should generate presigned URL for existing logo key")
    void generatePresignedUrl_existingKey_returnsUrl() {
        byte[] pngHeader = createMinimalPng();
        InputStream data = new ByteArrayInputStream(pngHeader);
        UUID tenantId = UUID.randomUUID();

        String key = logoStorage.upload(tenantId, data, "image/png", pngHeader.length);

        String url = logoStorage.generatePresignedUrl(key);

        assertThat(url).isNotBlank();
        assertThat(url).contains(key);
    }

    private byte[] createMinimalPng() {
        // Minimal valid PNG: 8-byte signature + IHDR chunk + IEND chunk
        return new byte[]{
                (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, // PNG signature
                0x00, 0x00, 0x00, 0x0D, // IHDR length
                0x49, 0x48, 0x44, 0x52, // IHDR type
                0x00, 0x00, 0x00, 0x01, // width: 1
                0x00, 0x00, 0x00, 0x01, // height: 1
                0x08, 0x02,             // bit depth: 8, color type: 2 (RGB)
                0x00, 0x00, 0x00,       // compression, filter, interlace
                0x1E, (byte) 0x92, 0x6E, (byte) 0x05, // IHDR CRC
                0x00, 0x00, 0x00, 0x00, // IEND length
                0x49, 0x45, 0x4E, 0x44, // IEND type
                (byte) 0xAE, 0x42, 0x60, (byte) 0x82  // IEND CRC
        };
    }
}
