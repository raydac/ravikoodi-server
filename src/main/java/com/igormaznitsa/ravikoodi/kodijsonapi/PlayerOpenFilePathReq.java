package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerOpenFilePathReq {
  
  
  @JsonProperty(value="item") 
  protected final FileItem item;
  
  public PlayerOpenFilePathReq(final String filePath) {
    this.item = new FileItem(filePath);
  }
}
