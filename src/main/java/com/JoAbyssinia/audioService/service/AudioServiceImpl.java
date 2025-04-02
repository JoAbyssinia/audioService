package com.JoAbyssinia.audioService.service;

import com.JoAbyssinia.audioService.entity.Audio;
import com.JoAbyssinia.audioService.repository.AudioMetadataRepository;
import com.JoAbyssinia.audioService.util.Constant;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import java.util.List;

/**
 * @author Yohannes k Yimam
 */
public class AudioServiceImpl implements AudioService {

  Logger logger = LoggerFactory.getLogger(AudioServiceImpl.class);

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
  public Future<Audio> save(Audio audio) {
    Promise<Audio> promise = Promise.promise();
    Future<Audio> audioSave = audioMetadataRepository.save(audio);

    audioSave
        .onSuccess(
            res -> {
              JsonObject json = new JsonObject();
              json.put("id", res.getId());
              json.put("title", res.getTitle());
              // publish save audio
              this.eventBus.send(Constant.AUDIO_TRANSCODE_ADDRESS, json.toString());

              promise.complete(res);
            })
        .onFailure(
            res -> {
              logger.error(res);
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
    return audioTransCoderService.generateResignedUrl(fileName);
  }

  @Override
  public Future<List<Audio>> findAll() {
    return audioMetadataRepository.findAll();
  }
}
