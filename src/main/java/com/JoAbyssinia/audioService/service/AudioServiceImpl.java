package com.JoAbyssinia.audioService.service;

import com.JoAbyssinia.audioService.entity.Audio;
import com.JoAbyssinia.audioService.repository.AudioRepository;
import io.vertx.core.Future;

import java.util.List;

/**
 * @author Yohannes k Yimam
 */
public class AudioServiceImpl implements AudioService {

  private AudioRepository audioRepository;

  public AudioServiceImpl(AudioRepository audioRepository) {
    this.audioRepository = audioRepository;
  }


  @Override
  public Future<Audio> save(Audio audio) {
    return audioRepository.save(audio);
  }

  @Override
  public Future<Audio> update(String newStatus, String streamPath, Long audioId) {
    return audioRepository.update(newStatus, streamPath, audioId);
  }

  @Override
  public Future<List<Audio>> findAll() {
    return audioRepository.findAll();
  }
}
