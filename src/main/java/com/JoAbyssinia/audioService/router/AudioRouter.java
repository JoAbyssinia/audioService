package com.JoAbyssinia.audioService.router;

import com.JoAbyssinia.audioService.entity.Audio;
import com.JoAbyssinia.audioService.interceptor.ErrorInterceptor;
import com.JoAbyssinia.audioService.interceptor.LogInterceptor;
import com.JoAbyssinia.audioService.interceptor.MetricsInterceptor;
import com.JoAbyssinia.audioService.service.AudioService;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpMethod;
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
  private final LogInterceptor logInterceptor;
  private final ErrorInterceptor errorInterceptor;
  private final MetricsInterceptor metricsInterceptor;

  public AudioRouter(
      Vertx vertx,
      AudioService audioService,
      LogInterceptor logInterceptor,
      ErrorInterceptor errorInterceptor,
      MetricsInterceptor metricsInterceptor) {
    this.vertx = vertx;
    this.audioService = audioService;
    this.logInterceptor = logInterceptor;
    this.errorInterceptor = errorInterceptor;
    this.metricsInterceptor = metricsInterceptor;
  }

  public Router getRouter() {
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());

    // allow cors
    router
        .route()
        .handler(
            CorsHandler.create()
                .addOrigin("*")
                .allowCredentials(true)
                .allowedMethods(
                    Set.of(
                        HttpMethod.GET,
                        HttpMethod.POST,
                        HttpMethod.PUT,
                        HttpMethod.DELETE,
                        HttpMethod.OPTIONS))
                .allowedHeaders(
                    Set.of(
                        "Authorization",
                        "Content-Type",
                        "Accept",
                        "Origin",
                        "Access-Control-Allow-Origin")));

    // add interceptors
    router
        .route()
        .handler(logInterceptor::interceptor)
        .failureHandler(errorInterceptor::interceptor)
        .handler(metricsInterceptor::interceptor);

    //    save
    router
        .post("/audio/save")
        .handler(
            context -> {
              Audio audio = new Audio();
              audio.setTitle(context.queryParams().get("title"));
              audio.setOriginalPath(context.queryParams().get("originalPath"));
              audio.setArtist(context.queryParams().get("artist"));
              audio.setDuration(Long.parseLong(context.queryParams().get("duration")));

              audioService
                  .setContext(context)
                  .save(audio)
                  .onSuccess(
                      ar ->
                          context
                              .response()
                              .putHeader("content-type", "application/json")
                              .setStatusCode(200)
                              .end(ar))
                  .onFailure(error -> context.fail(500, error));
            });

    // update

    router
        .get("/playlists/tracks")
        .handler(
            context ->
                audioService
                    .setContext(context)
                    .findAll()
                    .onSuccess(
                        audioList ->
                            context
                                .response()
                                .putHeader("content-type", "application/json")
                                .setStatusCode(200)
                                .end(audioList))
                    .onFailure(error -> context.fail(500, error)));

    router
        .get("/audio/presigned")
        .handler(
            context -> {
              String streamPath = context.queryParams().get("streamPath");
              String durationStr = context.queryParams().get("duration");
              audioService
                  .setContext(context)
                  .generatePresignedUrl(streamPath, Long.parseLong(durationStr))
                  .onSuccess(
                      presignedUrl ->
                          context
                              .response()
                              .putHeader("content-type", "application/json")
                              .setStatusCode(200)
                              .end((presignedUrl != null ? presignedUrl : "")))
                  .onFailure(error -> context.fail(500, error));
            });

    router.get("/health").handler(context -> context.response().setStatusCode(200).end("OK"));
    return router;
  }
}
