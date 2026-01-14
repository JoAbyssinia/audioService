package com.JoAbyssinia.audioService.service;

import com.JoAbyssinia.audioService.aws.AwsS3Client;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import java.io.File;

/**
 * @author Yohannes k Yimam
 */
public class AudioTransCoderServiceImpl implements AudioTransCoderService {

  Logger logger = LoggerFactory.getLogger(AudioTransCoderServiceImpl.class);

  private final AwsS3Client awsS3Client;

  public AudioTransCoderServiceImpl(AwsS3Client awsS3Client) {
    this.awsS3Client = awsS3Client;
  }

  public Future<Void> uploadFolderToS3(File file, String s3FolderKey) {
    return awsS3Client.uploadFolderToS3(file, s3FolderKey);
  }

  public Future<File> downloadFile(String fileName) {
    Promise<File> promise = Promise.promise();
    awsS3Client
        .downloadFile(fileName)
        .onSuccess(
            result -> {
              logger.info(
                  "Downloaded file " + result.getName() + " to S3 at " + result.getAbsolutePath());
              promise.complete(result);
            })
        .onFailure(
            err -> {
              logger.error("download failed " + err.getMessage(), err);
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
              logger.error("cant create " + err);
              promise.fail(err);
            });
    return promise.future();
  }

  public Future<String> generateResignedUrl(String fileName, long duration) {
    Promise<String> promise = Promise.promise();

    if (fileName == null) {
      logger.error("fileName is null");
      promise.fail("fileName is null");
    }
    if (duration <= 0) {
      logger.error("duration is invalid");
      promise.fail("duration is invalid");
    }

    awsS3Client
        .generateResignedUrl(fileName, duration)
        .onSuccess(promise::complete)
        .onFailure(
            err -> {
              logger.error(err);
              promise.fail(err);
            });
    return promise.future();
  }
}
