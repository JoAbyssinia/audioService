package com.JoAbyssinia.audioService.router;

import com.JoAbyssinia.audioService.entity.Audio;
import com.JoAbyssinia.audioService.service.AudioService;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author Yohannes k Yimam
 */
public class AudioRouter {

  private final Vertx vertx;
  private final AudioService audioService;

  public AudioRouter(Vertx vertx, AudioService audioService) {
    this.vertx = vertx;
    this.audioService = audioService;
  }

  public Router getRouter() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    //    save
    router
        .post("/audio/save")
        .handler(
            context -> {
              Audio audio = new Audio();
              audio.setTitle(context.queryParams().get("title"));
              audio.setOriginalPath(context.queryParams().get("originalPath"));

              audioService
                  .save(audio)
                  .onSuccess(
                      ar ->
                          context
                              .response()
                              .putHeader("content-type", "application/json")
                              .setStatusCode(200)
                              .end(Json.encode(audio)))
                  .onFailure(error -> context.fail(500, error));
            });

    // update

    router
        .get("/audio/list")
        .handler(
            context -> {
              audioService
                  .findAll()
                  .onSuccess(
                      audioList -> {
                        context
                            .response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end(Json.encode(audioList));
                      })
                  .onFailure(
                      error -> {
                        context.fail(500, error);
                      });
            });

    return router;
  }
}
