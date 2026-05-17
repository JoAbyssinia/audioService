package com.JoAbyssinia.audioService.service;

import com.JoAbyssinia.audioService.aws.AwsS3Client;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import java.io.File;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yohannes k Yimam
 */
@Slf4j
public class AudioTransCoderServiceImpl implements AudioTransCoderService {

  private final AwsS3Client awsS3Client;

  public AudioTransCoderServiceImpl(AwsS3Client awsS3Client) {
    this.awsS3Client = awsS3Client;
  }

  public Future<Void> uploadFolderToS3(File file, String s3FolderKey) {
    return awsS3Client.uploadFolderToS3(file, s3FolderKey);
  }

  public Future<File> downloadFile(String audioFileLocation, String fileName) {
    Promise<File> promise = Promise.promise();
    awsS3Client
        .downloadFile(audioFileLocation, fileName)
        .onSuccess(
            result -> {
              log.info(
                  "Downloaded file {} to S3 at {}", result.getName(), result.getAbsolutePath());
              promise.complete(result);
            })
        .onFailure(
            err -> {
              log.error("download failed {}", err.getMessage(), err);
              promise.fail(err);
            });

    return promise.future();
  }

  public Future<String> createFolderS3(String folderName) {
    Promise<String> promise = Promise.promise();
    if (!folderName.endsWith("/")) {
      folderName += "_output/";
    }

    awsS3Client
        .createFolder(folderName)
        .onSuccess(promise::complete)
        .onFailure(
            err -> {
              log.error("cant create {}", String.valueOf(err));
              promise.fail(err);
            });
    return promise.future();
  }
}
