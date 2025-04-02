package com.JoAbyssinia.audioService.service;

import com.JoAbyssinia.audioService.entity.Audio;
import io.vertx.core.Future;
import java.util.List;

/**
 * @author Yohannes k Yimam
 */
public interface AudioService {

  Future<Audio> save(Audio audio);

  Future<Audio> update(String newStatus, String streamPath, Long audioId);

  Future<List<Audio>> findAll();
}
