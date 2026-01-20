package com.JoAbyssinia.audioService.broker;

import com.JoAbyssinia.audioService.DTO.TrackRequestBrokerDTO;
import com.JoAbyssinia.audioService.DTO.TrackResponseBrokerDTO;
import com.JoAbyssinia.audioService.entity.Audio;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.kafka.client.consumer.KafkaConsumer;
import io.vertx.kafka.client.producer.KafkaProducer;
import io.vertx.kafka.client.producer.KafkaProducerRecord;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Yohannes k Yimam
 */
public class KafkaClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(KafkaClient.class);
  private final Vertx vertx;
  private final Map<String, String> config;
  private volatile KafkaProducer<String, TrackResponseBrokerDTO> producer;
  private volatile KafkaConsumer<String, TrackRequestBrokerDTO> consumer;

  public KafkaClient(Vertx vertx, Map<String, String> config) {
    this.vertx = vertx;
    this.config = config;
  }

  public synchronized KafkaProducer<String, TrackResponseBrokerDTO> getProducer() {
    if (producer == null) {
      try {
        producer = KafkaProducer.create(vertx, config);
      } catch (Exception e) {
        LOGGER.error("Error creating producer for KafkaClient", e);
        throw new RuntimeException(e);
      }
    }
    return producer;
  }

  public synchronized KafkaConsumer<String, TrackRequestBrokerDTO> getConsumer() {
    if (consumer == null) {
      try {
        consumer = KafkaConsumer.create(vertx, config);
      } catch (Exception e) {
        LOGGER.error("Error creating consumer for KafkaClient", e);
        throw new RuntimeException(e);
      }
    }
    return consumer;
  }

  public Future<Void> close() {
    Future<Void> pFuture = producer != null ? producer.close() : Future.succeededFuture();
    Future<Void> cFuture = consumer != null ? consumer.close() : Future.succeededFuture();
    return Future.all(pFuture, cFuture).mapEmpty();
  }

  public Future<Void> writeToTopic(String topic, Audio audio) {
    // create response DTO to send to broker
    TrackResponseBrokerDTO responseBrokerDTO =
        new TrackResponseBrokerDTO(
            audio.getTrackId(), audio.getTitle(), audio.getArtistName(), audio.getStreamPath());

    KafkaProducerRecord<String, TrackResponseBrokerDTO> record =
        KafkaProducerRecord.create(topic, responseBrokerDTO);

    return getProducer()
        .write(record)
        .onSuccess(v -> LOGGER.info("Message sent to topic: {}", topic))
        .onFailure(
            err ->
                LOGGER.error(
                    "Failed to send message to topic: {} due to {}", topic, err.getMessage()));
  }
}
