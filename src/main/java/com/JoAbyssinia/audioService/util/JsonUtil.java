package com.JoAbyssinia.audioService.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.util.List;
import java.util.Map;

/**
 * @author Yohannes k Yimam
 */
public class JsonUtil {

  private static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new Jdk8Module());

  public static String mapToJson(Map<String, String> map) throws JsonProcessingException {
    return MAPPER.writeValueAsString(map);
  }

  public static String listToJson(List<?> lists) throws JsonProcessingException {
    return MAPPER.writeValueAsString(lists);
  }
}
