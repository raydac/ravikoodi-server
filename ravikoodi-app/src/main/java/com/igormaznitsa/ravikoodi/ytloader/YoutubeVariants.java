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

import com.github.kiulian.downloader.model.videos.formats.Format;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class YoutubeVariants {

  private static final Pattern FRAME_RATE = Pattern.compile("(?i)\\d+p(\\d+)");

  private static final Comparator<Choice> DISPLAY_ORDER = Comparator
      .comparingInt((Choice choice) -> Math.max(choice.quality().getLines(), 0)).reversed()
      .thenComparingInt(Choice::frameRate)
      .thenComparingInt(choice -> audioRank(choice.soundtrackCodec()))
      .thenComparing(Choice::toString);

  private YoutubeVariants() {
  }

  static List<Choice> withSound(final List<Format> formats, final YtVideoType required) {
    return unique(videos(formats, required).stream()
        .filter(format -> audioCodec(format).isPresent())
        .toList(), YoutubeVariants::includedKey).stream()
        .map(YoutubeVariants::included)
        .sorted(DISPLAY_ORDER)
        .toList();
  }

  static List<Choice> separate(final List<Format> formats, final YtVideoType required) {
    final List<Format> pictures = unique(videos(formats, required).stream()
        .filter(format -> audioCodec(format).isEmpty())
        .toList(), YoutubeVariants::pictureKey);
    final List<Format> tracks = unique(playable(formats).stream()
        .filter(format -> format.hasAudio() && !format.hasVideo())
        .filter(format -> audioCodec(format).isPresent())
        .toList(), YoutubeVariants::soundtrackLabel);
    if (pictures.isEmpty() || tracks.isEmpty()) {
      return List.of();
    }

    return pictures.stream()
        .flatMap(video -> tracks.stream().map(audio -> joined(video, audio)))
        .sorted(DISPLAY_ORDER)
        .toList();
  }

  static Optional<Choice> preferred(
      final List<Choice> choices,
      final YtQuality quality,
      final YtVideoType container) {
    if (choices.isEmpty()) {
      return Optional.empty();
    }

    final List<Choice> exact = choices.stream()
        .filter(choice -> choice.quality() == quality)
        .toList();
    final List<Choice> pool = exact.isEmpty() ? nearest(choices, quality) : exact;

    return pool.stream().min(preference(container));
  }

  private static List<Format> playable(final List<Format> formats) {
    if (formats == null) {
      return List.of();
    }

    return formats.stream()
        .filter(format -> format.url() != null && !format.url().isBlank())
        .toList();
  }

  private static List<Format> videos(final List<Format> formats, final YtVideoType required) {
    final List<Format> video = playable(formats).stream().filter(Format::hasVideo).toList();
    final List<Format> matching = video.stream()
        .filter(format -> YtVideoType.find(format.mimeType()) == required)
        .toList();

    return matching.isEmpty() ? video : matching;
  }

  private static List<Format> unique(
      final List<Format> formats,
      final Function<Format, String> key) {
    return formats.stream()
        .collect(Collectors.toMap(
            key, Function.identity(), YoutubeVariants::higherBitrate, LinkedHashMap::new))
        .values().stream()
        .toList();
  }

  private static Choice included(final Format video) {
    return new Choice(pictureLine(video), soundtrackLabel(video), video, null);
  }

  private static Choice joined(final Format video, final Format audio) {
    return new Choice(pictureLine(video), soundtrackLabel(audio), video, audio);
  }

  private static String pictureLine(final Format video) {
    return pictureLabel(video, videoFamily(video), YtVideoType.find(video.mimeType()));
  }

  private static String includedKey(final Format video) {
    return pictureKey(video) + '|' + audioCodec(video).orElse("")
        + '|' + trackName(video) + '|' + video.isDrc();
  }

  private static String trackName(final Format format) {
    return format.audioTrackName() == null ? "" : format.audioTrackName();
  }

  private static List<Choice> nearest(final List<Choice> choices, final YtQuality quality) {
    final int target = quality.getLines();
    final OptionalInt below = choices.stream()
        .mapToInt(choice -> choice.quality().getLines())
        .filter(lines -> lines > 0 && lines < target)
        .max();
    if (below.isPresent()) {
      return atLines(choices, below.getAsInt());
    }

    final OptionalInt above = choices.stream()
        .mapToInt(choice -> choice.quality().getLines())
        .filter(lines -> lines > target)
        .min();
    if (above.isPresent()) {
      return atLines(choices, above.getAsInt());
    }

    return choices;
  }

  private static List<Choice> atLines(final List<Choice> choices, final int lines) {
    return choices.stream()
        .filter(choice -> choice.quality().getLines() == lines)
        .toList();
  }

  private static Comparator<Choice> preference(final YtVideoType container) {
    return Comparator
        .comparingInt((Choice choice) -> choice.container() == container ? 0 : 1)
        .thenComparingInt(choice -> videoRank(videoFamily(choice.video())))
        .thenComparing(Choice::hdr)
        .thenComparingInt(choice -> choice.frameRate() > 30 ? 1 : 0)
        .thenComparingInt(choice -> audioRank(choice.soundtrackCodec()))
        .thenComparing(Comparator.comparingInt((Choice choice) -> bitrate(choice.video()))
            .reversed());
  }

  private static String pictureKey(final Format format) {
    return qualityText(format).toLowerCase(Locale.ROOT)
        + '|' + YtVideoType.find(format.mimeType()).name()
        + '|' + videoFamily(format);
  }

  private static String pictureLabel(
      final Format sample,
      final String family,
      final YtVideoType container) {
    final String codec = videoFamilyName(family);
    final String base = qualityText(sample) + " · " + containerName(container);

    return codec.isEmpty() ? base : base + " · " + codec;
  }

  private static String qualityText(final Format format) {
    if (format.qualityLabel() != null && !format.qualityLabel().isBlank()) {
      return format.qualityLabel();
    }
    if (format.height() > 0) {
      return format.height() + "p";
    }

    return "video";
  }

  private static int frameRate(final Format format) {
    final Matcher matcher = FRAME_RATE.matcher(qualityText(format));
    if (matcher.find()) {
      return Integer.parseInt(matcher.group(1));
    }

    return format.fps() > 0 ? format.fps() : 30;
  }

  private static Format higherBitrate(final Format left, final Format right) {
    return bitrate(left) >= bitrate(right) ? left : right;
  }

  private static String soundtrackLabel(final Format format) {
    if (audioCodec(format).isEmpty()) {
      return "No soundtrack";
    }

    final List<String> parts = new ArrayList<>();
    parts.add(audioName(audioCodec(format).orElse("")));
    final int bits = bitrate(format);
    if (bits > 0) {
      parts.add((bits / 1000) + " kbps");
    }
    if (format.audioChannels() == 1) {
      parts.add("mono");
    } else if (format.audioChannels() == 2) {
      parts.add("stereo");
    } else if (format.audioChannels() == 6) {
      parts.add("5.1");
    } else if (format.audioChannels() > 0) {
      parts.add(format.audioChannels() + " ch");
    }
    if (format.audioTrackName() != null && !format.audioTrackName().isBlank()) {
      parts.add(format.audioTrackName());
    }
    if (format.isDrc()) {
      parts.add("DRC");
    }

    return String.join(" · ", parts);
  }

  private static int bitrate(final Format format) {
    return format.averageBitrate() > 0 ? format.averageBitrate() : format.bitrate();
  }

  private static List<String> tokens(final Format format) {
    final String codecs = format.codecs();
    if (codecs == null || codecs.isBlank()) {
      return List.of();
    }

    return List.of(codecs.split(",")).stream()
        .map(String::strip)
        .filter(token -> !token.isEmpty())
        .toList();
  }

  private static Optional<String> audioCodec(final Format format) {
    return tokens(format).stream().filter(YoutubeVariants::isAudioCodec).findFirst();
  }

  private static Optional<String> videoCodec(final Format format) {
    return tokens(format).stream().filter(YoutubeVariants::isVideoCodec).findFirst();
  }

  private static boolean isAudioCodec(final String token) {
    final String lower = token.toLowerCase(Locale.ROOT);

    return lower.startsWith("mp4a")
        || lower.startsWith("opus")
        || lower.startsWith("vorbis")
        || lower.startsWith("mp3")
        || lower.startsWith("ac-3")
        || lower.startsWith("ec-3")
        || lower.startsWith("ac3")
        || lower.startsWith("eac3")
        || lower.startsWith("dts")
        || lower.startsWith("flac")
        || lower.startsWith("alac");
  }

  private static boolean isVideoCodec(final String token) {
    final String lower = token.toLowerCase(Locale.ROOT);

    return lower.startsWith("avc1")
        || lower.startsWith("avc3")
        || lower.startsWith("vp9")
        || lower.startsWith("vp09")
        || lower.startsWith("vp8")
        || lower.startsWith("av01")
        || lower.startsWith("hev1")
        || lower.startsWith("hvc1");
  }

  private static String videoFamily(final Format format) {
    return videoCodec(format).map(YoutubeVariants::familyOf).orElse("");
  }

  private static String familyOf(final String codec) {
    final String lower = codec.toLowerCase(Locale.ROOT);
    if (lower.startsWith("avc1") || lower.startsWith("avc3")) {
      return "avc";
    }
    if (lower.startsWith("vp8")) {
      return "vp8";
    }
    if (lower.startsWith("vp9") || lower.startsWith("vp09")) {
      return "vp9";
    }
    if (lower.startsWith("av01")) {
      return "av1";
    }
    if (lower.startsWith("hev1") || lower.startsWith("hvc1")) {
      return "hevc";
    }

    return lower;
  }

  private static int videoRank(final String family) {
    return switch (family) {
      case "avc" -> 0;
      case "vp8" -> 1;
      case "vp9" -> 2;
      case "hevc" -> 3;
      case "av1" -> 4;
      default -> 5;
    };
  }

  private static String videoFamilyName(final String family) {
    return switch (family) {
      case "avc" -> "AVC";
      case "vp8" -> "VP8";
      case "vp9" -> "VP9";
      case "av1" -> "AV1";
      case "hevc" -> "HEVC";
      default -> "";
    };
  }

  private static String containerName(final YtVideoType container) {
    return switch (container) {
      case MP4 -> "MP4";
      case WEBM -> "WebM";
      case GPP3 -> "3GPP";
      case UNKNOWN -> "video";
    };
  }

  private static int audioRank(final String codec) {
    final String lower = codec.toLowerCase(Locale.ROOT);
    if (lower.startsWith("mp4a.40.2") && !lower.startsWith("mp4a.40.29")) {
      return 0;
    }
    if (lower.startsWith("mp4a.40.5") || lower.startsWith("mp4a.40.29")) {
      return 1;
    }
    if (lower.startsWith("mp4a") || lower.startsWith("mp3")) {
      return 2;
    }
    if (lower.startsWith("vorbis")) {
      return 4;
    }
    if (lower.startsWith("opus")) {
      return 6;
    }
    if (lower.startsWith("ac-3") || lower.startsWith("ac3")) {
      return 7;
    }
    if (lower.startsWith("ec-3") || lower.startsWith("eac3")) {
      return 8;
    }
    if (lower.startsWith("dts")) {
      return 9;
    }
    if (lower.isEmpty()) {
      return 100;
    }

    return 10;
  }

  private static String audioName(final String codec) {
    final String lower = codec.toLowerCase(Locale.ROOT);
    if (lower.startsWith("mp4a.40.5") || lower.startsWith("mp4a.40.29")) {
      return "HE-AAC";
    }
    if (lower.startsWith("mp4a")) {
      return "AAC";
    }
    if (lower.startsWith("opus")) {
      return "Opus";
    }
    if (lower.startsWith("vorbis")) {
      return "Vorbis";
    }
    if (lower.startsWith("mp3")) {
      return "MP3";
    }
    if (lower.startsWith("ec-3") || lower.startsWith("eac3")) {
      return "E-AC-3";
    }
    if (lower.startsWith("ac-3") || lower.startsWith("ac3")) {
      return "AC-3";
    }
    if (lower.startsWith("dts")) {
      return "DTS";
    }
    if (lower.isEmpty()) {
      return "No soundtrack";
    }

    return codec;
  }

  public static final class Choice {

    private final String picture;
    private final String soundtrack;
    private final Format video;
    private final Format audio;

    private Choice(
        final String picture,
        final String soundtrack,
        final Format video,
        final Format audio) {
      this.picture = picture;
      this.soundtrack = soundtrack;
      this.video = video;
      this.audio = audio;
    }

    public Format video() {
      return this.video;
    }

    public Optional<Format> audio() {
      return Optional.ofNullable(this.audio);
    }

    public String pictureLine() {
      return this.picture;
    }

    public String soundtrackLine() {
      return this.soundtrack;
    }

    private YtQuality quality() {
      return YtQuality.find(qualityText(this.video));
    }

    private int frameRate() {
      return YoutubeVariants.frameRate(this.video);
    }

    private YtVideoType container() {
      return YtVideoType.find(this.video.mimeType());
    }

    private boolean hdr() {
      return qualityText(this.video).toLowerCase(Locale.ROOT).contains("hdr");
    }

    private String soundtrackCodec() {
      return audioCodec(this.audio == null ? this.video : this.audio).orElse("");
    }

    @Override
    public String toString() {
      return this.audio == null
          ? this.picture + " · " + this.soundtrack
          : this.picture + " + " + this.soundtrack;
    }
  }
}
