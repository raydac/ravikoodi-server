package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerItem {
  public static final String [] ALL_NAMES = {
    "file",
  };

  private final PlaylistItem item;
}
