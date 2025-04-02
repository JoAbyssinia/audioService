package com.JoAbyssinia.audioService.eventBus;

import com.JoAbyssinia.audioService.service.AudioTransCoderService;
import com.JoAbyssinia.audioService.worker.util.AudioUtil;
import com.JoAbyssinia.audioService.worker.util.Constant;
import com.JoAbyssinia.audioService.worker.util.FileHelper;
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
import java.util.AbstractMap;

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

    // Start the chain with downloadFile
    audioTransCoderService
        .downloadFile(title)
        .compose(
            file -> {
              // Transcode file
              logger.info("Transcoding " + title + " to " + outputFile.getAbsolutePath());

              vertx.executeBlocking(
                  () ->
                      AudioUtil.transcodeToM3u8(file, outputFile)
                          .onSuccess(result -> promise.complete()));
              return Future.succeededFuture(file);
            })
        .compose(
            file -> {
              // Create folder in S3 bucket
              return audioTransCoderService
                  .createFolderS3(title)
                  .map(
                      s3folderName -> {
                        logger.info("folder " + s3folderName + " created");
                        // Return both file and folderName for next step
                        return new AbstractMap.SimpleEntry<>(file, s3folderName);
                      });
            })
        .compose(
            entry -> {
              File file = entry.getKey();
              String s3folderName = entry.getValue();

              // Upload file
              return audioTransCoderService
                  .uploadFolderToS3(outputFile, s3folderName)
                  .map(
                      uploadResult -> {

                        // Send a message
                        String transcodeFileName = s3folderName + "output.m3u8";
                        var jsonMsg =
                            new JsonObject()
                                .put("id", id)
                                .put("title", title)
                                .put("distinction", transcodeFileName);
                        this.eventBus.send(Constant.METADATA_UPDATE_ADDRESS, jsonMsg);

                        // Clean up
                        FileHelper.deleteTempFolder(file);
                        FileHelper.deleteTempFolder(outputFile);

                        return null;
                      });
            })
        .onSuccess(v -> promise.complete())
        .onFailure(
            throwable -> {
              logger.error("Transcode failed: " + throwable.getMessage(), throwable);
              promise.fail(throwable);
            });

    return promise.future();
  }
}
