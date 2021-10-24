package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ApplicationPropertiesReq {

  @JsonProperty(value = "properties")
  private final String[] properties;

  public ApplicationPropertiesReq(final String... properties) {
    this.properties = properties;
  }
}
