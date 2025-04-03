package com.JoAbyssinia.audioService.verticle;

import com.JoAbyssinia.audioService.aws.AwsS3Client;
import com.JoAbyssinia.audioService.config.S3Config;
import com.JoAbyssinia.audioService.eventBus.AudioTranscoderEventBus;
import com.JoAbyssinia.audioService.service.AudioTransCoderService;
import com.JoAbyssinia.audioService.service.AudioTransCoderServiceImpl;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import software.amazon.awssdk.services.s3.S3AsyncClient;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * @author Yohannes k Yimam
 */
public class AudioTranscodeWorkerVerticle extends AbstractVerticle {

  Logger logger = LoggerFactory.getLogger(AudioTranscodeWorkerVerticle.class);

  @Override
  public void start() throws Exception {
    // event bus
    EventBus eventBus = vertx.eventBus();
    // s3 configs
    S3AsyncClient s3Client = S3Config.getS3AsyncClient();
    S3Presigner s3Presigner = S3Config.getS3Presigner();
    // aws client
    AwsS3Client awsS3Client = new AwsS3Client(vertx, s3Client, s3Presigner);
    //    awsS3Client.listFiles();
    // audio transcoder service
    AudioTransCoderService audioTransCoderService = new AudioTransCoderServiceImpl(awsS3Client);
    // create event bus listener
    new AudioTranscoderEventBus(vertx, eventBus, audioTransCoderService).eventBus();

    System.out.println("AudioSegmentationWorkerVerticle started");
  }
}
