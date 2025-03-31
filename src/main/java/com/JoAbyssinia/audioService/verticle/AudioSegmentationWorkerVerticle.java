package com.JoAbyssinia.audioService.verticle;

import com.JoAbyssinia.audioService.aws.S3ClientService;
import com.JoAbyssinia.audioService.config.S3Config;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @author Yohannes k Yimam
 */
public class AudioSegmentationWorkerVerticle extends AbstractVerticle {

  Logger logger = LoggerFactory.getLogger(AudioSegmentationWorkerVerticle.class);

  @Override
  public void start() throws Exception {

    S3AsyncClient s3Client = S3Config.getS3AsyncClient();
    S3Presigner s3Presigner = S3Config.getS3Presigner();

    S3ClientService s3ClientService = new S3ClientService(vertx, s3Client, s3Presigner);
    s3ClientService.listFiles();

    System.out.println("AudioSegmentationWorkerVerticle started");
  }
}
