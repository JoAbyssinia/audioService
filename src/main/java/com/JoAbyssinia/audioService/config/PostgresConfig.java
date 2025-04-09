package com.JoAbyssinia.audioService.config;

import io.vertx.core.Vertx;
import io.vertx.pgclient.PgBuilder;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.SqlClient;

/**
 * @author Yohannes k Yimam
 */
public class PostgresConfig {

  private static final String HOST = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
  private static final String PORT = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
  private static final String USERNAME =
      System.getenv().getOrDefault("POSTGRES_USERNAME", "example");
  private static final String PASSWORD =
      System.getenv().getOrDefault("POSTGRES_PASSWORD", "example");
  private static final String DATABASE =
      System.getenv().getOrDefault("POSTGRES_DATABASE", "postgres");

  private final Vertx vertx;
  private volatile SqlClient pool;

  public PostgresConfig(Vertx vertx) {
    this.vertx = vertx;
  }

  public SqlClient getPool() {
    if (pool == null) {
      synchronized (this) {
        if (pool == null) { // Double-checked locking
          PgConnectOptions connectOptions =
              new PgConnectOptions()
                  .setHost(HOST)
                  .setPort(Integer.parseInt(PORT))
                  .setDatabase(DATABASE)
                  .setUser(USERNAME)
                  .setPassword(PASSWORD)
                  .setSsl(false)
                  .setConnectTimeout(5000);

          PoolOptions poolOptions = new PoolOptions().setMaxSize(10);

          pool =
              PgBuilder.pool()
                  .connectingTo(connectOptions.setPipeliningLimit(16))
                  .with(poolOptions)
                  .using(vertx)
                  .build();
        }
      }
    }

    return pool;
  }
}
