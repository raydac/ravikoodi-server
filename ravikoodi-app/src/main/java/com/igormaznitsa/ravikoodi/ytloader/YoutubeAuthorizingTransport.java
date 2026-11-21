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

import static com.igormaznitsa.ravikoodi.ytloader.YoutubeTvCredentials.TV_CLIENT_NAME_ID;

import com.github.kiulian.downloader.http.HttpTransport;
import java.io.IOException;
import java.util.function.Supplier;
import org.jspecify.annotations.Nullable;

final class YoutubeAuthorizingTransport implements HttpTransport {

  private final HttpTransport delegate;
  private final Supplier<@Nullable String> accessToken;

  YoutubeAuthorizingTransport(
      final HttpTransport delegate,
      final Supplier<@Nullable String> accessToken) {
    this.delegate = delegate;
    this.accessToken = accessToken;
  }

  private static boolean isTvInnertube(final Request request) {
    return
        Integer.toString(TV_CLIENT_NAME_ID).equals(request.headers().get("X-YouTube-Client-Name"))
            && request.url().contains("/youtubei/");
  }

  private static Request.Builder copy(final Request request) {
    final Request.Builder builder = request.body() == null
        ? Request.get(request.url())
        : Request.post(request.url(), request.body());
    request.headers().forEach(builder::header);
    return builder;
  }

  @Override
  public Response send(final Request request) throws IOException {
    final String token = this.accessToken.get();
    if (token == null || token.isBlank() || !isTvInnertube(request)) {
      return this.delegate.send(request);
    }
    final Request.Builder authorized = copy(request).header("Authorization", "Bearer " + token);
    return this.delegate.send(authorized.build());
  }
}
