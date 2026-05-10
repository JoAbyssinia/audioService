package com.JoAbyssinia.audioService.config;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yohannes k Yimam
 */
@Slf4j
public class PostgresConfig {

  private static final String DATABASE_HOST =
      System.getenv().getOrDefault("DATABASE_HOST", "localhost");
  private static final String DATABASE_PORT = System.getenv().getOrDefault("DATABASE_PORT", "5432");
  private static final String DATABASE_NAME =
      System.getenv().getOrDefault("DATABASE_NAME", "postgres");

  // DATABASE_SECRET is injected by ECS from AWS Secrets Manager (RDS secret JSON)
  // containing "username" and "password" fields
  private static final String DATABASE_USERNAME =
      System.getenv().getOrDefault("DB_USERNAME", "username");
  private static final String DATABASE_PASSWORD =
      System.getenv().getOrDefault("DB_PASSWORD", "password");

  private final Vertx vertx;
  private volatile SqlClient pool;

  public PostgresConfig(Vertx vertx) {
    this.vertx = vertx;
  }

  public SqlClient getPool() {
    if (pool == null) {
      synchronized (this) {
        if (pool == null) { // Double-checked locking
          PgConnectOptions connectOptions = buildConnectOptions();

          PoolOptions poolOptions = new PoolOptions().setMaxSize(10);

          pool =
              PgBuilder.pool()
                  .connectingTo(connectOptions.setPipeliningLimit(16))
                  .with(poolOptions)
                  .using(vertx)
                  .build();
          // create the table if it doesn't exist
          String query =
              """
            CREATE TABLE IF NOT EXISTS audio (
                 id BIGSERIAL NOT NULL PRIMARY KEY,
                 trackid BIGSERIAL,
                 title VARCHAR(50),
                 artistName VARCHAR(50),
                 status VARCHAR(25),
                 originalPath VARCHAR(250),
                 streamPath VARCHAR(250),
                 last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                 version BIGINT
            );""";
          pool.preparedQuery(query)
              .execute()
              .onSuccess(res -> log.info("Audio table is ready"))
              .onFailure(err -> log.error("Error creating audio table: {}", err.getMessage()));
        }
      }
    }

    return pool;
  }

  private PgConnectOptions buildConnectOptions() {
    if (!DATABASE_USERNAME.isBlank() && !DATABASE_PASSWORD.isBlank()) {
      return buildFromSecret();
    }
    // Fallback to direct env vars for local development
    return new PgConnectOptions()
        .setHost(DATABASE_HOST)
        .setPort(Integer.parseInt(DATABASE_PORT))
        .setDatabase(DATABASE_NAME)
        .setUser(DATABASE_USERNAME)
        .setPassword(DATABASE_PASSWORD);
  }

  private PgConnectOptions buildFromSecret() {
    return new PgConnectOptions()
        .setHost(DATABASE_HOST)
        .setPort(Integer.parseInt(DATABASE_PORT))
        .setDatabase(DATABASE_NAME)
        .setUser(DATABASE_USERNAME)
        .setPassword(DATABASE_PASSWORD)
        .setSslMode(SslMode.REQUIRE);
  }

  public void closePool() {
    if (pool != null) {
      pool.close();
    }
  }
}
