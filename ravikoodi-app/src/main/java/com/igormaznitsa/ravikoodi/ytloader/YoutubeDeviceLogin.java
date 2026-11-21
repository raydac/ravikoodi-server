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

import static com.igormaznitsa.ravikoodi.ytloader.YoutubeTvCredentials.DEVICE_CODE_URL;
import static com.igormaznitsa.ravikoodi.ytloader.YoutubeTvCredentials.DEVICE_GRANT;
import static com.igormaznitsa.ravikoodi.ytloader.YoutubeTvCredentials.REVOKE_URL;
import static com.igormaznitsa.ravikoodi.ytloader.YoutubeTvCredentials.SCOPE;
import static com.igormaznitsa.ravikoodi.ytloader.YoutubeTvCredentials.TOKEN_URL;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.joining;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class YoutubeDeviceLogin {

  private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeDeviceLogin.class);
  private static final JsonMapper JSON = JsonMapper.builder().build();
  private static final String DEFAULT_VERIFICATION_URL = "https://www.google.com/device";

  private final HttpClient http;

  public YoutubeDeviceLogin() {
    this.http = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build();
  }

  static YoutubeDeviceCode parseDeviceCode(final String json, final long nowEpochMillis)
      throws IOException {
    final JsonNode node = read(json);
    final String error = errorCode(node);
    if (error != null) {
      throw new IOException("Google refused the device code (" + error + ")");
    }
    final String deviceCode = text(node, "device_code");
    final String userCode = text(node, "user_code");
    if (deviceCode == null || userCode == null) {
      throw new IOException("Google did not return a device code");
    }
    final String verification = text(node, "verification_url");
    return new YoutubeDeviceCode(
        deviceCode,
        userCode,
        verification == null || verification.isBlank() ? DEFAULT_VERIFICATION_URL : verification,
        nowEpochMillis + Math.max(1, node.path("expires_in").asLong(1800)) * 1000L,
        clampInterval(node.path("interval").asInt(5)));
  }

  static YoutubeDevicePoll parseTokenResponse(
      final String json,
      final long nowEpochMillis,
      @Nullable final String existingRefreshToken) throws IOException {
    final JsonNode node = read(json);
    final String error = errorCode(node);
    if (error != null) {
      return switch (error) {
        case "authorization_pending" -> new YoutubeDevicePoll.Pending(false);
        case "slow_down" -> new YoutubeDevicePoll.Pending(true);
        case "access_denied" -> new YoutubeDevicePoll.Rejected("Google denied the sign-in", false);
        case "expired_token" ->
            new YoutubeDevicePoll.Rejected("The code expired. Try again.", false);
        case "invalid_grant" -> new YoutubeDevicePoll.Rejected(
            "The saved YouTube sign-in is no longer valid", true);
        default ->
            new YoutubeDevicePoll.Rejected("Google refused the sign-in (" + error + ")", false);
      };
    }
    final String accessToken = text(node, "access_token");
    if (accessToken == null || accessToken.isBlank()) {
      throw new IOException("Google did not return an access token");
    }
    final String refreshToken = text(node, "refresh_token");
    final String keptRefresh = refreshToken == null || refreshToken.isBlank()
        ? existingRefreshToken
        : refreshToken;
    if (keptRefresh == null || keptRefresh.isBlank()) {
      throw new IOException("Google did not return a refresh token");
    }
    return new YoutubeDevicePoll.Approved(new YoutubeSession(
        keptRefresh,
        accessToken,
        nowEpochMillis + Math.max(1, node.path("expires_in").asLong(3600)) * 1000L));
  }

  private static JsonNode read(final String json) throws IOException {
    try {
      return JSON.readTree(json);
    } catch (IOException ex) {
      throw new IOException("Unexpected response from Google", ex);
    }
  }

  private static @Nullable String errorCode(final JsonNode node) {
    final JsonNode error = node.get("error");
    if (error == null || error.isNull()) {
      return null;
    }
    if (error.isTextual()) {
      return error.asText();
    }
    final JsonNode message = error.get("message");
    return message != null && message.isTextual() ? message.asText() : "error";
  }

  private static @Nullable String text(final JsonNode node, final String field) {
    final JsonNode value = node.get(field);
    return value != null && value.isTextual() ? value.asText() : null;
  }

  private static int clampInterval(final int seconds) {
    return Math.min(60, Math.max(5, seconds));
  }

  private static String encode(final Map<String, String> form) {
    return form.entrySet().stream()
        .map(entry -> URLEncoder.encode(entry.getKey(), UTF_8) + "="
            + URLEncoder.encode(entry.getValue(), UTF_8))
        .collect(joining("&"));
  }

  public YoutubeDeviceCode requestCode() throws IOException {
    final Map<String, String> form = new LinkedHashMap<>();
    form.put("client_id", YoutubeTvCredentials.clientId());
    form.put("scope", SCOPE);
    return parseDeviceCode(this.post(DEVICE_CODE_URL, form), System.currentTimeMillis());
  }

  public YoutubeDevicePoll poll(final String deviceCode) throws IOException {
    return this.exchange(deviceCode, null);
  }

  public YoutubeSession refresh(final YoutubeSession current) throws IOException {
    Objects.requireNonNull(current, "current");
    final YoutubeDevicePoll poll = this.exchange(null, current.refreshToken());
    if (poll instanceof YoutubeDevicePoll.Approved approved) {
      return approved.session();
    }
    if (poll instanceof YoutubeDevicePoll.Rejected rejected) {
      throw new YoutubeAuthRejected(rejected.message(), rejected.grantRejected());
    }
    throw new IOException("YouTube sign-in is still pending");
  }

  public void revokeQuietly(final String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return;
    }
    try {
      this.post(REVOKE_URL, Map.of("token", refreshToken));
    } catch (IOException ex) {
      LOGGER.warn("YouTube sign-out could not revoke the saved grant");
    }
  }

  private YoutubeDevicePoll exchange(
      @Nullable final String deviceCode,
      @Nullable final String refreshToken) throws IOException {
    final Map<String, String> form = new LinkedHashMap<>();
    form.put("client_id", YoutubeTvCredentials.clientId());
    form.put("client_secret", YoutubeTvCredentials.clientSecret());
    if (refreshToken == null) {
      form.put("code", Objects.requireNonNull(deviceCode, "deviceCode"));
      form.put("grant_type", DEVICE_GRANT);
    } else {
      form.put("refresh_token", refreshToken);
      form.put("grant_type", "refresh_token");
    }
    return parseTokenResponse(this.post(TOKEN_URL, form), System.currentTimeMillis(), refreshToken);
  }

  private String post(final String url, final Map<String, String> form) throws IOException {
    final HttpRequest request = HttpRequest.newBuilder(URI.create(url))
        .timeout(Duration.ofSeconds(30))
        .header("Content-Type", "application/x-www-form-urlencoded")
        .POST(HttpRequest.BodyPublishers.ofString(encode(form)))
        .build();
    try {
      final HttpResponse<String> response =
          this.http.send(request, HttpResponse.BodyHandlers.ofString(UTF_8));
      if (response.body() == null || response.body().isBlank()) {
        throw new IOException("Empty response from Google");
      }
      return response.body();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IOException("YouTube sign-in was interrupted", ex);
    }
  }
}
