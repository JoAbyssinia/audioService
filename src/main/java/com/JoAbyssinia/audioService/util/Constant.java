package com.JoAbyssinia.audioService.util;

/**
 * @author Yohannes k Yimam
 */
public class Constant {

  // EventBus addresses
  public static final String AUDIO_TRANSCODE_ADDRESS = "audio.transcode";
  public static final String METADATA_UPDATE_ADDRESS = "metadata.update";

  // Kafka topics
  public static final String AUDIO_PROCESSING_REQUEST =
      System.getenv().getOrDefault("AUDIO_PROCESSING_REQUEST", "audio-processing-requests");
  public static final String PROCESSED_AUDIO_NOTIFICATIONS =
      System.getenv()
          .getOrDefault("PROCESSED_AUDIO_NOTIFICATIONS", "processed-audio-notifications");

  // s3 bucket name
  public static final String STREAM_AUDIO_FOLDERS = "stream-audio-files";
  public static final String ROW_AUDIO_FOLDER = "row-audio-files";
}
