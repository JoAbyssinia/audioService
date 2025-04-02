package com.JoAbyssinia.audioService.eventBus;

import com.JoAbyssinia.audioService.service.AudioTransCoderService;
import com.JoAbyssinia.audioService.util.Constant;
import com.JoAbyssinia.audioService.util.FileHelper;
import com.JoAbyssinia.audioService.worker.AudioProcesses;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Yohannes k Yimam
 */
public class AudioTranscoderEventBus {

  Logger logger = LoggerFactory.getLogger(AudioTranscoderEventBus.class);

  private final EventBus eventBus;
  private final AudioTransCoderService audioTransCoderService;
  private final Vertx vertx;

  public AudioTranscoderEventBus(
      Vertx vertx, EventBus eventBus, AudioTransCoderService audioTransCoderService) {
    this.vertx = vertx;
    this.eventBus = eventBus;
    this.audioTransCoderService = audioTransCoderService;
  }

  public void eventBus() {

    eventBus
        .consumer(Constant.AUDIO_TRANSCODE_ADDRESS)
        .handler(
            event -> {
              JsonObject json = new JsonObject(event.body().toString());
              Long id = json.getLong("id");
              String title = json.getString("title");

              try {
                transcodeAudio(vertx, id, title)
                    .onSuccess(result -> logger.info("Audio transcode successfully"))
                    .onFailure(throwable -> logger.error("Audio transcode failed"));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }

              logger.info("message received " + json.getLong("id") + " " + json.getString("title"));
            });
  }

  private Future<Void> transcodeAudio(Vertx vertx, Long id, String title) throws IOException {
    Promise<Void> promise = Promise.promise();

    // Temporary output location
    File outputFile = Files.createTempDirectory("output_" + title).toFile();
    AtomicReference<File> downloadedFile = new AtomicReference<>();

    // Start the chain with downloadFile
    audioTransCoderService
        .downloadFile(title)
        .compose(
            file -> {
              // Store reference to downloaded file for cleanup
              downloadedFile.set(file);

              // Transcode file asynchronously - using non-deprecated executeBlocking
              return vertx.executeBlocking(
                  promise1 -> {
                    try {
                      AudioProcesses.transcodeToM3u8(file, outputFile);
                      promise1.complete();
                    } catch (Exception e) {
                      logger.error("Audio transcode failed", e);
                      promise1.fail(e);
                    }
                  },
                  false); // false means don't order with other blocking operations
            })
        .compose(
            v -> {
              // Create folder in S3 bucket
              return audioTransCoderService.createFolderS3(title);
            })
        .compose(
            s3folderName -> {
              logger.info("folder " + s3folderName + " created");

              // Upload file to S3
              return vertx
                  .executeBlocking(
                      uploadPromise ->
                          audioTransCoderService
                              .uploadFolderToS3(outputFile, s3folderName)
                              .onSuccess(
                                  v -> {
                                    logger.info("Successfully uploaded files to " + s3folderName);
                                    uploadPromise.complete();
                                  })
                              .onFailure(
                                  err -> {
                                    logger.error("Audio upload failed", err);
                                    uploadPromise.fail(err);
                                  }),
                      false)
                  .map(v -> s3folderName); // Pass the s3folderName to the next step
            })
        .compose(
            s3folderName -> {
              // Send a message
              String transcodeFileName = s3folderName + "output.m3u8";
              var jsonMsg =
                  new JsonObject()
                      .put("id", id)
                      .put("title", title)
                      .put("distinction", transcodeFileName);

              eventBus.send(Constant.METADATA_UPDATE_ADDRESS, jsonMsg);
              logger.info("Transcoding of " + title + " completed successfully");
              return Future.succeededFuture();
            })
        .onComplete(
            ar -> {
              // Clean up resources regardless of success/failure
              try {
                if (downloadedFile.get() != null) {
                  FileHelper.deleteTempFolder(downloadedFile.get());
                }
                if (outputFile.exists()) {
                  FileHelper.deleteTempFolder(outputFile);
                }
              } catch (Exception e) {
                logger.warn("Cleanup failed", e);
              }

              // Complete or fail the original promise
              if (ar.succeeded()) {
                promise.complete();
              } else {
                promise.fail(ar.cause());
              }
            });

    return promise.future();
  }
}
