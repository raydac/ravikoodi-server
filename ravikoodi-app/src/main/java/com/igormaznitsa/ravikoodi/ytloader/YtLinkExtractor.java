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
import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.VideoFormat;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.logging.log4j.util.TriConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class YtLinkExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(YtLinkExtractor.class);

    final ScheduledExecutorService executor;

    public YtLinkExtractor(@NonNull final ScheduledExecutorService executor) {
        this.executor = executor;
    }

    @NonNull
    private static Optional<Pair<VideoFormat, YtQuality>> findAppropriateVideoFormat(
            @NonNull final List<? extends VideoFormat> formats, 
            @NonNull final YtQuality preferredQuality,
            @NonNull final YtVideoType requiredVideoType) {
        VideoFormat found = null;
        YtQuality foundQuality = YtQuality.UNKNOWN;
        for (var format : formats) {
            final YtQuality currentQuality = YtQuality.find(format.qualityLabel());
            final YtVideoType type = YtVideoType.find(format.mimeType());

            if (requiredVideoType == type) {
                if (currentQuality == preferredQuality) {
                    found = format;
                    foundQuality = currentQuality;
                    break;
                } else if (foundQuality.ordinal() < currentQuality.ordinal() && currentQuality.ordinal() < preferredQuality.ordinal()) {
                    found = format;
                    foundQuality = currentQuality;
                }
            }
        }
        return found == null ? Optional.empty() : Optional.of(Pair.of(found, foundQuality));
    }
    
    public void findUrlAsync(
            @NonNull final String youTubeVideoId,
            @NonNull final YtQuality preferredQuality,
            @NonNull final YtVideoType requiredVideoType,
            @NonNull final TriConsumer<String, String, Throwable> resultConsumer) {
        LOGGER.info("Request to find URL on youtube: {}", youTubeVideoId);

        final YoutubeDownloader downloader = new YoutubeDownloader();
        downloader.getConfig().setExecutorService(this.executor);
        downloader.getConfig().setMaxRetries(5);
        downloader.getConfig().setCompressionEnabled(true);

        final RequestVideoInfo request = new RequestVideoInfo(youTubeVideoId)
                .callback(new YoutubeCallback<VideoInfo>() {
                    @Override
                    public void onFinished(final VideoInfo videoInfo) {
                        final List<? extends VideoFormat> formatsWithSound = videoInfo.videoWithAudioFormats();
                        final List<VideoFormat> formatsNoSound = videoInfo.videoFormats();
                        LOGGER.info("Resolved video info for {}, found {} sounded formats, found {} no sound formats", youTubeVideoId, 
                                formatsWithSound.size(), formatsNoSound.size());

                        final Pair<VideoFormat, YtQuality> found = findAppropriateVideoFormat(formatsWithSound, preferredQuality, requiredVideoType)
                                .orElseGet(() -> findAppropriateVideoFormat(formatsNoSound, preferredQuality, requiredVideoType).orElse(null));
                        
                        if (found == null) {
                            LOGGER.error("Can't find any format for '{}', for preferred quality {} and required type {}", youTubeVideoId, preferredQuality, requiredVideoType);
                            resultConsumer.accept(youTubeVideoId, null, new NoSuchElementException("Can't find required video format or resolution for '" + youTubeVideoId + '\''));
                        } else {
                            final String url = found.getLeft().url();
                            if (url != null) {
                                LOGGER.info("Detected URL for {}: {}", youTubeVideoId, url);
                                resultConsumer.accept(youTubeVideoId, url, null);
                            } else {
                                LOGGER.error("URL for {} is null!", youTubeVideoId);
                                resultConsumer.accept(youTubeVideoId, url, new NullPointerException("URL is null for found link of video '" + youTubeVideoId + "'"));
                            }
                        }
                    }

                    @Override
                    public void onError(final Throwable throwable) {
                        LOGGER.error("Error during search video formats for youtube '{}'", youTubeVideoId, throwable);
                        resultConsumer.accept(youTubeVideoId, null, throwable);
                    }
                })
                .async();
        downloader.getVideoInfo(request);
    }
}
