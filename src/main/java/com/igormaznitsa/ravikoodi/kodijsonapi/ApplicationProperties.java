package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor(force = true)
public class ApplicationProperties {

  public static final String[] ALL_NAMES = {"name", "muted", "volume", "version"};

  private final String name;
  private final boolean muted;
  private final long volume;
  private final ApplicationVersion version;
}
