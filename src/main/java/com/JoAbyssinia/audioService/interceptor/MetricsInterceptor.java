package com.JoAbyssinia.audioService.interceptor;

import io.vertx.ext.web.RoutingContext;

/**
 * @author Yohannes k Yimam
 */
public class MetricsInterceptor implements Interceptor {
  @Override
  public void interceptor(RoutingContext routingContext) {
    routingContext.next();
  }
}
