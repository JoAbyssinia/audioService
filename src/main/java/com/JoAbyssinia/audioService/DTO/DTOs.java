package com.JoAbyssinia.audioService.DTO;

/**
 * @author Yohannes k Yimam
 */
public class DTOs {

  public record TrackDTO(
      Long id, Long trackId, String title, String artistName, String streamPath) {}
}
