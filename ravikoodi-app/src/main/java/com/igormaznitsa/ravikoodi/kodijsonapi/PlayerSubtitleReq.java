package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerSubtitleReq extends PlayerIdReq {
  @JsonProperty(value = "subtitle")
  protected final long subtitle;
  
  @JsonProperty(value = "enable")
  protected final boolean enable;
  
  public PlayerSubtitleReq(final ActivePlayerInfo player, final Subtitle subtitle, final boolean enable) {
    super(player);
    this.subtitle = subtitle.getIndex();
    this.enable = enable;
  }
}
