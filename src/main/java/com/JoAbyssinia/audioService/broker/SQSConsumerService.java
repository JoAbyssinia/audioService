package com.JoAbyssinia.audioService.broker;

import com.JoAbyssinia.audioService.entity.Audio;
import com.JoAbyssinia.audioService.service.AudioService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

/**
 * @author Yohannes k Yimam
 */
@Slf4j
public class SQSConsumerService {

  private static final String INPUT_QUEUE_URL =
      System.getenv()
          .getOrDefault("INPUT_QUEUE_URL", "http://localhost:4566/000000000000/audio-ingest-queue");
  private final SqsAsyncClient sqsAsyncClient;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private final AudioService audioService;
  private final Vertx vertx;

  public SQSConsumerService(Vertx vertx, AudioService audioService, SqsAsyncClient sqsAsyncClient) {
    this.vertx = vertx;
    this.audioService = audioService;
    this.sqsAsyncClient = sqsAsyncClient;
  }

  public void consumeMessages() {
    log.info("Starting SQS Long Polling on: " + INPUT_QUEUE_URL);
    ReceiveMessageRequest receiveMessageRequest =
        ReceiveMessageRequest.builder()
            .queueUrl(INPUT_QUEUE_URL)
            .maxNumberOfMessages(10)
            .waitTimeSeconds(20) // Enable long polling
            .build();

    sqsAsyncClient
        .receiveMessage(receiveMessageRequest)
        .whenComplete(
            (receiveMessageResponse, throwable) -> {
              if (throwable != null) {
                log.error("Error receiving messages from SQS: ", throwable);
                vertx.setTimer(5000L, t -> consumeMessages());
              } else {
                if (receiveMessageResponse.hasMessages()) {
                  receiveMessageResponse
                      .messages()
                      .forEach(
                          message -> {
                            log.info("Received message: {}", message.body());

                            try {
                              JsonNode jsonNode = objectMapper.readTree(message.body());

                              if (jsonNode.has("rowAudioUrl") && jsonNode.has("trackId")) {

                                Long trackId = jsonNode.get("trackId").asLong();
                                String rowAudioUrl = jsonNode.get("rowAudioUrl").asText();
                                String title = jsonNode.get("title").asText();
                                String artistName = jsonNode.get("artistName").asText();

                                // Create an audio entity from the record
                                Audio audio = new Audio();

                                audio.setTrackId(trackId);
                                audio.setTitle(title);
                                audio.setArtistName(artistName);
                                audio.setOriginalPath(rowAudioUrl);

                                // Save audio entity - WAIT for completion before committing
                                audioService
                                    .save(audio)
                                    .onSuccess(
                                        savedAudio -> {
                                          log.info(
                                              "Successfully saved audio for track id: {}", trackId);
                                          deleteMessageFromQueue(message.receiptHandle());
                                        })
                                    .onFailure(
                                        err ->
                                            log.error(
                                                "Failed to process audio request for track id: {} due to {}",
                                                trackId,
                                                err.getMessage()));
                              } else {
                                log.warn("Message missing required fields: {}", message.body());
                              }

                              vertx.runOnContext(v -> consumeMessages());

                            } catch (JsonProcessingException e) {
                              throw new RuntimeException(e);
                            }
                          });
                }
              }
            });
  }

  private void deleteMessageFromQueue(String receiptHandle) {
    sqsAsyncClient.deleteMessage(
        DeleteMessageRequest.builder()
            .queueUrl(INPUT_QUEUE_URL)
            .receiptHandle(receiptHandle)
            .build());
  }
}
