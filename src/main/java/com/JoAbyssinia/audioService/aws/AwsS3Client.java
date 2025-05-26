package com.JoAbyssinia.audioService.aws;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @author Yohannes k Yimam
 */
public class AwsS3Client {

  private final Logger logger = LoggerFactory.getLogger(AwsS3Client.class);

  private final S3AsyncClient s3AsyncClient;
  private final S3Presigner s3Presigner;
  private final Vertx vertx;

  private final String bucket = "audio-files";

  public AwsS3Client(Vertx vertx, S3AsyncClient s3AsyncClient, S3Presigner s3Presigner) {
    this.s3AsyncClient = s3AsyncClient;
    this.s3Presigner = s3Presigner;
    this.vertx = vertx;
  }

  public Future<Void> uploadFolderToS3(File folder, String s3FolderKey) {
    Promise<Void> promise = Promise.promise();
    if (!folder.exists() || !folder.isDirectory()) {
      return Future.failedFuture(
          new IllegalArgumentException(
              "Provided path is not a valid directory: " + folder.getAbsolutePath()));
    }

    File[] files = folder.listFiles();
    if (files == null) {
      return Future.failedFuture(
          new IllegalArgumentException(
              "Provided path is not a valid directory: " + folder.getAbsolutePath()));
    }

    // Create a list to track all file upload futures
    List<Future> uploadFutures = new ArrayList<>();

    for (File file : files) {
      if (file.isFile()) {
        String s3Key = s3FolderKey + file.getName(); // Maintain folder structure in S3
        uploadFutures.add(uploadAudioFileToS3(file.getAbsoluteFile(), s3Key));
      } else if (file.isDirectory()) {
        // Recursively upload subdirectories
        uploadFutures.add(uploadFolderToS3(file, s3FolderKey + "/" + file.getName()));
      }
    }

    CompositeFuture.all(uploadFutures)
        .onSuccess(
            result -> {
              logger.info(
                  "Uploaded folder " + folder.getAbsolutePath() + " to S3 at " + s3FolderKey);
              promise.complete();
            })
        .onFailure(
            throwable -> {
              logger.error(
                  "Failed to upload folder " + folder.getAbsolutePath() + " to S3", throwable);
              promise.fail(throwable);
            });

    return promise.future();
  }

  private Future<Void> uploadAudioFileToS3(File file, String s3FolderKey) {
    Promise<Void> promise = Promise.promise();

    vertx
        .fileSystem()
        .readFile(file.getAbsolutePath())
        .onSuccess(
            readResult -> {
              PutObjectRequest request =
                  PutObjectRequest.builder()
                      .bucket(bucket)
                      .key(s3FolderKey)
                      .contentType("application/x-mpegURL")
                      .build();

              s3AsyncClient
                  .putObject(request, AsyncRequestBody.fromBytes(readResult.getBytes()))
                  .whenComplete(
                      (result, error) -> {
                        if (error != null) {
                          logger.error("Error uploading file to S3", error);
                          promise.fail(error);
                        } else promise.complete();
                      });
            })
        .onFailure(promise::fail);

    return promise.future();
  }

  public Future<File> downloadFile(String fileName) {
    Promise<File> promise = Promise.promise();

    try {
      // Create a temp file in the system temp directory
      Path tempFilePath = Files.createTempFile("temp_audio_" + fileName + "_", ".mp3");
      File tempFile = tempFilePath.toFile();

      // Create S3 request
      String downloadFileName = fileName + ".mp3";
      GetObjectRequest request =
          GetObjectRequest.builder().bucket(bucket).key(downloadFileName).build();

      // Download from S3 and write to temp file asynchronously
      s3AsyncClient
          .getObject(request, AsyncResponseTransformer.toBytes())
          .whenComplete(
              (responseBytes, error) -> {
                if (error != null) {
                  logger.error("Failed to download file " + downloadFileName);
                  promise.fail("Error downloading file from S3: " + error.getMessage());
                  return;
                }

                // Write a file using Vert.x async file system (Non-blocking)
                vertx
                    .fileSystem()
                    .writeFile(tempFilePath.toString(), Buffer.buffer(responseBytes.asByteArray()))
                    .onSuccess(v -> promise.complete(tempFile))
                    .onFailure(promise::fail);
              });

    } catch (IOException e) {
      logger.error("Failed to download file " + fileName);
      promise.fail("Failed to create temp file: " + e.getMessage());
    }

    return promise.future();
  }

  public Future<String> createFolder(String folderName) {
    Promise<String> promise = Promise.promise();

    // Create the S3 request
    PutObjectRequest putObjectRequest =
        PutObjectRequest.builder().bucket(bucket).key(folderName).build();

    final String finalFolderName = folderName;
    // Use `AsyncRequestBody.fromBytes(new byte[0])` for an empty object
    s3AsyncClient
        .putObject(putObjectRequest, AsyncRequestBody.fromBytes(new byte[0]))
        .whenComplete(
            (PutObjectResponse response, Throwable error) -> {
              if (error != null) {
                logger.error("Error creating folder " + finalFolderName + ":" + error.getMessage());
                promise.fail(error);
              } else promise.complete(finalFolderName);
            });

    return promise.future();
  }

  public Future<String> generateResignedUrl(String fileName) {
    Promise<String> promise = Promise.promise();

    promise.complete(
        s3Presigner
            .presignGetObject(
                builder ->
                    builder
                        .signatureDuration(Duration.ofMinutes(10))
                        .getObjectRequest(
                            GetObjectRequest.builder().bucket(bucket).key(fileName).build()))
            .url()
            .toString());
    return promise.future();
  }

  private Future<Void> listFiles() {
    Promise<Void> promise = Promise.promise();
    ListObjectsV2Request request = ListObjectsV2Request.builder().bucket(bucket).build();
    s3AsyncClient
        .listObjectsV2(request)
        .whenComplete(
            (res, err) -> {
              if (err != null) {
                promise.fail(err);
              } else {
                res.contents().stream().map(S3Object::key).forEach(System.out::println);
                promise.complete();
              }
            });

    return promise.future();
  }
}
