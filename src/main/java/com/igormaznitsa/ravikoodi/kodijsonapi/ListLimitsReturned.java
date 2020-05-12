package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
public class ListLimitsReturned {
  @JsonProperty(value = "total",required = true)
  private final long total;
  @JsonProperty(value = "start", required = false, defaultValue = "0")
  private final long start;
  @JsonProperty(value = "end", required = false, defaultValue = "-1")
  private final long end;
}
