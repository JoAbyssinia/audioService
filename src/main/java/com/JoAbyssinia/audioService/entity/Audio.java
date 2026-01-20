package com.JoAbyssinia.audioService.entity;

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
  private Long trackId;
  private String title;
  private String artistName;
  private AudioStatus status;
  private String originalPath;
  private String streamPath;
}
