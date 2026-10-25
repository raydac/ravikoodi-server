package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class PlayerSpeedReq extends PlayerIdReq {
  @JsonProperty("speed")
  protected final long speed;
  
  public PlayerSpeedReq(final ActivePlayerInfo player, final long speed) {
    super(player);
    this.speed = speed;
  }
}
