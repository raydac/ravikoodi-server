package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ListLimitsReturned {
  private final long total;
  private final long start;
  private final long end;

  @JsonCreator
  public ListLimitsReturned(
      @JsonProperty(value = "total", required = true) final long total,
      @JsonProperty(value = "start", required = false) final Long start,
      @JsonProperty(value = "end", required = false) final Long end) {
    this.total = total;
    this.start = start == null ? 0L : start;
    this.end = end == null ? -1L : end;
  }
}
