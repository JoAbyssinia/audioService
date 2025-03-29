package com.JoAbyssinia.audioService.aws;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * @author Yohannes k Yimam
 */

public class S3ClientService {

  private final Logger logger = LoggerFactory.getLogger(S3ClientService.class);

  private final S3Client s3Client;
  private final S3Presigner s3Presigner;

  private final String bucket = "audio-file";

  public S3ClientService(S3Client s3Client, S3Presigner s3Presigner) {
    this.s3Client = s3Client;
    this.s3Presigner = s3Presigner;
  }


  public void uploadFolderToS3(File folder, String s3FolderKey) {
    if (!folder.exists() || !folder.isDirectory()) {
      throw new IllegalArgumentException(
        "Provided path is not a valid directory: " + folder.getAbsolutePath());
    }

    File[] files = folder.listFiles();
    if (files == null) {
      throw new RuntimeException("Failed to list files in folder: " + folder.getAbsolutePath());
    }

    for (File file : files) {
      if (file.isFile()) {
        String s3Key = s3FolderKey + file.getName(); // Maintain folder structure in S3
        uploadToS3(file.getAbsoluteFile(), s3Key);
      } else if (file.isDirectory()) {
        // Recursively upload subdirectories
        uploadFolderToS3(file, s3FolderKey + "/" + file.getName());
      }
    }

    logger.info("Uploaded folder {} to S3 at {}", folder.getAbsolutePath(), s3FolderKey);
  }

  public void uploadToS3(File file, String s3Key) {

    try {
      s3Client.putObject(
        PutObjectRequest.builder()
          .bucket(bucket)
          .key(s3Key)
          .contentType("application/x-mpegURL")
          .build(),
        RequestBody.fromBytes(Files.readAllBytes(file.toPath())));
    } catch (IOException e) {
      throw new RuntimeException("Error uploading file to S3", e);
    }
    logger.info("Uploaded {} to {}", file, s3Key);
  }

  public File downloadFile(String fileName) {
    File tempFile =
      new File("input/temp_audio_" + fileName.split("\\.")[0] + "_" + UUID.randomUUID() + ".mp3");

    // create object request
    GetObjectRequest objectRequest =
      GetObjectRequest.builder().bucket(bucket).key(fileName).build();

    // download
    ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObjectAsBytes(objectRequest);

    try (FileOutputStream fos = new FileOutputStream(tempFile)) {
      fos.write(objectBytes.asByteArray());
    } catch (IOException e) {
      throw new RuntimeException("Error downloading file from s3", e);
    }

    return tempFile;
  }

  public String createFolder(String folderName) {

    if (!folderName.endsWith("/")) {
      folderName += "_output/";
    }

    try {
      PutObjectRequest putObjectRequest =
        PutObjectRequest.builder().bucket(bucket).key(folderName).build();
      s3Client.putObject(putObjectRequest, RequestBody.empty());
    } catch (AwsServiceException | SdkClientException e) {
      logger.error("Error on folder creating {}", e.getMessage());
    }

    logger.info("Folder {} created", folderName);
    return folderName;
  }

  public String generateResignedUrl(String fileName) {
    PresignedGetObjectRequest objectRequest =
      s3Presigner.presignGetObject(
        builder ->
          builder
            .signatureDuration(Duration.ofMinutes(30))
            .getObjectRequest(
              GetObjectRequest.builder().bucket(bucket).key(fileName).build()));
    URL url = objectRequest.url();
    return url.toString();
  }

  public void listFiles() {
    ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).build();
    s3Client.listObjectsV2(request).contents().stream().map(S3Object::key).forEach(System.out::println);
  }

}
