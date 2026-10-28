package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaylistProperties {
  static final String[] ALL_NAMES = {"size", "type"};

  public final long size;
  public final String type;

}
