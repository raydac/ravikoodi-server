/*
 * Copyright 2023 Igor Maznitsa.
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

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.client.ClientProfiles;
import com.github.kiulian.downloader.http.UrlConnectionTransport;
import com.github.kiulian.downloader.model.playlist.Playlist;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.Format;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.TriConsumer;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class YtLinkExtractor {

  private static final Logger LOGGER = LoggerFactory.getLogger(YtLinkExtractor.class);

  private final ExecutorService executor;
  private final YoutubeSessionStore sessions;
  private volatile YoutubeDownloader youtubeDownloader;

  public YtLinkExtractor(
      @NonNull final ExecutorService executor,
      @NonNull final YoutubeSessionStore sessions) {
    this.executor = executor;
    this.sessions = sessions;
  }

  public void reload() {
    final YoutubeDownloader previous;
    synchronized (this) {
      previous = this.youtubeDownloader;
      this.youtubeDownloader = this.buildDownloader();
    }
    if (previous != null) {
      previous.close();
    }
  }

  private YoutubeDownloader downloader() {
    final YoutubeDownloader current = this.youtubeDownloader;
    if (current != null) {
      return current;
    }
    synchronized (this) {
      if (this.youtubeDownloader == null) {
        this.youtubeDownloader = this.buildDownloader();
      }
      return this.youtubeDownloader;
    }
  }

  private YoutubeDownloader buildDownloader() {
    final YoutubeDownloader.Builder builder = YoutubeDownloader.builder()
        .executor(this.executor)
        .retries(5)
        .transport(new YoutubeAuthorizingTransport(
            UrlConnectionTransport.create(), this.sessions::accessToken));
    if (this.sessions.hasSession()) {
      builder.clients(List.of(
          YoutubeTvCredentials.playerProfile(),
          ClientProfiles.VISIONOS,
          ClientProfiles.ANDROID_VR));
    }
    return builder.build();
  }

  @NonNull
  private static Optional<Pair<Format, YtQuality>> findAppropriateVideoFormat(
      @NonNull final List<Format> formats,
      @NonNull final YtQuality preferredQuality,
      @NonNull final YtVideoType requiredVideoType) {
    Format found = null;
    YtQuality foundQuality = YtQuality.UNKNOWN;
    for (final Format format : formats) {
      final YtQuality currentQuality = YtQuality.find(format.qualityLabel());
      final YtVideoType type = YtVideoType.find(format.mimeType());

      if (requiredVideoType == type) {
        if (currentQuality == preferredQuality) {
          found = format;
          foundQuality = currentQuality;
          break;
        } else if (foundQuality.ordinal() < currentQuality.ordinal()
            && currentQuality.ordinal() < preferredQuality.ordinal()) {
          found = format;
          foundQuality = currentQuality;
        }
      }
    }
    return found == null ? Optional.empty() : Optional.of(Pair.of(found, foundQuality));
  }

  public void findPlayListInfoAsync(
      @NonNull final String youTubePlaylistId,
      @NonNull final TriConsumer<String, Playlist, Throwable> resultConsumer) {
    LOGGER.info("Request to find Playlist on youtube: {}", youTubePlaylistId);

    try {
      this.executor.submit(() -> this.resolvePlaylist(youTubePlaylistId, resultConsumer));
    } catch (RejectedExecutionException ex) {
      LOGGER.error("Can't submit task, rejected execution", ex);
      resultConsumer.accept(youTubePlaylistId, null, ex);
    }
  }

  private void resolvePlaylist(
      @NonNull final String youTubePlaylistId,
      @NonNull final TriConsumer<String, Playlist, Throwable> resultConsumer) {
    try {
      final Playlist playlist = this.downloader().playlist(youTubePlaylistId);
      LOGGER.info("Resolved Youtube playlist request {}, items {} ",
          youTubePlaylistId,
          playlist.details().videoCount());
      resultConsumer.accept(youTubePlaylistId, playlist, null);
    } catch (Throwable error) {
      LOGGER.error("Error during search youtube playlist '{}'", youTubePlaylistId, error);
      resultConsumer.accept(youTubePlaylistId, null, error);
    }
  }

  public YoutubeDownloader getYoutubeDownloader() {
    return this.downloader();
  }

  public void findVideoUrlAsync(
      @NonNull final String youTubeVideoId,
      @NonNull final YtQuality preferredQuality,
      @NonNull final YtVideoType requiredVideoType,
      @NonNull final TriConsumer<String, String, Throwable> resultConsumer) {
    LOGGER.info("Request to find URL on youtube: {}", youTubeVideoId);

    final CompletableFuture<VideoInfo> pending;
    try {
      pending = this.downloader().videoInfoAsync(youTubeVideoId);
    } catch (RuntimeException error) {
      LOGGER.error("Can't request youtube video '{}'", youTubeVideoId, error);
      resultConsumer.accept(youTubeVideoId, null, error);
      return;
    }

    pending.whenComplete((videoInfo, error) -> this.publishResolvedVideoUrl(
        youTubeVideoId, preferredQuality, requiredVideoType, resultConsumer, videoInfo, error));
  }

  private void publishResolvedVideoUrl(
      @NonNull final String youTubeVideoId,
      @NonNull final YtQuality preferredQuality,
      @NonNull final YtVideoType requiredVideoType,
      @NonNull final TriConsumer<String, String, Throwable> resultConsumer,
      @Nullable final VideoInfo videoInfo,
      @Nullable final Throwable error) {
    if (error != null || videoInfo == null) {
      final Throwable failure = error == null
          ?
          new IllegalStateException("Youtube returned no video info for '" + youTubeVideoId + '\'')
          : error;
      LOGGER.error("Error during search video formats for youtube '{}'", youTubeVideoId, failure);
      resultConsumer.accept(youTubeVideoId, null, failure);
      return;
    }

    final List<Format> formatsWithSound = videoInfo.formats().muxed();
    final List<Format> formatsNoSound = videoInfo.formats().videoOnly();
    LOGGER.info(
        "Resolved video info for {}, found {} sounded formats, found {} no sound formats",
        youTubeVideoId,
        formatsWithSound.size(), formatsNoSound.size());

    final Pair<Format, YtQuality> found = findAppropriateVideoFormat(
        formatsWithSound, preferredQuality, requiredVideoType)
        .orElseGet(() -> findAppropriateVideoFormat(
            formatsNoSound, preferredQuality, requiredVideoType).orElse(null));

    if (found == null) {
      LOGGER.error(
          "Can't find any format for '{}', for preferred quality {} and required type {}",
          youTubeVideoId, preferredQuality, requiredVideoType);
      resultConsumer.accept(youTubeVideoId, null, new NoSuchElementException(
          "Can't find required video format or resolution for '" + youTubeVideoId + '\''));
      return;
    }

    final String url = found.getLeft().url();
    if (url.isBlank()) {
      LOGGER.error("URL for {} is empty!", youTubeVideoId);
      resultConsumer.accept(youTubeVideoId, null, new IllegalStateException(
          "URL is empty for found link of video '" + youTubeVideoId + "'"));
      return;
    }

    LOGGER.info("Detected URL for {}: {}", youTubeVideoId, url);
    resultConsumer.accept(youTubeVideoId, url, null);
  }
}
