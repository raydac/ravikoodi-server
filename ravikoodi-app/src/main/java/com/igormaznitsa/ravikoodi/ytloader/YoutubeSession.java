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

import java.util.Objects;

record YoutubeSession(String refreshToken, String accessToken, long expiresAtEpochMillis) {

  private static final long FRESH_MARGIN_MILLIS = 60_000L;

  YoutubeSession {
    Objects.requireNonNull(refreshToken, "refreshToken");
    if (refreshToken.isBlank()) {
      throw new IllegalArgumentException("refreshToken is empty");
    }
    accessToken = accessToken == null ? "" : accessToken;
  }

  boolean accessIsFresh(final long nowEpochMillis) {
    return !this.accessToken.isBlank()
        && this.expiresAtEpochMillis - nowEpochMillis > FRESH_MARGIN_MILLIS;
  }

  @Override
  public String toString() {
    return "YoutubeSession{signedIn=true}";
  }
}
