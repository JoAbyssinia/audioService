package com.JoAbyssinia.audioService.DTO;

/**
 * @author Yohannes k Yimam
 */
public class DTOs {

  public record TrackDTO(
      Long id,
      String title,
      String artist,
      String artistId,
      String album,
      String albumId,
      String albumArtUrl,
      Long duration,
      String streamPath) {}
}
