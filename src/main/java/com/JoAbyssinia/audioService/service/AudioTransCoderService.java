package com.JoAbyssinia.audioService.service;

import io.vertx.core.Future;
import java.io.File;

/**
 * @author Yohannes k Yimam
 */
public interface AudioTransCoderService {

  Future<Void> uploadFolderToS3(File file, String s3FolderKey);

  Future<File> downloadFile(String fileName);

  Future<String> createFolderS3(String folderName);

  Future<String> generateResignedUrl(String fileName, long duration);
}
