package com.JoAbyssinia.audioService.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;

/**
 * @author Yohannes k Yimam
 */
@Slf4j
public class FileHelper {
  public static void deleteTempFolder(File file) {
    file.deleteOnExit();
  }
}
