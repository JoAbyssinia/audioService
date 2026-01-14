package com.JoAbyssinia.audioService.eventBus;

import com.JoAbyssinia.audioService.entity.AudioStatus;
import com.JoAbyssinia.audioService.service.AudioService;
import com.JoAbyssinia.audioService.util.Constant;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

/**
 * @author Yohannes k Yimam
 */
public class MetadataEventBus {

  Logger logger = LoggerFactory.getLogger(MetadataEventBus.class);

  private final EventBus eventBus;
  private final AudioService audioService;

  public MetadataEventBus(EventBus eventBus, AudioService audioService) {
    this.eventBus = eventBus;
    this.audioService = audioService;
  }

  public void evenBus() {
    eventBus
        .consumer(Constant.METADATA_UPDATE_ADDRESS)
        .handler(
            event -> {
              JsonObject json = new JsonObject(event.body().toString());

              updateAudioStatus(json)
                  .onSuccess(
                      r ->
                          logger.info(
                              "Metadata updated to "
                                  + json.getLong("id")
                                  + " title: "
                                  + json.getString("title")))
                  .onFailure(err -> logger.error(err.getMessage()));
            });
  }

  private Future<Void> updateAudioStatus(JsonObject json) {
    Promise<Void> promise = Promise.promise();
    Long id = json.getLong("id");
    String distinction = json.getString("distinction");

    audioService
        .update(AudioStatus.COMPLETED, distinction, id)
        .onSuccess(v -> promise.complete())
        .onFailure(err -> promise.fail("audio update failed id: " + id));

    return promise.future();
  }
}
