package com.JoAbyssinia.audioService;

import com.JoAbyssinia.audioService.verticle.AudioSegmentationWorkerVerticle;
import com.JoAbyssinia.audioService.verticle.MetadataVerticle;
import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

  Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    Future.all(
            deployHelper(MetadataVerticle.class.getName()),
            deployHelper(AudioSegmentationWorkerVerticle.class.getName()))
        .onComplete(
            result -> {
              if (result.succeeded()) {
                logger.info("Audio service deployed");
                startPromise.complete();
              } else {
                logger.info("Audio service failed");
                startPromise.fail(result.cause());
              }
            });
  }

  private Future<Void> deployHelper(String name) {
    Promise<Void> promise = Promise.promise();

    vertx
        .deployVerticle(name)
        .onSuccess(
            id -> {
              logger.info("Deployed verticle " + name);
              promise.complete();
            })
        .onFailure(
            err -> {
              logger.error("Failed to deploy verticle " + name, err);
              promise.fail(err);
            });

    return promise.future();
  }
}
