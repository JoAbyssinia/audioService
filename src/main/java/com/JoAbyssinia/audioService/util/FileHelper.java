package com.JoAbyssinia.audioService.util;

import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import java.io.File;

/**
 * @author Yohannes k Yimam
 */
public class FileHelper {
  static Logger logger = LoggerFactory.getLogger(FileHelper.class);

  public static void deleteTempFolder(File file) {
    file.deleteOnExit();
  }
}
