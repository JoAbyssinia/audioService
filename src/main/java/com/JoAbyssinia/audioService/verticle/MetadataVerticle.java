package com.JoAbyssinia.audioService.verticle;

import com.JoAbyssinia.audioService.broker.KafkaClient;
import com.JoAbyssinia.audioService.broker.KafkaConsumerService;
import com.JoAbyssinia.audioService.config.KafkaConfig;
import com.JoAbyssinia.audioService.config.PostgresConfig;
import com.JoAbyssinia.audioService.eventBus.MetadataEventBus;
import com.JoAbyssinia.audioService.interceptor.ErrorInterceptor;
import com.JoAbyssinia.audioService.interceptor.LogInterceptor;
import com.JoAbyssinia.audioService.interceptor.MetricsInterceptor;
import com.JoAbyssinia.audioService.repository.AudioMetadataRepository;
import com.JoAbyssinia.audioService.router.AudioRouter;
import com.JoAbyssinia.audioService.service.AudioService;
import com.JoAbyssinia.audioService.service.AudioServiceImpl;
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
    // kafka client
    KafkaClient kafkaClient = new KafkaClient(vertx, KafkaConfig.getConfigs());
    // initialise classes
    AudioService audioService = getAudioService(eventBus, kafkaClient);
    // start kafka consumer
    new KafkaConsumerService(kafkaClient, audioService).messageConsumer();
    // create event bus listener
    new MetadataEventBus(eventBus, audioService).evenBus();

    // interceptors
    LogInterceptor logInterceptor = new LogInterceptor();
    ErrorInterceptor errorInterceptor = new ErrorInterceptor();
    MetricsInterceptor metricsInterceptor = new MetricsInterceptor();

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

  private AudioService getAudioService(EventBus eventBus, KafkaClient kafka) {
    PostgresConfig postgresConfig = new PostgresConfig(vertx);
    AudioMetadataRepository audioMetadataRepository =
        new AudioMetadataRepository(postgresConfig.getPool());

    return new AudioServiceImpl(eventBus, audioMetadataRepository, kafka);
  }
}
