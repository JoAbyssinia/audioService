package com.JoAbyssinia.audioService.service;

import com.JoAbyssinia.audioService.entity.Audio;
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
import java.util.Map;

/**
 * @author Yohannes k Yimam
 */
public class AudioServiceImpl implements AudioService {

  Logger logger = LoggerFactory.getLogger(AudioServiceImpl.class);
  private RoutingContext context;

  private final AudioMetadataRepository audioMetadataRepository;
  private final EventBus eventBus;
  private final AudioTransCoderService audioTransCoderService;

  public AudioServiceImpl(
      EventBus eventBus,
      AudioMetadataRepository audioMetadataRepository,
      AudioTransCoderService audioTransCoderService) {
    this.audioMetadataRepository = audioMetadataRepository;
    this.eventBus = eventBus;
    this.audioTransCoderService = audioTransCoderService;
  }

  @Override
  public Future<String> save(Audio audio) {
    Promise<String> promise = Promise.promise();
    Map<String, String> logMap = getContext().get("logs");
    Future<Audio> audioSave = audioMetadataRepository.save(audio);

    audioSave
        .onSuccess(
            res -> {
              JsonObject json = new JsonObject();
              json.put("id", res.getId());
              json.put("title", res.getTitle());
              // publish save audio
              this.eventBus.send(Constant.AUDIO_TRANSCODE_ADDRESS, json.toString());

              try {
                var response = JsonUtil.listToJson(List.of(res));
                logMap.put("audio", response);
                promise.complete(response);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
            })
        .onFailure(
            res -> {
              logMap.put("error", res.toString());
              promise.fail(res);
            });

    return promise.future();
  }

  @Override
  public Future<Audio> update(String newStatus, String streamPath, Long audioId) {
    return audioMetadataRepository.update(newStatus, streamPath, audioId);
  }

  @Override
  public Future<String> generatePresignedUrl(String fileName) {
    Promise<String> promise = Promise.promise();
    Map<String, String> logMap = getContext().get("logs");

    audioTransCoderService
        .generateResignedUrl(fileName)
        .onSuccess(
            result -> {
              JsonObject resignedUrlJson = new JsonObject();
              resignedUrlJson.put("presignedUrl", result);

              logMap.put("presigned-url", resignedUrlJson.toString());
              promise.complete(resignedUrlJson.toString());
            })
        .onFailure(
            err -> {
              logMap.put("error", err.getMessage());
              promise.fail(err);
            });

    return promise.future();
  }

  @Override
  public Future<String> findAll() {
    Promise<String> promise = Promise.promise();
    Map<String, String> logMap = getContext().get("logs");

    var audios = audioMetadataRepository.findAll();
    audios.onSuccess(
        result -> {
          try {
            var json = JsonUtil.listToJson(result);
            logMap.put("audios", json);
            promise.complete(json);
          } catch (JsonProcessingException e) {
            logMap.put("error", e.getMessage());
            promise.fail(e);
          }
        });
    return promise.future();
  }

  @Override
  public AudioService setContext(RoutingContext context) {
    this.context = context;
    return this;
  }

  @Override
  public RoutingContext getContext() {
    return context;
  }
}
