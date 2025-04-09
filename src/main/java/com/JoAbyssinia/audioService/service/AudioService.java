package com.JoAbyssinia.audioService.service;

import com.JoAbyssinia.audioService.entity.Audio;
import io.vertx.core.Future;
import io.vertx.ext.web.RoutingContext;

/**
 * @author Yohannes k Yimam
 */
public interface AudioService {

  Future<String> save(Audio audio);

  Future<Audio> update(String newStatus, String streamPath, Long audioId);

  Future<String> generatePresignedUrl(String fileName);

  Future<String> findAll();

  AudioService setContext(RoutingContext context);

  RoutingContext getContext();
}
