package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
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
