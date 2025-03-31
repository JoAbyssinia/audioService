package com.JoAbyssinia.audioService.config;

import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

public class S3Config {

  private static final String ENDPOINT =
      System.getenv().getOrDefault("AWS_S3_ENDPOINT", "http://localhost:4566");
  private static final String ACCESS_KEY_ID =
      System.getenv().getOrDefault("AWS_ACCESS_KEY_ID", "112233445566");
  private static final String ACCESS_KEY_SECRET =
      System.getenv().getOrDefault("AWS_SECRET_ACCESS_KEY", "112233445566");
  private static final String REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");

  private static volatile S3AsyncClient s3AsyncClientInstance;

  private static S3Client s3ClientInstance;

  public static S3AsyncClient getS3AsyncClient() {
    if (s3AsyncClientInstance == null) {
      synchronized (S3Config.class) {
        if (s3AsyncClientInstance == null) {
          s3AsyncClientInstance =
              S3AsyncClient.builder()
                  .credentialsProvider(
                      StaticCredentialsProvider.create(
                          AwsBasicCredentials.create(ACCESS_KEY_ID, ACCESS_KEY_SECRET)))
                  .region(Region.of(REGION))
                  .endpointOverride(URI.create(ENDPOINT))
                  .serviceConfiguration(
                      S3Configuration.builder().pathStyleAccessEnabled(true).build())
                  .build();
        }
      }
    }
    return s3AsyncClientInstance;
  }

  private static final class S3PresignerInstanceHolder {
    private static final S3Presigner s3PresignerInstance =
        S3Presigner.builder()
            .credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(ACCESS_KEY_ID, ACCESS_KEY_SECRET)))
            .region(Region.of(REGION))
            .endpointOverride(URI.create(ENDPOINT))
            .build();
  }

  public static S3Presigner getS3Presigner() {
    return S3PresignerInstanceHolder.s3PresignerInstance;
  }
}
