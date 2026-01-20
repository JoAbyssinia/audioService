package com.JoAbyssinia.audioService.service;

import com.JoAbyssinia.audioService.broker.KafkaClient;
import com.JoAbyssinia.audioService.entity.Audio;
import com.JoAbyssinia.audioService.entity.AudioStatus;
import com.JoAbyssinia.audioService.repository.AudioMetadataRepository;
import com.JoAbyssinia.audioService.util.Constant;
import com.JoAbyssinia.audioService.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import java.util.List;

/**
 * @author Yohannes k Yimam
 */
public class AudioServiceImpl implements AudioService {

  Logger logger = LoggerFactory.getLogger(AudioServiceImpl.class);
  private RoutingContext context;

  private final AudioMetadataRepository audioMetadataRepository;
  private final EventBus eventBus;
  private final KafkaClient kafkaClient;

  public AudioServiceImpl(
      EventBus eventBus, AudioMetadataRepository audioMetadataRepository, KafkaClient kafkaClient) {
    this.audioMetadataRepository = audioMetadataRepository;
    this.eventBus = eventBus;
    this.kafkaClient = kafkaClient;
  }

  @Override
  public Future<String> save(Audio audio) {
    Promise<String> promise = Promise.promise();

    // set initial status to pending
    audio.setStatus(AudioStatus.PENDING);

    Future<Audio> audioSave = audioMetadataRepository.save(audio);

    audioSave
        .onSuccess(
            res -> {
              JsonObject json = new JsonObject();
              json.put("id", res.getId());
              json.put("title", res.getTitle());
              json.put("artistName", res.getArtistName());
              json.put("originalPath", res.getOriginalPath());
              // publish save audio
              this.eventBus.send(Constant.AUDIO_TRANSCODE_ADDRESS, json.toString());

              try {
                var response = JsonUtil.listToJson(List.of(res));
                promise.complete(response);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .onFailure(
            res -> {
              promise.fail(res);
            });

    return promise.future();
  }

  @Override
  public Future<Audio> update(AudioStatus newStatus, String streamPath, Long audioId) {
    Promise<Audio> promise = Promise.promise();
    audioMetadataRepository
        .update(newStatus, streamPath, audioId)
        .onSuccess(
            audio -> {
              promise.complete(audio);
              // send message to kafka
              kafkaClient
                  .writeToTopic(Constant.PROCESSED_AUDIO_NOTIFICATIONS, audio)
                  .onSuccess(
                      v ->
                          logger.info(
                              "audio processed message sent to kafka for track id: "
                                  + audio.getTrackId()))
                  .onFailure(
                      err ->
                          logger.error(
                              "failed to send audio processed message to kafka for track id: "
                                  + audio.getTrackId()
                                  + " due to "
                                  + err.getMessage()));
            });

    return promise.future();
  }

  @Override
  public RoutingContext getContext() {
    return context;
  }
}
