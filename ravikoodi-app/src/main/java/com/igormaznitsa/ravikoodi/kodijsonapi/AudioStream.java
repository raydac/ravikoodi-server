package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AudioStream implements Comparable<AudioStream> {

  private final long bitrate;
  private final long channels;
  private final String codec;
  private final long index;
  private final String language;
  private final String name;

  @Override
  public String toString() {
    return this.language + " (" + this.name + ')';
  }

  @Override
  public int hashCode() {
    return (int) this.index + (this.name.hashCode() << 16);
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    }
    if (o instanceof AudioStream) {
      return this.index == ((AudioStream) o).index;
    }
    return false;
  }

  @Override
  public int compareTo(final AudioStream o) {
    return Long.compare(this.index, o.index);
  }
}
