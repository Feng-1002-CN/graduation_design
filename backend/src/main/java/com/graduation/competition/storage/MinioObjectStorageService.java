package com.graduation.competition.storage;

import com.graduation.competition.common.BizException;
import com.graduation.competition.common.ErrorCode;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * MinIO 对象存储实现，首次上传时自动创建文件桶。
 */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.storage.provider", havingValue = "minio", matchIfMissing = true)
public class MinioObjectStorageService implements ObjectStorageService {

    private final MinioClient minioClient;

    @Value("${app.storage.bucket}")
    private String bucket;

    @Override
    public String put(String objectName, String contentType, byte[] content) {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            try (InputStream input = new ByteArrayInputStream(content)) {
                minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(objectName)
                        .contentType(contentType)
                        .stream(input, content.length, -1)
                        .build());
            }
            return "minio://" + bucket + "/" + objectName;
        } catch (Exception exception) {
            throw new BizException(ErrorCode.STORAGE_UNAVAILABLE,
                    "MinIO 文件上传失败: " + exception.getMessage());
        }
    }

    @Override
    public InputStream get(String fileUrl) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName(fileUrl))
                    .build());
        } catch (Exception exception) {
            throw new BizException(ErrorCode.STORAGE_UNAVAILABLE,
                    "MinIO 文件读取失败: " + exception.getMessage());
        }
    }

    @Override
    public void delete(String fileUrl) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectName(fileUrl))
                    .build());
        } catch (Exception exception) {
            throw new BizException(ErrorCode.STORAGE_UNAVAILABLE,
                    "MinIO 文件删除失败: " + exception.getMessage());
        }
    }

    private String objectName(String fileUrl) {
        String prefix = "minio://" + bucket + "/";
        return fileUrl.startsWith(prefix) ? fileUrl.substring(prefix.length()) : fileUrl;
    }
}

