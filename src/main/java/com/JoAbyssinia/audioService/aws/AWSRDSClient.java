package com.JoAbyssinia.audioService.aws;

import com.JoAbyssinia.audioService.config.AWSConfig;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.SslMode;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;
import lombok.extern.slf4j.Slf4j;

/**
 * Manages the Vert.x PostgreSQL connection pool for AWS RDS.
 *
 * <p>Local dev (Floci): set DB_SSL_ENABLED=false (default) — connects with a static password
 * against the Floci-emulated RDS instance on localhost:7001.
 *
 * <p>Production (AWS RDS): set DB_SSL_ENABLED=true — connects with SSL and an IAM auth token
 * generated via {@link AWSConfig#generateRdsIamToken} (requires the ECS task role to have
 * rds:connect permission and the DB user to have the rds_iam role).
 *
 * @author Yohannes k Yimam
 */
@Slf4j
public class AWSRDSClient {

  private static final String DATABASE_HOST =
      System.getenv().getOrDefault("DATABASE_HOST", "localhost");
  private static final int DATABASE_PORT =
      Integer.parseInt(System.getenv().getOrDefault("DATABASE_PORT", "7001"));
  private static final String DATABASE_NAME =
      System.getenv().getOrDefault("DATABASE_NAME", "zemadb");

  private static final String DATABASE_USERNAME =
      System.getenv().getOrDefault("DB_USERNAME", "postgres");
  private static final String DATABASE_PASSWORD =
      System.getenv().getOrDefault("DB_PASSWORD", "c14db4b623ce8a7333f9f28b67c99258");

  // Set DB_SSL_ENABLED=true in production (AWS RDS requires SSL + IAM auth).
  // Leave unset / false locally — Floci RDS does not require SSL.
  private static final boolean SSL_ENABLED =
      Boolean.parseBoolean(System.getenv().getOrDefault("DB_SSL_ENABLED", "false"));

  private final Vertx vertx;
  private volatile SqlClient pool;

  public AWSRDSClient(Vertx vertx) {
    this.vertx = vertx;
  }

  public SqlClient getPool() {
    if (pool == null) {
      synchronized (this) {
        if (pool == null) {
          PgConnectOptions connectOptions = buildConnectOptions();
          PoolOptions poolOptions = new PoolOptions().setMaxSize(10);

          pool =
              PgBuilder.pool()
                  .connectingTo(connectOptions.setPipeliningLimit(16))
                  .with(poolOptions)
                  .using(vertx)
                  .build();

          // Create the audio table if it does not already exist
          pool.preparedQuery(
                  """
                  CREATE TABLE IF NOT EXISTS audio (
                       id           BIGSERIAL     NOT NULL PRIMARY KEY,
                       trackid      BIGSERIAL,
                       title        VARCHAR(50),
                       artistName   VARCHAR(50),
                       status       VARCHAR(25),
                       originalPath VARCHAR(250),
                       streamPath   VARCHAR(250),
                       last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                       version      BIGINT
                  );
                  """)
              .execute()
              .onSuccess(res -> log.info("Audio table is ready"))
              .onFailure(err -> log.error("Error creating audio table: {}", err.getMessage()));
        }
      }
    }
    return pool;
  }

  private PgConnectOptions buildConnectOptions() {
    if (SSL_ENABLED) {
      return buildWithSslAndIamAuth();
    }
    // Local / Floci — plain password, no SSL
    return new PgConnectOptions()
        .setHost(DATABASE_HOST)
        .setPort(DATABASE_PORT)
        .setDatabase(DATABASE_NAME)
        .setUser(DATABASE_USERNAME)
        .setPassword(DATABASE_PASSWORD);
  }

  /**
   * Production path: SSL required + IAM authentication token as password.
   *
   * <p>The token is valid for 15 minutes. Because the pool holds connections open, existing
   * connections remain valid after expiry — only reconnects require a fresh token. For full
   * token-refresh support, recreate the pool on a 14-minute schedule.
   */
  private PgConnectOptions buildWithSslAndIamAuth() {
    String iamToken =
        AWSConfig.generateRdsIamToken(DATABASE_HOST, DATABASE_PORT, DATABASE_USERNAME);
    log.info(
        "Using RDS IAM auth token for user '{}' at {}:{}",
        DATABASE_USERNAME,
        DATABASE_HOST,
        DATABASE_PORT);
    return new PgConnectOptions()
        .setHost(DATABASE_HOST)
        .setPort(DATABASE_PORT)
        .setDatabase(DATABASE_NAME)
        .setUser(DATABASE_USERNAME)
        .setPassword(iamToken) // IAM token used as the PostgreSQL password
        .setSslMode(SslMode.REQUIRE);
  }

  public void closePool() {
    if (pool != null) {
      pool.close();
    }
  }
}
