package com.JoAbyssinia.audioService.broker;

import com.JoAbyssinia.audioService.DTO.TrackRequestBrokerDTO;
import com.JoAbyssinia.audioService.entity.Audio;
import com.JoAbyssinia.audioService.service.AudioService;
import com.JoAbyssinia.audioService.util.Constant;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.consumer.KafkaConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yohannes k Yimam
 */
public class KafkaConsumerService {

  Logger logger = LoggerFactory.getLogger(KafkaConsumerService.class);
  private final KafkaClient kafkaClient;
  private final AudioService audioService;

  public KafkaConsumerService(KafkaClient kafkaClient, AudioService audioService) {
    this.kafkaClient = kafkaClient;
    this.audioService = audioService;
  }

  public void messageConsumer() {

    KafkaConsumer<String, TrackRequestBrokerDTO> consumer = kafkaClient.getConsumer();

    // Subscribe FIRST
    consumer
        .subscribe(Constant.AUDIO_PROCESSING_REQUEST)
        .onSuccess(
            v -> {
              logger.info("Subscribed to topic: {}", Constant.AUDIO_PROCESSING_REQUEST);

              consumer.handler(record -> processRecord(consumer, record));
            })
        .onFailure(
            err ->
                logger.error(
                    "Failed to subscribe to topic: {} due to {}",
                    Constant.AUDIO_PROCESSING_REQUEST,
                    err.getMessage()));
  }

  private void processRecord(
      KafkaConsumer<String, TrackRequestBrokerDTO> consumer,
      KafkaConsumerRecord<String, TrackRequestBrokerDTO> record) {
    try {
      logger.info("Processing message at offset {}: {}", record.offset(), record.value());

      // Create audio entity from the record
      Audio audio = new Audio();
      audio.setTrackId(record.value().trackId());
      audio.setTitle(record.value().title());
      audio.setArtistName(record.value().artistName());
      audio.setOriginalPath(record.value().rowAudioUrl());

      // Save audio entity - WAIT for completion before committing
      audioService
          .save(audio)
          .onSuccess(
              savedAudio -> {
                logger.info("Successfully saved audio for track id: {}", record.value().trackId());

                // COMMIT OFFSET - This ensures message is processed exactly once
                consumer
                    .commit()
                    .onSuccess(
                        v ->
                            logger.debug(
                                "Offset committed for track id: {}", record.value().trackId()))
                    .onFailure(
                        commitErr ->
                            logger.error(
                                "Failed to commit offset for track id: {}, will reprocess on restart",
                                record.value().trackId(),
                                commitErr));
              })
          .onFailure(
              err -> {
                logger.error(
                    "Failed to process audio request for track id: {} due to {}",
                    record.value().trackId(),
                    err.getMessage());

                // DON'T commit - message will be reprocessed
                // Optionally: send to DLQ, implement retry logic, etc.
              });

    } catch (Exception e) {
      logger.error("Error parsing message at offset {}: {}", record.offset(), e.getMessage(), e);
      // Don't commit - will retry
    }
  }
}
