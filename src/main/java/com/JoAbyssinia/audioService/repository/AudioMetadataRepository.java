package com.JoAbyssinia.audioService.repository;

import com.JoAbyssinia.audioService.entity.Audio;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.sqlclient.*;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yohannes k Yimam
 */
public class AudioMetadataRepository {

  private static final Logger logger = LoggerFactory.getLogger(AudioMetadataRepository.class);
  private final SqlClient pool;

  public AudioMetadataRepository(SqlClient pool) {
    this.pool = pool;
  }

  public Future<Audio> save(Audio audio) {
    Promise<Audio> promise = Promise.promise();

    // Modified query to return the generated ID
    String query =
        "INSERT INTO audio (title, status, originalPath, streamPath) VALUES ($1, $2, $3, $4) RETURNING id";
    Tuple params =
        Tuple.of(
            audio.getTitle(),
            audio.getStatus() == null ? "uploaded" : audio.getStatus(),
            audio.getOriginalPath(),
            audio.getStreamPath());

    pool.preparedQuery(query)
        .execute(params)
        .onSuccess(
            rows -> {
              // Get the generated ID and set it on the audio object
              if (rows.size() > 0) {
                Long id = rows.iterator().next().getLong("id");
                audio.setId(id);
              }
              promise.complete(audio);
            })
        .onFailure(
            err -> {
              logger.error("error on insert query " + err.getMessage());
              promise.fail(err); // Properly fail the promise so callers know about the error
            });

    return promise.future();
  }

  public Future<Audio> update(String newStatus, String streamPath, Long audioId) {
    if (audioId == null || newStatus == null || newStatus.isEmpty()) {
      return Future.failedFuture("Audio ID and status cannot be null or empty");
    }

    Promise<Audio> promise = Promise.promise();

    //  query's
    String selectQuery = "SELECT * FROM audio WHERE id = $1";
    String updateQuery = "UPDATE audio SET status = $1, steampath =$2 WHERE id = $3";

    // extract the audio
    pool.preparedQuery(selectQuery)
        .execute(Tuple.of(audioId))
        .compose(
            rows -> {
              if (rows.size() == 0) {
                return Future.failedFuture("Audio ID " + audioId + " not found");
              }

              Row row = rows.iterator().next();
              Audio audio = new Audio();
              audio.setId(row.getLong("id"));
              audio.setTitle(row.getString("title"));
              audio.setStatus(newStatus);
              audio.setOriginalPath(row.getString("originalpath"));
              audio.setStreamPath(row.getString("streampath"));

              // update
              return pool.preparedQuery(updateQuery)
                  .execute(Tuple.of(newStatus, streamPath, audioId))
                  .map(updateResult -> audio);
            })
        .onSuccess(promise::complete)
        .onFailure(
            err -> {
              logger.error("error on update query " + err.getMessage());
              promise.fail(err);
            });

    return promise.future();
  }

  public Future<List<Audio>> findAll() {
    String query = "SELECT * FROM audio WHERE status = 'completed'";

    return pool.query(query)
        .execute()
        .map(
            rows -> {
              List<Audio> audioList = new ArrayList<>();
              rows.forEach(
                  row -> {
                    Audio audio = new Audio();
                    audio.setId(row.getLong("id"));
                    audio.setTitle(row.getString("title"));
                    audio.setStatus(row.getString("status"));
                    audio.setOriginalPath(row.getString("originalpath"));
                    audio.setStreamPath(row.getString("streampath"));
                    audioList.add(audio);
                  });
              return audioList;
            });
  }
}
