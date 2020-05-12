package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FileItem {
  
  @JsonProperty(value = "file")
  protected final String file;

  FileItem(final String filePath) {
    this.file = filePath;
  }
  
}
