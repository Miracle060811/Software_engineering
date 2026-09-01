package com.travelmate.storage;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Service
@ConditionalOnProperty(prefix = "app.storage", name = "type", havingValue = "s3")
public class S3FileStorageService implements FileStorageService {

    private final MinioClient client;
    private final String bucket;
    private final String publicBaseUrl;

    public S3FileStorageService(
            @Value("${app.storage.s3.endpoint:}") String endpoint,
            @Value("${app.storage.s3.region:us-east-1}") String region,
            @Value("${app.storage.s3.bucket:travelmate}") String bucket,
            @Value("${app.storage.s3.access-key:}") String accessKey,
            @Value("${app.storage.s3.secret-key:}") String secretKey,
            @Value("${app.storage.s3.public-base-url:}") String publicBaseUrl) {
        if (!StringUtils.hasText(endpoint) || !StringUtils.hasText(bucket)
                || !StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)
                || !StringUtils.hasText(publicBaseUrl)) {
            throw new IllegalStateException("S3 storage requires endpoint, bucket, credentials and public base URL");
        }
        this.client = MinioClient.builder()
                .endpoint(endpoint.trim())
                .credentials(accessKey.trim(), secretKey.trim())
                .region(region.trim())
                .build();
        this.bucket = bucket.trim();
        this.publicBaseUrl = publicBaseUrl.replaceAll("/+$", "");
    }

    @PostConstruct
    void ensureBucket() {
        try {
            boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to initialize S3 bucket " + bucket, exception);
        }
    }

    @Override
    public StoredFile store(MultipartFile file, String extension) throws IOException {
        String objectKey = StorageObjectNames.newImageKey(extension);
        try (InputStream input = file.getInputStream()) {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .contentType(file.getContentType())
                    .stream(input, file.getSize(), -1)
                    .build());
        } catch (Exception exception) {
            throw new IOException("Object storage upload failed", exception);
        }
        return new StoredFile(objectKey, publicBaseUrl + "/" + objectKey);
    }

    @Override
    public void delete(String objectKey) throws IOException {
        String safeKey = StorageObjectNames.requireSafeKey(objectKey);
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(safeKey).build());
        } catch (Exception exception) {
            throw new IOException("Object storage deletion failed", exception);
        }
    }

    @Override
    public boolean exists(String objectKey) throws IOException {
        String safeKey = StorageObjectNames.requireSafeKey(objectKey);
        try {
            client.statObject(StatObjectArgs.builder().bucket(bucket).object(safeKey).build());
            return true;
        } catch (ErrorResponseException exception) {
            if ("NoSuchKey".equals(exception.errorResponse().code())
                    || "NoSuchObject".equals(exception.errorResponse().code())) {
                return false;
            }
            throw new IOException("Object storage existence check failed", exception);
        } catch (Exception exception) {
            throw new IOException("Object storage existence check failed", exception);
        }
    }

    @Override
    public String publicUrl(String objectKey) {
        return publicBaseUrl + "/" + StorageObjectNames.requireSafeKey(objectKey);
    }
}
