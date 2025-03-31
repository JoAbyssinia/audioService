package com.JoAbyssinia.audioService.verticle;

import com.JoAbyssinia.audioService.config.PostgresConfig;
import com.JoAbyssinia.audioService.repository.AudioRepository;
import com.JoAbyssinia.audioService.router.AudioRouter;
import com.JoAbyssinia.audioService.service.AudioServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.Router;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yohannes k Yimam
 */
@NoArgsConstructor
public class MetadataVerticle extends AbstractVerticle {

  private final Logger logger = LoggerFactory.getLogger(MetadataVerticle.class);

  @Override
  public void start() throws Exception {

    PostgresConfig postgresConfig = new PostgresConfig(vertx);
    AudioRepository audioRepository = new AudioRepository(postgresConfig.getPool());
    AudioServiceImpl audioService = new AudioServiceImpl(audioRepository);

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
