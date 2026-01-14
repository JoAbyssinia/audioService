package com.JoAbyssinia.audioService.util;

import com.JoAbyssinia.audioService.DTO.DTOs;
import com.JoAbyssinia.audioService.entity.Audio;
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

  public static String listToTrackDTOJson(List<?> lists) throws JsonProcessingException {

    List<DTOs.TrackDTO> trackDTOs =
        lists.stream()
            .map(
                obj -> {
                  var audio = (Audio) obj;
                  return new DTOs.TrackDTO(
                      audio.getId(),
                      audio.getTitle(),
                      audio.getArtist(),
                      audio.getArtistId().orElse(null),
                      audio.getAlbum().orElse(null),
                      audio.getAlbumId().orElse(null),
                      audio.getAlbumArtUrl().orElse(null),
                      audio.getDuration(),
                      audio.getStreamPath());
                })
            .toList();

    return MAPPER.writeValueAsString(trackDTOs);
  }
}
