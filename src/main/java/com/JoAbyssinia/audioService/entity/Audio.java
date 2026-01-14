package com.JoAbyssinia.audioService.entity;

import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Yohannes k Yimam
 */
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Audio {
  private Long id;
  private String title;
  private String artist;
  private Optional<String> artistId;
  private Optional<String> album;
  private Optional<String> albumId;
  private Optional<String> albumArtUrl;
  private Long duration; // Duration in milliseconds
  private AudioStatus status;
  private String originalPath;
  private String streamPath;
}
