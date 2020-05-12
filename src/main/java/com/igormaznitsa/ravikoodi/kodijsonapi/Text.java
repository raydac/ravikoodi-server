package com.igormaznitsa.ravikoodi.kodijsonapi;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
public class Text {
  private final String text;

  public Text(final String text){
    this.text = text;
  }
}
