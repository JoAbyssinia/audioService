package com.JoAbyssinia.audioService.config;

import java.net.URI;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.rds.RdsUtilities;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

/**
 * Central AWS client factory.
 *
 * <p>Local dev (Floci): set {@code AWS_ENDPOINT_OVERRIDE=http://localhost:4566}. All SDK clients
 * will route to Floci instead of real AWS endpoints.
 */
public class AWSConfig {

  // ── Shared AWS config ───────────────────────────────────────────────────────
  private static final String ACCESS_KEY_ID =
      System.getenv().getOrDefault("AWS_ACCESS_KEY_ID", "test");
  private static final String ACCESS_KEY_SECRET =
      System.getenv().getOrDefault("AWS_SECRET_ACCESS_KEY", "test");
  private static final String REGION = System.getenv().getOrDefault("AWS_REGION", "us-east-1");
  private static final String ENDPOINT =
      System.getenv().getOrDefault("AWS_ENDPOINT", "http://localhost:4566");

  static final StaticCredentialsProvider CREDENTIALS =
      StaticCredentialsProvider.create(
          AwsBasicCredentials.create(ACCESS_KEY_ID, ACCESS_KEY_SECRET));

  private static volatile S3AsyncClient s3AsyncClientInstance;

  public static S3AsyncClient getS3AsyncClient() {
    if (s3AsyncClientInstance == null) {
      synchronized (AWSConfig.class) {
        if (s3AsyncClientInstance == null) {
          var builder =
              S3AsyncClient.builder()
                  .credentialsProvider(CREDENTIALS)
                  .region(Region.of(REGION))
                  .serviceConfiguration(
                      S3Configuration.builder().pathStyleAccessEnabled(true).build());
          if (!ENDPOINT.isBlank()) builder.endpointOverride(URI.create(ENDPOINT));
          s3AsyncClientInstance = builder.build();
        }
      }
    }
    return s3AsyncClientInstance;
  }

  public static SqsAsyncClient getSqsClient() {
    return SqsClientInstanceHolder.INSTANCE;
  }

  private static final class SqsClientInstanceHolder {
    private static final SqsAsyncClient INSTANCE;

    static {
      var builder =
          SqsAsyncClient.builder().credentialsProvider(CREDENTIALS).region(Region.of(REGION));
      if (!ENDPOINT.isBlank()) builder.endpointOverride(URI.create(ENDPOINT));
      INSTANCE = builder.build();
    }
  }

  private static final class RdsUtilitiesHolder {
    private static final RdsUtilities INSTANCE =
        RdsUtilities.builder().credentialsProvider(CREDENTIALS).region(Region.of(REGION)).build();
  }

  /**
   * Generates an AWS RDS IAM authentication token to be used as the PostgreSQL password.
   *
   * <p>Prerequisites:
   *
   * <ul>
   *   <li>The IAM principal must have the {@code rds-db:connect} permission.
   *   <li>The PostgreSQL user must be granted the {@code rds_iam} role: {@code GRANT rds_iam TO
   *       <username>;}
   * </ul>
   *
   * @param host RDS instance endpoint hostname
   * @param port RDS port (typically 5432 for PostgreSQL)
   * @param username PostgreSQL username
   * @return short-lived IAM auth token (valid 15 minutes)
   */
  public static String generateRdsIamToken(String host, int port, String username) {
    return RdsUtilitiesHolder.INSTANCE.generateAuthenticationToken(
        r -> r.hostname(host).port(port).username(username).credentialsProvider(CREDENTIALS));
  }

  // ── S3 Presigner ────────────────────────────────────────────────────────────
  public static S3Presigner getS3Presigner() {
    return S3PresignerInstanceHolder.INSTANCE;
  }

  private static final class S3PresignerInstanceHolder {
    private static final S3Presigner INSTANCE;

    static {
      var builder =
          S3Presigner.builder()
              .credentialsProvider(CREDENTIALS)
              .region(Region.of(REGION))
              .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build());
      if (!ENDPOINT.isBlank()) builder.endpointOverride(URI.create(ENDPOINT));
      INSTANCE = builder.build();
    }
  }
}
