/*
 * Copyright 2026 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.igormaznitsa.ravikoodi.ytloader;

import static com.igormaznitsa.ravikoodi.ApplicationPreferences.Option.YOUTUBE_SESSION;
import static com.igormaznitsa.ravikoodi.ApplicationPreferences.Option.YOUTUBE_SESSION_KEY;
import static java.util.Base64.getDecoder;
import static java.util.Base64.getEncoder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.igormaznitsa.ravikoodi.ApplicationPreferences;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Optional;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class YoutubeSessionStore {

  private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeSessionStore.class);
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final int KEY_BYTES = 32;
  private static final int IV_BYTES = 12;
  private static final int TAG_BITS = 128;

  private final PreferenceNode preferences;
  private final TokenRefresher refresher;
  private final SecureRandom random = new SecureRandom();

  @Autowired
  public YoutubeSessionStore(final YoutubeDeviceLogin login) {
    this(new JavaPreferenceNode(ApplicationPreferences.findPreferences()), login::refresh);
  }

  YoutubeSessionStore(final PreferenceNode preferences, final TokenRefresher refresher) {
    this.preferences = preferences;
    this.refresher = refresher;
  }

  public synchronized boolean hasSession() {
    return this.read().isPresent();
  }

  public synchronized @Nullable String accessToken() {
    try {
      return this.fresh().map(YoutubeSession::accessToken).filter(token -> !token.isBlank())
          .orElse(null);
    } catch (IOException ex) {
      LOGGER.warn("YouTube access token is unavailable");
      return null;
    }
  }

  public synchronized void save(final YoutubeSession session) throws IOException {
    final ObjectNode node = JSON.createObjectNode();
    node.put("refreshToken", session.refreshToken());
    node.put("accessToken", session.accessToken());
    node.put("expiresAtEpochMillis", session.expiresAtEpochMillis());
    this.preferences.put(
        YOUTUBE_SESSION.getPropertyName(),
        getEncoder().encodeToString(this.encrypt(JSON.writeValueAsBytes(node))));
  }

  public synchronized void clear() {
    this.deleteSession();
  }

  public synchronized String takeRefreshToken() {
    final String refreshToken = this.read().map(YoutubeSession::refreshToken).orElse("");
    this.deleteSession();
    return refreshToken;
  }

  private Optional<YoutubeSession> fresh() throws IOException {
    final Optional<YoutubeSession> stored = this.read();
    if (stored.isEmpty()) {
      return stored;
    }
    final YoutubeSession session = stored.get();
    if (session.accessIsFresh(System.currentTimeMillis())) {
      return stored;
    }
    try {
      final YoutubeSession refreshed = this.refresher.refresh(session);
      this.save(refreshed);
      return Optional.of(refreshed);
    } catch (YoutubeAuthRejected rejected) {
      if (rejected.isGrantRejected()) {
        this.deleteSession();
      }
      LOGGER.warn("YouTube sign-in refresh was rejected");
      return Optional.empty();
    }
  }

  private Optional<YoutubeSession> read() {
    final String stored = this.preferences.get(YOUTUBE_SESSION.getPropertyName());
    if (stored == null || stored.isBlank()) {
      return Optional.empty();
    }
    try {
      final JsonNode node = JSON.readTree(this.decrypt(getDecoder().decode(stored)));
      return Optional.of(new YoutubeSession(
          node.path("refreshToken").asText(""),
          node.path("accessToken").asText(""),
          node.path("expiresAtEpochMillis").asLong(0)));
    } catch (IOException | RuntimeException ex) {
      LOGGER.warn("Saved YouTube sign-in can't be read");
      this.deleteSession();
      return Optional.empty();
    }
  }

  private void deleteSession() {
    try {
      this.preferences.remove(YOUTUBE_SESSION.getPropertyName());
      this.preferences.remove(YOUTUBE_SESSION_KEY.getPropertyName());
    } catch (IOException ex) {
      LOGGER.warn("Can't remove the saved YouTube sign-in");
    }
  }

  private byte[] encrypt(final byte[] plain) throws IOException {
    try {
      final byte[] iv = new byte[IV_BYTES];
      this.random.nextBytes(iv);
      final Cipher cipher = this.cipher(Cipher.ENCRYPT_MODE, iv);
      final byte[] body = cipher.doFinal(plain);
      final byte[] stored = new byte[iv.length + body.length];
      System.arraycopy(iv, 0, stored, 0, iv.length);
      System.arraycopy(body, 0, stored, iv.length, body.length);
      return stored;
    } catch (GeneralSecurityException ex) {
      throw new IOException("Can't encrypt the YouTube sign-in", ex);
    }
  }

  private byte[] decrypt(final byte[] stored) throws IOException {
    if (stored.length <= IV_BYTES) {
      throw new IOException("Saved YouTube sign-in is truncated");
    }
    try {
      final byte[] iv = new byte[IV_BYTES];
      System.arraycopy(stored, 0, iv, 0, iv.length);
      final Cipher cipher = this.cipher(Cipher.DECRYPT_MODE, iv);
      return cipher.doFinal(stored, IV_BYTES, stored.length - IV_BYTES);
    } catch (GeneralSecurityException ex) {
      throw new IOException("Can't decrypt the YouTube sign-in", ex);
    }
  }

  private Cipher cipher(final int mode, final byte[] iv)
      throws IOException, GeneralSecurityException {
    final Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
    cipher.init(mode, new SecretKeySpec(this.key(), "AES"), new GCMParameterSpec(TAG_BITS, iv));
    return cipher;
  }

  private byte[] key() throws IOException {
    final String stored = this.preferences.get(YOUTUBE_SESSION_KEY.getPropertyName());
    if (stored != null && !stored.isBlank()) {
      final byte[] decoded;
      try {
        decoded = getDecoder().decode(stored);
      } catch (IllegalArgumentException ex) {
        throw new IOException("YouTube sign-in key can't be read", ex);
      }
      if (decoded.length != KEY_BYTES) {
        throw new IOException("YouTube sign-in key has the wrong size");
      }
      return decoded;
    }
    final byte[] created = new byte[KEY_BYTES];
    this.random.nextBytes(created);
    this.preferences.put(YOUTUBE_SESSION_KEY.getPropertyName(),
        getEncoder().encodeToString(created));
    return created;
  }

  interface PreferenceNode {
    @Nullable String get(final String key);

    void put(final String key, final String value) throws IOException;

    void remove(final String key) throws IOException;
  }

  @FunctionalInterface
  interface TokenRefresher {
    YoutubeSession refresh(YoutubeSession current) throws IOException;
  }

  private static final class JavaPreferenceNode implements PreferenceNode {

    private final Preferences preferences;

    private JavaPreferenceNode(final Preferences preferences) {
      this.preferences = preferences;
    }

    @Override
    public @Nullable String get(final String key) {
      synchronized (this.preferences) {
        final String value = this.preferences.get(key, null);
        return value == null || value.isBlank() ? null : value;
      }
    }

    @Override
    public void put(final String key, final String value) throws IOException {
      if (value.length() > Preferences.MAX_VALUE_LENGTH) {
        throw new IOException("YouTube sign-in does not fit in the application properties");
      }
      synchronized (this.preferences) {
        this.preferences.put(key, value);
        this.flush();
      }
    }

    @Override
    public void remove(final String key) throws IOException {
      synchronized (this.preferences) {
        this.preferences.remove(key);
        this.flush();
      }
    }

    private void flush() throws IOException {
      try {
        this.preferences.flush();
      } catch (BackingStoreException ex) {
        throw new IOException("Can't store the YouTube sign-in", ex);
      }
    }
  }
}
