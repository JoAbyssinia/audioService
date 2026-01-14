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

    int workerInstances = config().getInteger("workerInstances", 5);
    DeploymentOptions worker =
        new DeploymentOptions()
            .setInstances(workerInstances)
            .setThreadingModel(ThreadingModel.WORKER);
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

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    super.stop(stopPromise);
    logger.info("Audio service stopped");
  }

  private Future<Void> deployHelper(String className, DeploymentOptions deploymentOptions) {
    Promise<Void> promise = Promise.promise();

    vertx
        .deployVerticle(className, deploymentOptions)
        .onSuccess(
            id -> {
              logger.info("Deployed verticle " + className);
              promise.complete();
            })
        .onFailure(
            err -> {
              logger.error("Failed to deploy verticle " + className, err);
              promise.fail(err);
            });

    return promise.future();
  }
}
