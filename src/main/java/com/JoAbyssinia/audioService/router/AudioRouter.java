package com.JoAbyssinia.audioService.router;

import com.JoAbyssinia.audioService.entity.Audio;
import com.JoAbyssinia.audioService.repository.AudioRepository;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;

/**
 * @author Yohannes k Yimam
 */
public class AudioRouter {

  private final Vertx vertx;
  private final AudioRepository repository;

  public AudioRouter(Vertx vertx, AudioRepository audioRepository) {
    this.vertx = vertx;
    this.repository = audioRepository;
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

              repository
                  .save(audio)
                  .onSuccess(
                      ar -> {
                        context
                            .response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end(Json.encode(audio));
                      })
                  .onFailure(
                      error -> {
                        context.fail(500, error);
                      });
            });

    // update

    router
        .get("/audio/list")
        .handler(
            context -> {
              repository
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
