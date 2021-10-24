package com.igormaznitsa.ravikoodi.kodijsonapi;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
public class Subtitle implements Comparable<Subtitle> {

  private final long index;
  private final String language;
  private final String name;

  @Override
  public boolean equals(final Object o){
    if (o == this) {
      return true;
    }

    if (o instanceof Subtitle) {
      return this.index == ((Subtitle) o).index;
    }

    return false;
  }

  @Override
  public int hashCode() {
    int hash = 7;
    hash = 83 * hash + (int) (this.index ^ (this.index >>> 32));
    return hash;
  }

  @Override
  public String toString() {
    return this.language + " (" + this.name + ')';
  }

  @Override
  public int compareTo(final Subtitle o) {
    return Long.compare(this.index, o.index);
  }

}
