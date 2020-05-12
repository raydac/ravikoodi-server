package com.igormaznitsa.ravikoodi.kodijsonapi;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
public class Playlist {
  private final long playlistid;
  private final String type;
}
