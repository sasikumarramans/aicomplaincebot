package com.aicompliance.infrastructure.storage;

import com.aicompliance.infrastructure.config.AppProperties;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3ClientConfig {

    private final AppProperties appProperties;

    public S3ClientConfig(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @Bean
    public S3Client s3Client() {
        AppProperties.Storage storage = appProperties.getStorage();
        return S3Client.builder()
                .endpointOverride(URI.create(storage.getEndpoint()))
                .region(Region.of(storage.getRegion()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storage.isPathStyleAccess())
                        .build())
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        AppProperties.Storage storage = appProperties.getStorage();
        return S3Presigner.builder()
                .endpointOverride(URI.create(storage.getEndpoint()))
                .region(Region.of(storage.getRegion()))
                .credentialsProvider(credentialsProvider())
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(storage.isPathStyleAccess())
                        .build())
                .build();
    }

    private StaticCredentialsProvider credentialsProvider() {
        AppProperties.Storage storage = appProperties.getStorage();
        return StaticCredentialsProvider.create(
                AwsBasicCredentials.create(storage.getAccessKey(), storage.getSecretKey()));
    }
}
