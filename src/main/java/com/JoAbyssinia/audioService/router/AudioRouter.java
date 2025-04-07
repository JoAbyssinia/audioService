package com.JoAbyssinia.audioService.router;

import com.JoAbyssinia.audioService.entity.Audio;
import com.JoAbyssinia.audioService.service.AudioService;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import java.util.Set;

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

    // allow cors
    router
        .route()
        .handler(
            CorsHandler.create()
                .allowedMethods(
                    Set.of(
                        HttpMethod.GET,
                        HttpMethod.POST,
                        HttpMethod.PUT,
                        HttpMethod.DELETE,
                        HttpMethod.OPTIONS))
                .allowedHeaders(Set.of("Authorization", "Content-Type")));

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
                              .end(Json.encode(ar)))
                  .onFailure(error -> context.fail(500, error));
            });

    // update

    router
        .get("/audio/list")
        .handler(
            context ->
                audioService
                    .findAll()
                    .onSuccess(
                        audioList ->
                            context
                                .response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .end(Json.encode(audioList)))
                    .onFailure(error -> context.fail(500, error)));

    router
        .get("/audio/presigned")
        .handler(
            context -> {
              String fileName = context.queryParams().get("fileName");
              audioService
                  .generatePresignedUrl(fileName)
                  .onSuccess(
                      presignedUrl -> {
                        JsonObject resignedUrlJson = new JsonObject();
                        resignedUrlJson.put("presignedUrl", presignedUrl);
                        context
                            .response()
                            .putHeader("content-type", "application/json")
                            .setStatusCode(200)
                            .end((resignedUrlJson.encode()));
                      })
                  .onFailure(error -> context.fail(500, error));
            });
    router
      .get("/health")
      .handler(context -> {
        context.response().setStatusCode(200).end("OK");
      });
    return router;
  }
}
