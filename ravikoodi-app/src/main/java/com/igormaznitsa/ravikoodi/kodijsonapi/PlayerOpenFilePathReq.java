package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class PlayerOpenFilePathReq {
  
  
  @JsonProperty(value="item") 
  protected final PlayerOpenFileItem item;
  
  @JsonProperty(value = "options")
  protected final Map<String,String> options;
  
  public PlayerOpenFilePathReq(final String filePath, final Map<String,String> options) {
    this.item = new PlayerOpenFileItem(filePath);
    this.options = options;
  }
}
