package com.graduation.competition.config;

import io.minio.MinioClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置。连接失败由具体存储操作返回业务错误，不阻断应用启动。
 */
@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(
            @Value("${app.storage.endpoint}") String endpoint,
            @Value("${app.storage.access-key}") String accessKey,
            @Value("${app.storage.secret-key}") String secretKey) {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}

