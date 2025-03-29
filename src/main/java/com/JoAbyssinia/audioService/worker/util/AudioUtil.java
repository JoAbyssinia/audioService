package com.JoAbyssinia.audioService.worker.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yohannes k Yimam
 */
public class AudioUtil {

  private static final Logger LOGGER = LoggerFactory.getLogger(AudioUtil.class);

  public static void transcodeToM3u8(File source, File outputFile) throws IOException {
    // build the commands
    List<String> commands = ffmpegCommands(source.getAbsolutePath());

    Process process = new ProcessBuilder().command(commands).directory(outputFile).start();

    new Thread(
      () -> {
        try (BufferedReader bufferedReader =
               new BufferedReader(new InputStreamReader(process.getInputStream()))) {
          String line;
          while ((line = bufferedReader.readLine()) != null) {
            LOGGER.info(line);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

    new Thread(
      () -> {
        try (BufferedReader bufferedReader =
               new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
          String line;
          while ((line = bufferedReader.readLine()) != null) {
            LOGGER.error(line);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      });

    try {
      if (process.waitFor() != 0) {
        throw new RuntimeException("Process exited with code " + process.exitValue());
      }
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  private static List<String> ffmpegCommands(String src) {

    List<String> commands = new ArrayList<>();
    commands.add("ffmpeg");
    commands.add("-i");
    commands.add(src);
    commands.add("-c:a");
    commands.add("aac");
    commands.add("-b:a");
    commands.add("128k");
    commands.add("-hls_time");
    commands.add("10");
    commands.add("-hls_playlist_type");
    commands.add("vod");
    commands.add("output.m3u8");

    return commands;
  }
}
