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
  private String title;
  private String status;
  private String originalPath;
  private String streamPath;
}
