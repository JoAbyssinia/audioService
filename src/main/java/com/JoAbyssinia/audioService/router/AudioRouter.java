package com.JoAbyssinia.audioService.router;

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

    router.get("/health").handler(context -> context.response().setStatusCode(200).end("OK"));
    return router;
  }
}
