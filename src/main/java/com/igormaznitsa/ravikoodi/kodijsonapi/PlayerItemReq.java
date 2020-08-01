package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerItemReq extends PlayerIdReq {
  
  @JsonProperty(value="properties")
  protected final String [] properties;
  
  public PlayerItemReq(final ActivePlayerInfo player, final String ... properties) {
    super(player);
    this.properties = properties;
  }
}
