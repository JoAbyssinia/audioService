package com.JoAbyssinia.audioService.eventBus;

import com.JoAbyssinia.audioService.service.AudioService;
import com.JoAbyssinia.audioService.worker.util.Constant;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;

/**
 * @author Yohannes k Yimam
 */
public class MetadataEventBus {

  Logger logger = LoggerFactory.getLogger(MetadataEventBus.class);

  private EventBus eventBus;
  private AudioService audioService;

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
              Long id = json.getLong("id");
              String title = json.getString("title");
              String distinction = json.getString("distinction");

              logger.info(
                  "metadata update received id: "
                      + id
                      + " title: "
                      + title
                      + " distinction: "
                      + distinction);
            });
  }
}
