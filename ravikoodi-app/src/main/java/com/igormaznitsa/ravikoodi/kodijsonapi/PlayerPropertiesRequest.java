package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerPropertiesRequest extends PlayerIdReq {
  
  @JsonProperty(value = "properties")
  private final String[] properties;

  public PlayerPropertiesRequest(final ActivePlayerInfo player, final String... properties) {
    super(player);
    this.properties = properties;
  }
  
}
