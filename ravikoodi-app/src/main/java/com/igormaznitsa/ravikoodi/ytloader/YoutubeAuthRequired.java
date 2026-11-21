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

import com.github.kiulian.downloader.YoutubeException;
import java.util.Locale;
import org.jspecify.annotations.Nullable;

public final class YoutubeAuthRequired {

  private YoutubeAuthRequired() {
  }

  public static boolean matches(@Nullable final Throwable error) {
    Throwable current = error;
    while (current != null) {
      if (requiresSignIn(current)) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  private static boolean requiresSignIn(final Throwable error) {
    if (error instanceof YoutubeException.BotCheckException) {
      return true;
    }
    if (error instanceof YoutubeException.HttpException http
        && (http.status() == 401 || http.status() == 403)) {
      return true;
    }
    final String message = error.getMessage();
    if (message == null) {
      return false;
    }
    final String lower = message.toLowerCase(Locale.ROOT);
    return lower.contains("forbidden")
        || lower.contains("bot check")
        || lower.contains("sign in")
        || lower.contains("login required");
  }
}
