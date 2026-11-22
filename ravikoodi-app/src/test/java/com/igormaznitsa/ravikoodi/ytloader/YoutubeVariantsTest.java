package com.igormaznitsa.ravikoodi.ytloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.kiulian.downloader.model.videos.formats.Format;
import com.github.kiulian.downloader.model.videos.quality.AudioQuality;
import com.igormaznitsa.ravikoodi.ytloader.YoutubeVariants.Choice;
import java.util.List;
import org.junit.Test;

public class YoutubeVariantsTest {

  private static Format video(
      final int itag,
      final String mime,
      final String quality,
      final int height,
      final int audioChannels,
      final int bitrate) {
    final Format.Builder builder = Format.builder(itag, "https://example.test/" + itag)
        .mimeType(mime)
        .qualityLabel(quality)
        .height(height)
        .averageBitrate(bitrate);
    if (audioChannels > 0) {
      builder.audioChannels(audioChannels).audioQuality(AudioQuality.medium);
    }

    return builder.build();
  }

  private static Format audio(
      final int itag,
      final String mime,
      final int audioChannels,
      final int bitrate) {
    return Format.builder(itag, "https://example.test/" + itag)
        .mimeType(mime)
        .audioChannels(audioChannels)
        .audioQuality(AudioQuality.medium)
        .averageBitrate(bitrate)
        .build();
  }

  @Test
  public void singleVideoWithSoundDoesNotNeedAChoice() {
    final List<Format> formats = List.of(
        video(18, "video/mp4; codecs=\"avc1.42001E, mp4a.40.2\"", "360p", 360, 2, 128000));
    final List<Choice> ready = YoutubeVariants.withSound(formats, YtVideoType.MP4);

    assertEquals(1, ready.size());
    assertTrue(YoutubeVariants.separate(formats, YtVideoType.MP4).isEmpty());
    assertEquals(18, ready.get(0).video().itag());
    assertTrue(ready.get(0).audio().isEmpty());
  }

  @Test
  public void defaultListKeepsOnlyVideosThatAlreadyIncludeSound() {
    final List<Format> formats = List.of(
        video(18, "video/mp4; codecs=\"avc1.42001E, mp4a.40.2\"", "360p", 360, 2, 128000),
        video(136, "video/mp4; codecs=\"avc1.4d401f\"", "720p", 720, 0, 2000000),
        video(137, "video/mp4; codecs=\"avc1.640028\"", "1080p", 1080, 0, 4000000),
        audio(140, "audio/mp4; codecs=\"mp4a.40.2\"", 2, 128000)
    );
    final List<Choice> ready = YoutubeVariants.withSound(formats, YtVideoType.MP4);

    assertEquals(1, ready.size());
    assertEquals(18, ready.get(0).video().itag());
    assertTrue(ready.get(0).toString().contains("AAC"));
    assertTrue(YoutubeVariants.separate(formats, YtVideoType.MP4).stream()
        .allMatch(choice -> choice.audio().isPresent()));
    assertTrue(YoutubeVariants.separate(formats, YtVideoType.MP4).stream()
        .noneMatch(choice -> choice.video().itag() == 18));
  }

  @Test
  public void prefersAacAtTheSettingsResolution() {
    final List<Format> formats = List.of(
        video(18, "video/mp4; codecs=\"avc1.42001E, mp4a.40.2\"", "480p", 480, 2, 128000),
        video(22, "video/mp4; codecs=\"avc1.42001E, opus\"", "480p", 480, 2, 160000),
        video(36, "video/mp4; codecs=\"avc1.4d401e, ec-3\"", "480p", 480, 6, 384000),
        video(135, "video/mp4; codecs=\"avc1.4d401e, mp4a.40.2\"", "480p", 480, 2, 1000000)
    );
    final List<Choice> ready = YoutubeVariants.withSound(formats, YtVideoType.MP4);
    final Choice preferred = YoutubeVariants.preferred(ready, YtQuality.p480, YtVideoType.MP4)
        .orElseThrow();

    assertEquals(135, preferred.video().itag());
    assertTrue(preferred.toString().contains("AAC"));
    assertFalse(preferred.toString().contains("+"));
  }

  @Test
  public void prefersAvcInsideTheRequestedContainer() {
    final List<Format> formats = List.of(
        video(401, "video/mp4; codecs=\"av01.0.08M.08, mp4a.40.2\"", "1080p", 1080, 2, 5000000),
        video(137, "video/mp4; codecs=\"avc1.640028, mp4a.40.2\"", "1080p", 1080, 2, 4000000),
        video(299, "video/mp4; codecs=\"avc1.640028, mp4a.40.2\"", "1080p60", 1080, 2, 6000000),
        video(248, "video/webm; codecs=\"vp9, opus\"", "1080p", 1080, 2, 3000000)
    );
    final List<Choice> ready = YoutubeVariants.withSound(formats, YtVideoType.MP4);
    final Choice preferred = YoutubeVariants.preferred(ready, YtQuality.p1080, YtVideoType.MP4)
        .orElseThrow();

    assertTrue(preferred.toString().startsWith("1080p · MP4 · AVC"));
    assertTrue(ready.stream().noneMatch(choice -> choice.toString().contains("WebM")));
  }

  @Test
  public void keepsOtherContainersWhenTheRequiredOneIsMissing() {
    final List<Format> formats = List.of(
        video(248, "video/webm; codecs=\"vp9, opus\"", "720p", 720, 2, 160000));
    final List<Choice> ready = YoutubeVariants.withSound(formats, YtVideoType.MP4);

    assertEquals("720p · WebM · VP9 · Opus · 160 kbps · stereo", ready.get(0).toString());
  }

  @Test
  public void separateCombinationsAreOptIn() {
    final List<Format> formats = List.of(
        video(137, "video/mp4; codecs=\"avc1.640028\"", "1080p", 1080, 0, 4000000),
        video(136, "video/mp4; codecs=\"avc1.4d401f\"", "720p", 720, 0, 2000000),
        audio(140, "audio/mp4; codecs=\"mp4a.40.2\"", 2, 128000),
        audio(251, "audio/webm; codecs=\"opus\"", 2, 160000)
    );

    assertTrue(YoutubeVariants.withSound(formats, YtVideoType.MP4).isEmpty());
    final List<Choice> separate = YoutubeVariants.separate(formats, YtVideoType.MP4);
    final Choice high = YoutubeVariants.preferred(separate, YtQuality.p1080, YtVideoType.MP4)
        .orElseThrow();

    assertEquals(4, separate.size());
    assertEquals(137, high.video().itag());
    assertEquals(140, high.audio().orElseThrow().itag());
    assertTrue(high.toString().contains("+"));
  }
}
