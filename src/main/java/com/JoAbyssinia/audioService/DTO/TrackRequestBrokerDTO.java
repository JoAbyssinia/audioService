package com.JoAbyssinia.audioService.DTO;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @author Yohannes k Yimam
 */
public record TrackRequestBrokerDTO(
    @JsonProperty("trackId") Long trackId,
    @JsonProperty("title") String title,
    @JsonProperty("artistName") String artistName,
    @JsonProperty("rowAudioUrl") String rowAudioUrl) {}
