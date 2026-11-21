package com.igormaznitsa.ravikoodi.ytloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.http.HttpTransport;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class YoutubeAuthTest {

  private static HttpTransport.Response emptyResponse() {
    return new HttpTransport.Response() {
      @Override
      public int status() {
        return 204;
      }

      @Override
      public String header(final String name) {
        return null;
      }

      @Override
      public InputStream body() {
        return new ByteArrayInputStream(new byte[0]);
      }

      @Override
      public void close() {
      }
    };
  }

  @Test
  public void deviceCodeUsesGoogleDevicePage() throws Exception {
    final YoutubeDeviceCode code = YoutubeDeviceLogin.parseDeviceCode("""
        {"device_code":"device","user_code":"ABCD-EFGH","verification_url":"https://www.google.com/device","expires_in":1800,"interval":5}
        """, 1_000L);

    assertEquals("ABCD-EFGH", code.userCode());
    assertEquals("https://www.google.com/device", code.verificationUrl());
    assertEquals(5, code.intervalSeconds());
    assertEquals(1_000L + 1_800_000L, code.expiresAtEpochMillis());
  }

  @Test
  public void pendingAndDeniedPollsStayFreeOfTokens() throws Exception {
    assertTrue(YoutubeDeviceLogin.parseTokenResponse(
        "{\"error\":\"authorization_pending\"}", 0, null) instanceof YoutubeDevicePoll.Pending);
    assertTrue(YoutubeDeviceLogin.parseTokenResponse(
        "{\"error\":\"slow_down\"}", 0, null) instanceof YoutubeDevicePoll.Pending pending
        && pending.slowDown());

    final YoutubeDevicePoll denied = YoutubeDeviceLogin.parseTokenResponse(
        "{\"error\":\"access_denied\"}", 0, null);
    assertTrue(denied instanceof YoutubeDevicePoll.Rejected);
  }

  @Test
  public void approvedPollKeepsThePreviousRefreshToken() throws Exception {
    final YoutubeDevicePoll poll = YoutubeDeviceLogin.parseTokenResponse("""
        {"access_token":"access-new","expires_in":10}
        """, 5_000L, "refresh-old");

    final YoutubeSession session = ((YoutubeDevicePoll.Approved) poll).session();
    assertEquals("access-new", session.accessToken());
    assertEquals("refresh-old", session.refreshToken());
    assertEquals("YoutubeSession{signedIn=true}", session.toString());
  }

  @Test
  public void sessionRoundTripStaysEncryptedAndRefreshes() throws Exception {
    final MemoryPreferences preferences = new MemoryPreferences();
    final AtomicReference<Integer> refreshes = new AtomicReference<>(0);
    final YoutubeSessionStore store = new YoutubeSessionStore(preferences, current -> {
      refreshes.set(refreshes.get() + 1);
      return new YoutubeSession(current.refreshToken(), "access-2",
          System.currentTimeMillis() + 3_600_000L);
    });
    store.save(new YoutubeSession("refresh", "access-1", System.currentTimeMillis() + 3_600_000L));

    assertEquals("access-1", store.accessToken());
    assertEquals(0, refreshes.get().intValue());
    assertNotNull(preferences.get("youtube.session"));
    assertNotNull(preferences.get("youtube.session.key"));
    assertFalse(preferences.storedText().contains("access-1"));

    store.save(new YoutubeSession("refresh", "access-1", System.currentTimeMillis() - 1_000L));
    assertEquals("access-2", store.accessToken());
    assertEquals(1, refreshes.get().intValue());

    final YoutubeSessionStore reopened = new YoutubeSessionStore(preferences, current -> current);
    assertEquals("access-2", reopened.accessToken());
    assertEquals("refresh", reopened.takeRefreshToken());
    assertFalse(reopened.hasSession());
    assertNull(preferences.get("youtube.session"));
    assertNull(preferences.get("youtube.session.key"));
  }

  @Test
  public void bearerIsAddedOnlyForTheTvClient() throws Exception {
    final AtomicReference<HttpTransport.Request> seen = new AtomicReference<>();
    final HttpTransport capture = request -> {
      seen.set(request);
      return emptyResponse();
    };
    final YoutubeAuthorizingTransport transport =
        new YoutubeAuthorizingTransport(capture, () -> "token-value");

    transport.send(
        HttpTransport.Request.post("https://www.youtube.com/youtubei/v1/player", new byte[] {1})
            .header("X-YouTube-Client-Name", "7")
            .build());
    assertEquals("Bearer token-value", seen.get().headers().get("Authorization"));

    transport.send(HttpTransport.Request.get("https://www.youtube.com/youtubei/v1/player")
        .header("X-YouTube-Client-Name", "101")
        .build());
    assertNull(seen.get().headers().get("Authorization"));
  }

  @Test
  public void forbiddenAndBotCheckAskForSignIn() {
    assertTrue(
        YoutubeAuthRequired.matches(new YoutubeException.BotCheckException("id", List.of())));
    assertTrue(YoutubeAuthRequired.matches(new YoutubeException.HttpException(403, "nope", null)));
    assertTrue(YoutubeAuthRequired.matches(new IOException("403 Forbidden")));
    assertFalse(YoutubeAuthRequired.matches(new IOException("timed out")));
    assertEquals(7, YoutubeTvCredentials.playerProfile().clientId());
    assertEquals("TVHTML5", YoutubeTvCredentials.playerProfile().clientName());
  }

  private static final class MemoryPreferences implements YoutubeSessionStore.PreferenceNode {

    private final Map<String, String> values = new HashMap<>();

    @Override
    public String get(final String key) {
      return this.values.get(key);
    }

    @Override
    public void put(final String key, final String value) {
      this.values.put(key, value);
    }

    @Override
    public void remove(final String key) {
      this.values.remove(key);
    }

    private String storedText() {
      return String.join("\n", this.values.values());
    }
  }
}
