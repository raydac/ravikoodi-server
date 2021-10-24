package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PlayerOpenPlaylistItem {
  
  @JsonProperty(value = "playlistid")
  protected final long playlistid;

  PlayerOpenPlaylistItem(final Playlist playlist) {
    this.playlistid = playlist.getPlaylistid();
  }
  
}
