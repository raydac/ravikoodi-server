package com.igormaznitsa.ravikoodi.kodijsonapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.igormaznitsa.ravikoodi.KodiAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KodiService {

  private static final Logger LOGGER = LoggerFactory.getLogger(KodiService.class);

  private final KodiAddress address;
  private final URL url;

  public enum Control {
    UP,
    DOWN,
    LEFT,
    RIGHT,
    SELECT,
    HOME,
    SEND_TEXT,
    BACK,
    CONTEXT_MENU
  }

  private static final class PlaylistReq {

    @JsonProperty("playlistid")
    final long playlistid;

    public PlaylistReq(final Playlist playlist) {
      this.playlistid = playlist.getPlaylistid();
    }
  }

  public KodiService(final KodiAddress address) throws MalformedURLException {
    this.address = address;
    this.url = new URL((address.isUseSsl() ? "https://" : "http://") + address.getHost() + ':' + address.getPort() + "/jsonrpc");
  }

  public PlaylistProperties getPlaylistProperties(final Playlist playlist, final String... properties) throws Throwable {
    final PlaylistPropertiesRequest request = new PlaylistPropertiesRequest(playlist, properties);
    return makeJsonRpcClient().invoke("Playlist.GetProperties", request, PlaylistProperties.class);
  }

  public ApplicationProperties getAllApplicationProperties() throws Throwable {
    return makeJsonRpcClient().invoke("Application.GetProperties", new ApplicationPropertiesReq(ApplicationProperties.ALL_NAMES), ApplicationProperties.class);
  }

  public ApplicationProperties getApplicationProperties(final String... properties) throws Throwable {
    return makeJsonRpcClient().invoke("Application.GetProperties", new ApplicationPropertiesReq(properties), ApplicationProperties.class);
  }

  public PlayerProperties getAllPlayerProperties(final ActivePlayerInfo player) throws Throwable {
    final PlayerPropertiesRequest request = new PlayerPropertiesRequest(player, PlayerProperties.ALL_NAMES);
    return makeJsonRpcClient().invoke("Player.GetProperties", request, PlayerProperties.class);
  }

  public PlayerProperties getPlayerProperties(final ActivePlayerInfo player, final String... playerProperties) throws Throwable {
    final PlayerPropertiesRequest request = new PlayerPropertiesRequest(player, playerProperties);
    return makeJsonRpcClient().invoke("Player.GetProperties", request, PlayerProperties.class);
  }

  public PlayerSeekResult doPlayerSeekPercentage(final ActivePlayerInfo player, final double percentage) throws Throwable {
    return makeJsonRpcClient().invoke("Player.Seek", new PlayerSeekPercentageReq(player, percentage), PlayerSeekResult.class);
  }

  public ActivePlayerInfo[] getActivePlayers() throws Throwable {
    return makeJsonRpcClient().invoke("Player.GetActivePlayers", null, ActivePlayerInfo[].class);
  }

  public String clearPlaylist(final Playlist playlist) throws Throwable {
    return makeJsonRpcClient().invoke("Playlist.Clear", new PlaylistReq(playlist), String.class);
  }

  public long setApplicationVolume(final long volume) throws Throwable {
    final Volume volumeObj = new Volume();
    volumeObj.setVolume(volume);
    return makeJsonRpcClient().invoke("Application.SetVolume", volumeObj, long.class);
  }

  public boolean setApplicationMute(final boolean mute) throws Throwable {
    final Mute muteObj = new Mute();
    muteObj.setMute(mute);
    return makeJsonRpcClient().invoke("Application.SetMute", muteObj, boolean.class);
  }

  public String sendInputExecuteAction(final ExecuteAction action) throws Throwable {
    return makeJsonRpcClient().invoke("Input.ExecuteAction", new InputExecuteAction(action), String.class);
  }

  public String sendControlEvent(final Control event, final Object... args) throws Throwable {
    switch (event) {
      case SELECT:
        return makeJsonRpcClient().invoke("Input.Select", new Empty(), String.class);
      case BACK:
        return makeJsonRpcClient().invoke("Input.Back", new Empty(), String.class);
      case CONTEXT_MENU:
        return makeJsonRpcClient().invoke("Input.ContextMenu", new Empty(), String.class);
      case DOWN:
        return makeJsonRpcClient().invoke("Input.Down", new Empty(), String.class);
      case LEFT:
        return makeJsonRpcClient().invoke("Input.Left", new Empty(), String.class);
      case RIGHT:
        return makeJsonRpcClient().invoke("Input.Right", new Empty(), String.class);
      case UP:
        return makeJsonRpcClient().invoke("Input.Up", new Empty(), String.class);
      case HOME:
        return makeJsonRpcClient().invoke("Input.Home", new Empty(), String.class);
      case SEND_TEXT:
        return makeJsonRpcClient().invoke("Input.SendText", new Text(args.length > 0 ? String.valueOf(args[0]) : ""), String.class);
      default:
        throw new Error("Unexpected event " + event);
    }
  }

  public String doSystemReboot() throws Throwable {
    return makeJsonRpcClient().invoke("System.Reboot", new Empty(), String.class);
  }

  public String doSystemShutdown() throws Throwable {
    return makeJsonRpcClient().invoke("System.Shutdown", new Empty(), String.class);
  }

  public String setPlayerAudiostream(final ActivePlayerInfo player, final AudioStream stream) throws Throwable {
    return makeJsonRpcClient().invoke("Player.SetAudioStream", new PlayerAudioStreamReq(player, stream), String.class);
  }

  public String setPlayerSubtitle(final ActivePlayerInfo player, final Subtitle subtitle, final boolean enable) throws Throwable {
    return makeJsonRpcClient().invoke("Player.SetSubtitle", new PlayerSubtitleReq(player, subtitle, enable), String.class);
  }

  public PlaylistItems getPlaylistItems(final Playlist playlist) throws Throwable {
    return makeJsonRpcClient().invoke("Playlist.GetItems", new PlaylistReq(playlist), PlaylistItems.class);
  }

  public PlayerSpeed setPlayerSpeed(final ActivePlayerInfo player, final long speed) throws Throwable {
    return makeJsonRpcClient().invoke("Player.SetSpeed", new PlayerSpeedReq(player, speed), PlayerSpeed.class);
  }

  public String doPlayerStop(final ActivePlayerInfo player) throws Throwable {
    return makeJsonRpcClient().invoke("Player.Stop", new PlayerIdReq(player), String.class);
  }

  public String doPlayerRepeat(final ActivePlayerInfo player, final String state) throws Throwable {
    return makeJsonRpcClient().invoke("Player.SetRepeat", new PlayerRepeatReq(player, state), String.class);
  }
  
  public String doPlayerOpenFile(final String filePath, final Map<String,String> ... options) throws Throwable {
    try {
      final Map<String,String> collectedOptions = Arrays.stream(options).flatMap(x -> x.entrySet().stream()).collect(Collectors.toMap(
          Map.Entry::getKey, Map.Entry::getValue));
      return makeJsonRpcClient().invoke("Player.Open", new PlayerOpenFilePathReq(filePath, collectedOptions), String.class);
    } catch (Exception e) {
      if (e.getMessage().contains("no response body")) {
        LOGGER.warn("Can't get response body");
        return "OK";
      } else {
        throw e;
      }
    }
  }

  public PlayerSpeed doPlayerStartPause(final ActivePlayerInfo player) throws Throwable {
    return makeJsonRpcClient().invoke("Player.PlayPause", new PlayerIdReq(player), PlayerSpeed.class);
  }

  public PlayerItem getPlayerItem(final ActivePlayerInfo player) throws Throwable {
    return makeJsonRpcClient().invoke("Player.GetItem", new PlayerItemReq(player, PlayerItem.ALL_NAMES), PlayerItem.class);
  }

  public Playlist[] getPlaylists() throws Throwable {
    return makeJsonRpcClient().invoke("Playlist.GetPlaylists", null, Playlist[].class);
  }

  private JsonRpcHttpClient makeJsonRpcClient() {
    final Map<String, String> headers = new HashMap<>();
    if (this.address.getName() != null && this.address.getPassword() != null) {
      headers.put("Authorization", "Basic " + Base64.getEncoder().encodeToString((this.address.getName() + ":" + this.address.getPassword()).getBytes(Charset.forName("US-ASCII"))));
    }

    final JsonRpcHttpClient result = new JsonRpcHttpClient(this.url, headers);

    result.setReadTimeoutMillis(5000);

    return result;
  }
}
