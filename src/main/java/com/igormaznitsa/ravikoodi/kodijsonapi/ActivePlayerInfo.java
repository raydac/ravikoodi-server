package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class ActivePlayerInfo {
  private final long playerid;
  private final String type;
}
