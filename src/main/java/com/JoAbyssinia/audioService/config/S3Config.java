package com.JoAbyssinia.audioService.config;


import java.net.URI;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @author Yohannes k Yimam
 */

public class S3Config {


  private final String endpoint = "http://localhost:4566";

  private final String accessKeyId ="112233445566";


  private final String accessKeySecret ="112233445566";

  private  final String region = "us-east-1";


  public S3Client s3Client() {

    return S3Client.builder()
      .credentialsProvider(
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKeyId, accessKeySecret)))
      .region(Region.of(region))
      .endpointOverride(URI.create(endpoint))
      .serviceConfiguration(
        S3Configuration.builder()
          .pathStyleAccessEnabled(true) // LocalStack requires path-style access
          .build())
      .build();
  }

  public S3Presigner s3Presigner() {
    return S3Presigner.builder()
      .credentialsProvider(
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create(accessKeyId, accessKeySecret)))
      .region(Region.of(region))
      .endpointOverride(URI.create(endpoint))
      .serviceConfiguration(S3Configuration.builder().pathStyleAccessEnabled(true).build())
      .build();
  }
}
