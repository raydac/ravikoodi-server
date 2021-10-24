package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
public class PlayerIdReq {
  @JsonProperty("playerid")
  protected final long playerid;
  
  public PlayerIdReq(final ActivePlayerInfo player) {
    this.playerid = player.getPlayerid();
  }
}
