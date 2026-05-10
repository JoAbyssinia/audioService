package com.JoAbyssinia.audioService.util;

import java.io.File;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Yohannes k Yimam
 */
@Slf4j
public class FileHelper {
  public static void deleteTempFolder(File file) {
    file.deleteOnExit();
  }
}
