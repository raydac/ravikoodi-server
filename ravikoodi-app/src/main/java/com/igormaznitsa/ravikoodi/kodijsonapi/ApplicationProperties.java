package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApplicationProperties {

  static final String[] ALL_NAMES = {"name", "muted", "volume", "version"};

  private final String name;
  private final boolean muted;
  private final long volume;
  private final ApplicationVersion version;
}
