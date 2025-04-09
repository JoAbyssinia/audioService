package com.JoAbyssinia.audioService.verticle;

import com.JoAbyssinia.audioService.aws.AwsS3Client;
import com.JoAbyssinia.audioService.config.PostgresConfig;
import com.JoAbyssinia.audioService.config.S3Config;
import com.JoAbyssinia.audioService.eventBus.MetadataEventBus;
import com.JoAbyssinia.audioService.interceptor.ErrorInterceptor;
import com.JoAbyssinia.audioService.interceptor.LogInterceptor;
import com.JoAbyssinia.audioService.interceptor.MetricsInterceptor;
import com.JoAbyssinia.audioService.repository.AudioMetadataRepository;
import com.JoAbyssinia.audioService.router.AudioRouter;
import com.JoAbyssinia.audioService.service.AudioService;
import com.JoAbyssinia.audioService.service.AudioServiceImpl;
import com.JoAbyssinia.audioService.service.AudioTransCoderService;
import com.JoAbyssinia.audioService.service.AudioTransCoderServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServerOptions;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import lombok.NoArgsConstructor;

/**
 * @author Yohannes k Yimam
 */
@NoArgsConstructor
public class MetadataVerticle extends AbstractVerticle {

  private final Logger logger = LoggerFactory.getLogger(MetadataVerticle.class);

  @Override
  public void start() throws Exception {
    // create event bus
    EventBus eventBus = vertx.eventBus();
    // initialise classes
    PostgresConfig postgresConfig = new PostgresConfig(vertx);
    AudioMetadataRepository audioMetadataRepository =
        new AudioMetadataRepository(postgresConfig.getPool());

    // for pre signed generator.
    AwsS3Client awsS3Client =
        new AwsS3Client(vertx, S3Config.getS3AsyncClient(), S3Config.getS3Presigner());
    AudioTransCoderService audioTransCoderService = new AudioTransCoderServiceImpl(awsS3Client);

    AudioService audioService =
        new AudioServiceImpl(eventBus, audioMetadataRepository, audioTransCoderService);
    // interceptors
    LogInterceptor logInterceptor = new LogInterceptor();
    ErrorInterceptor errorInterceptor = new ErrorInterceptor();
    MetricsInterceptor metricsInterceptor = new MetricsInterceptor();

    // create event bus listener
    new MetadataEventBus(eventBus, audioService).evenBus();

    // router
    Router router =
        new AudioRouter(vertx, audioService, logInterceptor, errorInterceptor, metricsInterceptor)
            .getRouter();
    HttpServerOptions httpServerOptions = new HttpServerOptions();
    httpServerOptions.setHost("0.0.0.0").setPort(8888);
    vertx
        .createHttpServer(httpServerOptions)
        .requestHandler(router)
        .listen()
        .onComplete(
            http -> {
              if (http.succeeded()) {
                logger.info("Server started on port 8888");
                System.out.println("HTTP server started on port 8888");
              } else {
                logger.error("Server failed to start on port 8888", http.cause());
              }
            });
  }
}
