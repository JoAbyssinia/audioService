package com.JoAbyssinia.audioService.config;

import java.util.Map;

/**
 * @author Yohannes k Yimam
 */
public class KafkaConfig {
  private static final String SERVER =
      System.getenv().getOrDefault("KAFKA_SERVER", "localhost:9092");
  private static final String SCHEMA_REGISTRY_URL =
      System.getenv().getOrDefault("SCHEMA_REGISTRY_URL", "http://localhost:8081");
  private static final String STRING_SERIALIZER =
      "org.apache.kafka.common.serialization.StringSerializer";
  private static final String JSON_SCHEMA_SERIALIZER =
      "io.confluent.kafka.serializers.json.KafkaJsonSchemaSerializer";
  private static final String STRING_DESERIALIZER =
      "org.apache.kafka.common.serialization.StringDeserializer";
  private static final String JSON_SCHEMA_DESERIALIZER =
      "io.confluent.kafka.serializers.json.KafkaJsonSchemaDeserializer";

  public static Map<String, String> getConfigs() {
    return Map.ofEntries(
        // Connection & Registry
        Map.entry("bootstrap.servers", SERVER),
        Map.entry("schema.registry.url", SCHEMA_REGISTRY_URL),

        // Serialization
        Map.entry("key.serializer", STRING_SERIALIZER),
        Map.entry("value.serializer", JSON_SCHEMA_SERIALIZER),
        Map.entry("key.deserializer", STRING_DESERIALIZER),
        Map.entry("value.deserializer", JSON_SCHEMA_DESERIALIZER),

        // Consumer/Producer Settings
        Map.entry("json.value.type", "com.JoAbyssinia.audioService.DTO.TrackRequestBrokerDTO"),
        Map.entry("group.id", "audio-processor-group"),
        Map.entry("auto.offset.reset", "earliest"),
        Map.entry("enable.auto.commit", "false"),
        Map.entry("auto.register.schemas", "true"),
        Map.entry("acks", "1"));
  }
}
