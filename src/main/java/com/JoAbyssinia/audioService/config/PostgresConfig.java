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

  private static final String host = "localhost";
  private static final int port = 5432;
  private static final String username = "example";
  private static final String password = "example";
  private static final String database = "postgres";

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
                  .setHost(host)
                  .setPort(port)
                  .setDatabase(database)
                  .setUser(username)
                  .setPassword(password)
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
