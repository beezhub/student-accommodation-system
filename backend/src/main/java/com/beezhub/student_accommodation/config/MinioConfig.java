package com.beezhub.student_accommodation.config;

import io.minio.MinioClient;
import lombok.Getter;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class MinioConfig {

    @Value("${minio.endpoint}")
    private String url;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Getter
    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.connect-timeout-s:30}")
    private int connectTimeoutSeconds;

    @Value("${minio.read-timeout-s:30}")
    private int readTimeoutSeconds;

    @Value("${minio.write-timeout-s:30}")
    private int writeTimeoutSeconds;

    @Bean
    public MinioClient minioClient() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .connectTimeout(connectTimeoutSeconds, TimeUnit.SECONDS)
                .readTimeout(readTimeoutSeconds, TimeUnit.SECONDS)
                .writeTimeout(writeTimeoutSeconds, TimeUnit.SECONDS)
                .build();

        return MinioClient.builder()
                .endpoint(url)
                .credentials(accessKey, secretKey)
                .httpClient(httpClient)
                .build();
    }

}
