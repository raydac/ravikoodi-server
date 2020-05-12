package com.igormaznitsa.ravikoodi.kodijsonapi;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
public class ActivePlayerInfo {
  private final long playerid;
  private final String type;
}
