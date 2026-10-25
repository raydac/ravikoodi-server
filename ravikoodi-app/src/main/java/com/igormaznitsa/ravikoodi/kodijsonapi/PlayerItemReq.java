package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
public class PlayerItemReq extends PlayerIdReq {

  @JsonProperty(value = "properties")
  protected final String[] properties;

  public PlayerItemReq(final ActivePlayerInfo player, final String... properties) {
    super(player);
    this.properties = properties;
  }
}
