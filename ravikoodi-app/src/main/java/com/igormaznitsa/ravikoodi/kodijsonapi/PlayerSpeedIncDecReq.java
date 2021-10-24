package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerSpeedIncDecReq extends PlayerIdReq {
  public enum Direction {
      INCREMENT("increment"),
      DECREMENT("decrement");
      
      private final String text;

      private Direction(final String text) {
        this.text = text;
      }
  }
    
  @JsonProperty("speed")
  protected final String speed;
  
  public PlayerSpeedIncDecReq(final ActivePlayerInfo player, final Direction direction) {
    super(player);
    this.speed = direction.text;
  }
}
