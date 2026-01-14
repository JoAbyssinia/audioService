package com.JoAbyssinia.audioService.repository;

import com.JoAbyssinia.audioService.entity.Audio;
import com.JoAbyssinia.audioService.entity.AudioStatus;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.sqlclient.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        "INSERT INTO audio (title, artist, duration, status, originalPath, streamPath) VALUES ($1, $2, $3, $4, $5, $6) RETURNING id";
    Tuple params =
        Tuple.of(
            audio.getTitle(),
            audio.getArtist(),
            audio.getDuration(),
            audio.getStatus().toString(),
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

  public Future<Audio> update(AudioStatus newStatus, String streamPath, Long audioId) {
    if (audioId == null || newStatus == null) {
      return Future.failedFuture("Audio ID and status cannot be null or empty");
    }

    Promise<Audio> promise = Promise.promise();

    //  query's
    String selectQuery = "SELECT * FROM audio WHERE id = $1";
    String updateQuery = "UPDATE audio SET status = $1, streampath =$2 WHERE id = $3";

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
              audio.setArtist(row.getString("artist"));
              audio.setStatus(AudioStatus.valueOf(row.getString("status")));
              audio.setDuration(row.getLong("duration"));
              audio.setOriginalPath(row.getString("originalpath"));
              audio.setStreamPath(streamPath);

              // update
              return pool.preparedQuery(updateQuery)
                  .execute(Tuple.of(newStatus.toString(), streamPath, audioId))
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
    String audioStatus = AudioStatus.COMPLETED.toString();
    String query = "SELECT * FROM audio WHERE status = '" + audioStatus + "'";

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
                    audio.setArtist(row.getString("artist"));
                    audio.setArtistId(Optional.ofNullable(row.getString("artistid")));
                    audio.setAlbum(Optional.ofNullable(row.getString("album")));
                    audio.setAlbumId(Optional.ofNullable(row.getString("albumid")));
                    audio.setAlbumArtUrl(Optional.ofNullable(row.getString("albumarturl")));
                    audio.setDuration(row.getLong("duration"));
                    audio.setStatus(AudioStatus.valueOf(row.getString("status")));
                    audio.setOriginalPath(row.getString("originalpath"));
                    audio.setStreamPath(row.getString("streampath"));
                    audioList.add(audio);
                  });
              return audioList;
            });
  }
}
