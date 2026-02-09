package com.JoAbyssinia.audioService.aws;

import com.JoAbyssinia.audioService.DTO.TrackResponseBrokerDTO;
import com.JoAbyssinia.audioService.entity.Audio;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

/**
 * @author Yohannes k Yimam
 */
@Slf4j
public class AwsSqsClient {
  private static final String QUEUE_NAME = "http://localhost:4566/000000000000/audio-status-queue";
  private final SqsAsyncClient sqsClient;

  private final ObjectMapper objectMapper = new ObjectMapper();

  public AwsSqsClient(SqsAsyncClient sqsClient) {
    this.sqsClient = sqsClient;
  }

  public Future<Void> sendMessage(Audio audio) throws JsonProcessingException {
    Promise<Void> promise = Promise.promise();

    if (audio == null) {
      log.error("Audio object is null. Cannot send message to SQS.");
      return Future.failedFuture(new IllegalArgumentException("Audio object cannot be null"));
    }

    // create response DTO to send to broker
    TrackResponseBrokerDTO responseBrokerDTO =
        new TrackResponseBrokerDTO(
            audio.getTrackId(), audio.getTitle(), audio.getArtistName(), audio.getStreamPath());

    // will replace it with on production.
    GetQueueUrlRequest queueUrlRequest = GetQueueUrlRequest.builder().queueName(QUEUE_NAME).build();
    SendMessageRequest request =
        SendMessageRequest.builder()
            .queueUrl(QUEUE_NAME)
            .messageBody(objectMapper.writeValueAsString(responseBrokerDTO))
            .build();
    sqsClient.sendMessage(request);

    return promise.future();
  }
}
