package com.JoAbyssinia.audioService.verticle;

import com.JoAbyssinia.audioService.config.PostgresConfig;
import com.JoAbyssinia.audioService.eventBus.MetadataEventBus;
import com.JoAbyssinia.audioService.repository.AudioMetadataRepository;
import com.JoAbyssinia.audioService.router.AudioRouter;
import com.JoAbyssinia.audioService.service.AudioService;
import com.JoAbyssinia.audioService.service.AudioServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
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
    //   initialise classes
    PostgresConfig postgresConfig = new PostgresConfig(vertx);
    AudioMetadataRepository audioMetadataRepository =
        new AudioMetadataRepository(postgresConfig.getPool());
    AudioService audioService = new AudioServiceImpl(eventBus, audioMetadataRepository);

    // create event bus listener
    new MetadataEventBus(eventBus, audioService).evenBus();

    // router
    Router router = new AudioRouter(vertx, audioService).getRouter();
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(8888)
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
