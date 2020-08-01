package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerOpenFileItem {
  
  @JsonProperty(value = "file")
  protected final String file;

  PlayerOpenFileItem(final String filePath) {
    this.file = filePath;
  }
  
}
