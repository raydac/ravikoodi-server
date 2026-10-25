package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class PlayerAudioStreamReq extends PlayerIdReq {
  @JsonProperty(value = "stream")
  protected final long stream;

  public PlayerAudioStreamReq(final ActivePlayerInfo player, final AudioStream audioStream) {
    super(player);
    this.stream = audioStream.getIndex();
  }
}
