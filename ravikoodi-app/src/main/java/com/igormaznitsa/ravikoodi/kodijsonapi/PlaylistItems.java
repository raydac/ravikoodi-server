package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaylistItems {
  @JsonProperty(value = "items", required = true)
  private final PlaylistFileItem [] items;
  @JsonProperty(value = "limits", required = true)
  private final ListLimitsReturned limits;
}
