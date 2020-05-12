package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerSpeedReq extends PlayerIdReq {
  @JsonProperty("speed")
  protected final long speed;
  
  public PlayerSpeedReq(final ActivePlayerInfo player, final long speed) {
    super(player);
    this.speed = speed;
  }
}
