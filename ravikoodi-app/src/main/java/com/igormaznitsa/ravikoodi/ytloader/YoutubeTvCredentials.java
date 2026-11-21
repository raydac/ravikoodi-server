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

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Base64.getDecoder;

import com.github.kiulian.downloader.client.ClientProfile;

final class YoutubeTvCredentials {

  static final String CLIENT_DOMAIN = ".apps.googleusercontent.com";
  static final String SCOPE = "https://www.googleapis.com/auth/youtube";
  static final String DEVICE_CODE_URL = "https://accounts.google.com/o/oauth2/device/code";
  static final String TOKEN_URL = "https://www.googleapis.com/oauth2/v4/token";
  static final String REVOKE_URL = "https://accounts.google.com/o/oauth2/revoke";
  static final String DEVICE_GRANT = "http://oauth.net/grant_type/device/1.0";
  static final int TV_CLIENT_NAME_ID = 7;

  private static final String CLIENT_ID = decode(
      "ODYxNTU2NzA4NDU0LWQ2ZGxtM2xoMDVpZGQ4bnBlazE4azZiZThiYTNvYzY4");
  private static final String CLIENT_SECRET = decode("U2JvVmhvRzlzMHJOYWZpeENTR0dLWEFU");

  private YoutubeTvCredentials() {
  }

  static String clientId() {
    return CLIENT_ID.endsWith(CLIENT_DOMAIN) ? CLIENT_ID : CLIENT_ID + CLIENT_DOMAIN;
  }

  static String clientSecret() {
    return CLIENT_SECRET;
  }

  static ClientProfile playerProfile() {
    return ClientProfile.builder("tv", "TVHTML5", TV_CLIENT_NAME_ID)
        .version("7.20250923.13.00")
        .userAgent("Mozilla/5.0 (ChromiumStylePlatform) Cobalt/25.lts.30.1034943-gold"
            + " (unlike Gecko) Unknown_TV_Unknown_0/Unknown (Unknown, Unknown)")
        .context("deviceMake", "Samsung")
        .context("deviceModel", "SmartTV")
        .context("osName", "Tizen")
        .context("osVersion", "4.0.0.2")
        .context("platform", "TV")
        .build();
  }

  private static String decode(final String encoded) {
    return new String(getDecoder().decode(encoded), UTF_8);
  }
}
