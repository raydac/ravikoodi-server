package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerItem {
  static final String[] ALL_NAMES = {
      "file",
  };

  private final PlaylistFileItem item;
}
