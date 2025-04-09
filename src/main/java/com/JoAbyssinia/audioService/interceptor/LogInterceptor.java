package com.JoAbyssinia.audioService.interceptor;

import com.JoAbyssinia.audioService.util.JsonUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.vertx.ext.web.RoutingContext;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Yohannes k Yimam
 */
public class LogInterceptor implements Interceptor {

  @Override
  public void interceptor(RoutingContext routingContext) {
    long startTime = System.currentTimeMillis();

    Map<String, String> logsMap = new HashMap<>();

    routingContext.put("logs", logsMap);

    logsMap.put("startTime", String.valueOf(startTime));
    logsMap.put("requestUrl", routingContext.request().uri());
    logsMap.put("method", routingContext.request().method().toString());

    routingContext.addBodyEndHandler(
        bodyEndHandler -> {
          long duration = System.currentTimeMillis() - startTime;
          int statusCode = routingContext.response().getStatusCode();
          logsMap.put("duration", String.valueOf(duration));
          logsMap.put("durationTime", String.valueOf(duration / 1000));
          logsMap.put("statusCode", String.valueOf(statusCode));

          try {
            System.out.println(JsonUtil.mapToJson(logsMap));
          } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
          }
        });
    routingContext.next();
  }
}
