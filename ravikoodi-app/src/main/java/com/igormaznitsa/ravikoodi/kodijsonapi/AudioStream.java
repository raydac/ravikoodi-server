package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
@AllArgsConstructor
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
    return Objects.toString(this.language, "") + " (" + Objects.toString(this.name, "") + ')';
  }

  @Override
  public int hashCode() {
    return Long.hashCode(this.index);
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
