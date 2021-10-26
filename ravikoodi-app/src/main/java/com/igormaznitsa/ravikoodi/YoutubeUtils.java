/*
 * Copyright 2021 Igor Maznitsa.
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
package com.igormaznitsa.ravikoodi;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YoutubeUtils {

    private YoutubeUtils() {
    }

    private static final Pattern PATTERN_YOUTUBE_VIDEO_ID = Pattern.compile(".*[&?]v=([\\w-]+).*|.*tu.be\\/([\\w-]+).*", Pattern.CASE_INSENSITIVE);
    private static final Pattern PATTERN_YOUTUBE_VIDEO_PLAYLIST_ID = Pattern.compile(".*[^\\w-]list=([\\w-]+).*", Pattern.CASE_INSENSITIVE);

    public static Optional<String> extractYoutubeVideoId(final String url) {
        final Matcher matcher = PATTERN_YOUTUBE_VIDEO_ID.matcher(url.trim());
        if (matcher.find()) {
            if (matcher.group(1)!=null) {
                return Optional.ofNullable(matcher.group(1));
            } else {
                return Optional.ofNullable(matcher.group(2));
            }
        } else {
            return Optional.empty();
        }
    }

    public static Optional<String> extractYoutubePlaylistId(final String url) {
        final Matcher matcher = PATTERN_YOUTUBE_VIDEO_PLAYLIST_ID.matcher(url.trim());
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1));
        } else {
            return Optional.empty();
        }
    }
}
