package com.JoAbyssinia.audioService.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;

/**
 * @author Yohannes k Yimam
 */
public class JsonUtil {

  public static String mapToJson(Map<String, String> map) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(map);
  }

  public static String listToJson(List<?> lists) throws JsonProcessingException {
    ObjectMapper mapper = new ObjectMapper();
    return mapper.writeValueAsString(lists);
  }
}
