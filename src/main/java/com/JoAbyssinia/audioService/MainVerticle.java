package com.JoAbyssinia.audioService;

import com.JoAbyssinia.audioService.aws.S3ClientService;
import com.JoAbyssinia.audioService.config.S3Config;
import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {

  @Override
  public void start(Promise<Void> startPromise) throws Exception {

    S3Config s3Config = new S3Config();
    S3ClientService s3ClientService = new S3ClientService(s3Config.s3Client(), s3Config.s3Presigner());
    s3ClientService.listFiles();

    vertx.createHttpServer().requestHandler(req -> {
      req.response()
        .putHeader("content-type", "text/plain")
        .end("Hello from Vert.x!");
    }).listen(8888).onComplete(http -> {
      if (http.succeeded()) {
        startPromise.complete();
        System.out.println("HTTP server started on port 8888");
      } else {
        startPromise.fail(http.cause());
      }
    });
  }
}
