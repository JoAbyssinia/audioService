package com.JoAbyssinia.audioService.aws;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.time.Duration;
import java.util.UUID;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.async.AsyncRequestBody;
import software.amazon.awssdk.core.async.AsyncResponseTransformer;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

/**
 * @author Yohannes k Yimam
 */
public class S3ClientService {

  private final Logger logger = LoggerFactory.getLogger(S3ClientService.class);

  private final S3AsyncClient s3AsyncClient;
  private final S3Presigner s3Presigner;
  private final Vertx vertx;

  private final String bucket = "audio-files";

  public S3ClientService(Vertx vertx, S3AsyncClient s3AsyncClient, S3Presigner s3Presigner) {
    this.s3AsyncClient = s3AsyncClient;
    this.s3Presigner = s3Presigner;
    this.vertx = vertx;
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
        uploadAudioFileToS3(file.getAbsoluteFile(), s3Key);
      } else if (file.isDirectory()) {
        // Recursively upload subdirectories
        uploadFolderToS3(file, s3FolderKey + "/" + file.getName());
      }
    }

    logger.info("Uploaded folder {} to S3 at {}", folder.getAbsolutePath(), s3FolderKey);
  }

  public void uploadAudioFileToS3(File file, String s3FolderKey) {
    Promise<Void> promise = Promise.promise();
    this.vertx
        .fileSystem()
        .open(
            file.getAbsolutePath(),
            new OpenOptions().setRead(true),
            asyncResult -> {
              if (asyncResult.failed()) {
                promise.fail("Failed to open " + asyncResult.cause());
                return;
              }

              AsyncFile asyncFile = asyncResult.result();
              Publisher<ByteBuffer> publisher = toByteBufferPublisher(asyncFile);
              PutObjectRequest request =
                  PutObjectRequest.builder()
                      .bucket(bucket)
                      .key(s3FolderKey)
                      .contentType("application/x-mpegURL")
                      .build();

              s3AsyncClient
                  .putObject(request, AsyncRequestBody.fromPublisher(publisher))
                  .whenComplete(
                      (response, error) -> {
                        if (error != null) {
                          logger.error(
                              "Failed to upload file {} to S3: {}",
                              file.getAbsoluteFile(),
                              error.getMessage());
                          promise.fail(error);
                        } else {
                          logger.info(
                              "Uploaded file {} to S3 at {}", file.getAbsolutePath(), s3FolderKey);
                          promise.complete();
                        }
                        asyncFile.close();
                      });
            });

    promise.future();
  }

  private Publisher<ByteBuffer> toByteBufferPublisher(AsyncFile asyncFile) {
    return subscriber ->
        asyncFile
            .handler(
                buffer -> {
                  subscriber.onNext(ByteBuffer.wrap(buffer.getBytes()));
                })
            .endHandler(v -> subscriber.onComplete())
            .exceptionHandler(subscriber::onError);
  }

  public Future<File> downloadFile(String fileName) {
    Promise<File> promise = Promise.promise();

    // Create a temp file path
    String tempFilePath =
        "input/temp_audio_" + fileName.split("\\.")[0] + "_" + UUID.randomUUID() + ".mp3";
    Path tempFile = Path.of(tempFilePath);

    // Create S3 request
    GetObjectRequest request = GetObjectRequest.builder().bucket(bucket).key(fileName).build();

    // Download from S3 and write to temp file asynchronously
    s3AsyncClient
        .getObject(request, AsyncResponseTransformer.toBytes())
        .whenComplete(
            (responseBytes, error) -> {
              if (error != null) {
                promise.fail("Error downloading file from S3: " + error.getMessage());
                return;
              }

              // Write to file using Vert.x file system
              vertx
                  .fileSystem()
                  .writeFile(tempFile.toString(), Buffer.buffer(responseBytes.asByteArray()))
                  .onSuccess(v -> promise.complete(tempFile.toFile()))
                  .onFailure(promise::fail);
            });

    return promise.future();
  }

  public Future<String> createFolder(String folderName) {
    Promise<String> promise = Promise.promise();

    // Ensure the folder name ends with a trailing slash
    if (!folderName.endsWith("/")) {
      folderName += "_output/";
    }

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
                logger.error("Error creating folder {}: {}", finalFolderName, error.getMessage());
                promise.fail(error);
              } else {
                logger.info("Folder {} created successfully", finalFolderName);
                promise.complete(finalFolderName);
              }
            });

    return promise.future();
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

  public Future<Void> listFiles() {
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
              }
            });
    promise.complete();
    return promise.future();
  }
}
