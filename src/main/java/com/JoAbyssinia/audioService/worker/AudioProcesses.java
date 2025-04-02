package com.JoAbyssinia.audioService.worker;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Yohannes k Yimam
 */
public class AudioProcesses {

  private static final Logger LOGGER = LoggerFactory.getLogger(AudioProcesses.class);

  public static Future<Void> transcodeToM3u8(File source, File outputFile) throws IOException {
    Promise<Void> promise = Promise.promise();

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
        promise.fail("Process exited with code " + process.exitValue());
        throw new RuntimeException("Process exited with code " + process.exitValue());
      } else {
        promise.complete();
      }

    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    return promise.future();
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
