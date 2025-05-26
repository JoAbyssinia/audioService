package com.JoAbyssinia.audioService;

import com.JoAbyssinia.audioService.verticle.AudioTranscodeWorkerVerticle;
import com.JoAbyssinia.audioService.verticle.MetadataVerticle;
import io.vertx.core.*;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

  Logger logger = LoggerFactory.getLogger(MainVerticle.class);

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    DeploymentOptions worker =
        new DeploymentOptions().setInstances(5).setThreadingModel(ThreadingModel.WORKER);
    DeploymentOptions instance = new DeploymentOptions().setInstances(1);

    Future.all(
            deployHelper(MetadataVerticle.class.getName(), instance),
            deployHelper(AudioTranscodeWorkerVerticle.class.getName(), worker))
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

  private Future<Void> deployHelper(String name, DeploymentOptions deploymentOptions) {
    Promise<Void> promise = Promise.promise();

    vertx
        .deployVerticle(name, deploymentOptions)
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
