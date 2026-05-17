package com.JoAbyssinia.audioService;

import com.JoAbyssinia.audioService.verticle.AudioTranscodeWorkerVerticle;
import com.JoAbyssinia.audioService.verticle.MetadataVerticle;
import io.vertx.core.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MainVerticle extends AbstractVerticle {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    vertx
        .deployVerticle(MainVerticle.class.getName())
        .onSuccess(id -> log.info("MainVerticle deployed: {}", id))
        .onFailure(
            err -> {
              log.info("Failed to deploy MainVerticle: {}", err.getMessage());
              System.exit(1);
            });
  }

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    int workerInstances = config().getInteger("workerInstances", 3);
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
                log.info("Audio service deployed");
                startPromise.complete();
              } else {
                log.info("Audio service failed");
                startPromise.fail(result.cause());
              }
            });
  }

  @Override
  public void stop(Promise<Void> stopPromise) throws Exception {
    super.stop(stopPromise);
    log.info("Audio service stopped");
  }

  private Future<Void> deployHelper(String className, DeploymentOptions deploymentOptions) {
    Promise<Void> promise = Promise.promise();

    vertx
        .deployVerticle(className, deploymentOptions)
        .onSuccess(
            id -> {
              log.info("Deployed verticle {}", className);
              promise.complete();
            })
        .onFailure(
            err -> {
              log.error("Failed to deploy verticle {}", className, err);
              promise.fail(err);
            });

    return promise.future();
  }
}
