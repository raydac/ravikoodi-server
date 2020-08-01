package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class PlayerOpenPlaylistReq {

    @JsonProperty(value = "item")
    protected final PlayerOpenPlaylistItem item;

    @JsonProperty(value = "options")
    protected final Map<String, String> options;

    public PlayerOpenPlaylistReq(final Playlist playlist, final Map<String, String> options) {
        this.item = new PlayerOpenPlaylistItem(playlist);
        this.options = options;
    }

}
