package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerSeekPercentageReq extends PlayerIdReq {

  @JsonProperty("value")
  private final double value;

  public PlayerSeekPercentageReq(final ActivePlayerInfo player, final double percentage) {
    super(player);
    this.value = percentage;
  }

  public double getValue() {
    return this.value;
  }
}
