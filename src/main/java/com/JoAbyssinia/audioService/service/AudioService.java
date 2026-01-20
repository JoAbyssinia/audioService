package com.JoAbyssinia.audioService.service;

import com.JoAbyssinia.audioService.entity.Audio;
import com.JoAbyssinia.audioService.entity.AudioStatus;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Yohannes k Yimam
 */
public interface AudioService {

  Future<String> save(Audio audio);

  Future<Audio> update(AudioStatus status, String streamPath, Long audioId);

  RoutingContext getContext();
}
