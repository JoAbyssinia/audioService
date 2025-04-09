package com.JoAbyssinia.audioService.interceptor;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Yohannes k Yimam
 */
public interface Interceptor {
  void interceptor(RoutingContext routingContext);
}
