package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
public class PlaylistIdReq {
  @JsonProperty("playlistid")
  protected final long playlistid;
  
  public PlaylistIdReq(final Playlist playlist) {
    this.playlistid = playlist.getPlaylistid();
  }
}
