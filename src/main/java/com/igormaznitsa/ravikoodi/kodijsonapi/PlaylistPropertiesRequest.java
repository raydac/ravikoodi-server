package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlaylistPropertiesRequest {
  
  @JsonProperty(value = "playlistid")
  public final long playlistid;
  @JsonProperty(value = "properties")
  public final String[] properties;

  public PlaylistPropertiesRequest(final Playlist playlist, final String... properties) {
    this.playlistid = playlist.getPlaylistid();
    this.properties = properties;
  }
  
}
