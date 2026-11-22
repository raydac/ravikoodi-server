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
import static java.util.Objects.requireNonNull;

import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.download.DownloadOptions;
import com.github.kiulian.downloader.download.DownloadProgress;
import com.github.kiulian.downloader.download.FileNames;
import com.github.kiulian.downloader.model.videos.formats.Format;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YoutubeMux {

  private static final Logger LOGGER = LoggerFactory.getLogger(YoutubeMux.class);
  private static final Pattern FFMPEG_TIME = Pattern.compile("time=(\\d+:\\d+:\\d+\\.\\d+)");

  private YoutubeMux() {
  }

  static Path freePath(final Path folder, final String fileName) {
    final Path directory = folder.toAbsolutePath().normalize();
    final String safe = fileName(fileName);
    Path candidate = inside(directory, safe);
    int index = 2;
    while (Files.exists(candidate)) {
      candidate = inside(directory, indexed(safe, index));
      index++;
      if (index > 10000) {
        throw new IllegalStateException("Can't find a free file name in " + directory);
      }
    }

    return candidate;
  }

  private static String fileName(final String fileName) {
    final String safe = FileNames.safe(fileName);
    final Path parsed = Path.of(safe);
    final Path name = parsed.getFileName();
    if (parsed.getNameCount() != 1
        || name == null
        || name.toString().equals(".")
        || name.toString().equals("..")) {
      return "video";
    }

    return name.toString();
  }

  private static String indexed(final String safe, final int index) {
    final int dot = safe.lastIndexOf('.');
    if (dot <= 0) {
      return safe + " (" + index + ")";
    }

    return safe.substring(0, dot) + " (" + index + ")" + safe.substring(dot);
  }

  private static Path inside(final Path directory, final String name) {
    final Path candidate = directory.resolve(name).normalize();
    final Path parent = candidate.getParent();
    if (parent == null || !parent.equals(directory)) {
      throw new IllegalStateException("Refusing a file name outside the video folder");
    }

    return candidate;
  }

  static List<String> command(
      final String ffmpeg,
      final Path video,
      final Path audio,
      final Path output) {
    return List.of(
        ffmpeg,
        "-hide_banner",
        "-nostdin",
        "-y",
        "-i",
        video.toString(),
        "-i",
        audio.toString(),
        "-map",
        "0:v:0",
        "-map",
        "1:a:0",
        "-c",
        "copy",
        "-f",
        "matroska",
        output.toString());
  }

  private static String partName(final String videoId, final String role, final Format format) {
    final String extension = format.extension() == null || format.extension().value().isBlank()
        ? "bin"
        : format.extension().value().toLowerCase(Locale.ROOT);

    return ".ravikoodi-" + videoId + "-" + role + "-" + format.itag() + "." + extension;
  }

  private static String movieName(final String title, final Format video) {
    final String quality = video.qualityLabel() != null && !video.qualityLabel().isBlank()
        ? video.qualityLabel()
        : (video.height() > 0 ? video.height() + "p" : "video");

    return title + " " + quality + ".mkv";
  }

  private static String sizeText(final DownloadProgress progress) {
    final String done = bytes(progress.bytesDone());
    if (progress.totalBytes() <= 0) {
      return done;
    }

    return done + " / " + bytes(progress.totalBytes());
  }

  private static int shownPercent(final DownloadProgress progress) {
    return progress.totalBytes() > 0 ? progress.percent() : -1;
  }

  private static String bytes(final long value) {
    if (value < 1024) {
      return value + " B";
    }
    if (value < 1024 * 1024) {
      return String.format(Locale.ROOT, "%.1f KB", value / 1024.0);
    }
    if (value < 1024L * 1024 * 1024) {
      return String.format(Locale.ROOT, "%.1f MB", value / (1024.0 * 1024));
    }

    return String.format(Locale.ROOT, "%.2f GB", value / (1024.0 * 1024 * 1024));
  }

  private static void appendTail(
      final Process process,
      final StringBuilder errors,
      final Consumer<Status> progress) {
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(process.getErrorStream(), UTF_8))) {
      String line;
      while ((line = reader.readLine()) != null) {
        if (errors.length() > 800) {
          errors.delete(0, errors.length() - 400);
        }
        errors.append(line).append('\n');
        final Matcher matcher = FFMPEG_TIME.matcher(line);
        if (matcher.find()) {
          progress.accept(new Status("Merging video and audio, " + matcher.group(1), -1));
        }
      }
    } catch (IOException ex) {
      errors.append("Can't read ffmpeg output");
    }
  }

  private static String failureText(final StringBuilder errors) {
    final String text = errors.toString().strip();
    if (text.isEmpty()) {
      return "FFmpeg could not join the picture and soundtrack";
    }

    final int last = text.lastIndexOf('\n');

    return last < 0 ? text : text.substring(last + 1);
  }

  public record Status(String text, int percent) {
  }

  public static final class Join {

    private final AtomicReference<Thread> worker = new AtomicReference<>();
    private final AtomicReference<Process> process = new AtomicReference<>();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private long reportedAt;

    public void cancel() {
      this.cancelled.set(true);
      final Thread running = this.worker.get();
      if (running != null) {
        running.interrupt();
      }
      final Process ffmpeg = this.process.get();
      if (ffmpeg != null) {
        ffmpeg.destroyForcibly();
      }
    }

    public Optional<Path> run(
        final YoutubeDownloader loader,
        final String ffmpeg,
        final Path folder,
        final String videoId,
        final String title,
        final Format video,
        final Format audio,
        final Consumer<Status> progress) throws IOException {
      requireNonNull(loader, "loader");
      requireNonNull(ffmpeg, "ffmpeg");
      requireNonNull(folder, "folder");
      requireNonNull(videoId, "videoId");
      requireNonNull(title, "title");
      requireNonNull(video, "video");
      requireNonNull(audio, "audio");
      requireNonNull(progress, "progress");

      this.worker.set(Thread.currentThread());
      if (this.cancelled.get()) {
        return Optional.empty();
      }

      Files.createDirectories(folder);
      final Path picture = freePath(folder, partName(videoId, "video", video));
      final Path sound = freePath(folder, partName(videoId, "audio", audio));
      final Path output = freePath(folder, movieName(title, video));
      picture.toFile().deleteOnExit();
      sound.toFile().deleteOnExit();
      try {
        this.download(loader, video, picture, "video", progress);
        if (this.cancelled.get()) {
          LOGGER.info("Cancelled YouTube download of {}", videoId);
          return Optional.empty();
        }
        this.download(loader, audio, sound, "audio", progress);
        if (this.cancelled.get()) {
          LOGGER.info("Cancelled YouTube download of {}", videoId);
          return Optional.empty();
        }
        this.report(progress, new Status("Merging video and audio", -1), true);
        final Optional<Path> merged = this.execute(
            command(ffmpeg, picture, sound, output), output, progress);
        if (this.cancelled.get()) {
          return Optional.empty();
        }

        return merged;
      } catch (YoutubeException.DownloadException ex) {
        if (this.cancelled.get()) {
          LOGGER.info("Cancelled YouTube download of {}", videoId);
          return Optional.empty();
        }
        final String message = ex.getMessage() == null
            ? "Can't download the YouTube stream"
            : ex.getMessage();
        throw new IOException(message, ex);
      } finally {
        Files.deleteIfExists(picture);
        Files.deleteIfExists(sound);
        if (this.cancelled.get()) {
          Files.deleteIfExists(output);
        }
      }
    }

    Optional<Path> execute(final List<String> args, final Path output) throws IOException {
      return this.execute(args, output, status -> {
      });
    }

    private void download(
        final YoutubeDownloader loader,
        final Format format,
        final Path target,
        final String label,
        final Consumer<Status> progress) {
      LOGGER.info("Downloading YouTube {} into {}", label, target);
      this.report(progress, new Status("Downloading " + label, -1), true);
      loader.download(format, target, DownloadOptions.defaults().onProgress(update ->
          this.report(progress, new Status(
              "Downloading " + label + ", " + sizeText(update), shownPercent(update)), false)));
    }

    private Optional<Path> execute(
        final List<String> args,
        final Path output,
        final Consumer<Status> progress) throws IOException {
      if (this.cancelled.get()) {
        Files.deleteIfExists(output);
        return Optional.empty();
      }

      final long startedAt = System.nanoTime();
      final Process started = new ProcessBuilder(args)
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .start();
      this.process.set(started);
      if (this.cancelled.get()) {
        started.destroyForcibly();
      }
      LOGGER.info("Merging YouTube files into {}", output);

      final StringBuilder errors = new StringBuilder();
      final Thread drain = new Thread(
          () -> appendTail(started, errors, line -> this.report(progress, line, false)),
          "youtube-mux");
      drain.setDaemon(true);
      drain.start();
      try {
        final int code = started.waitFor();
        drain.join(2000);
        final long seconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startedAt);
        if (this.cancelled.get()) {
          LOGGER.info("Cancelled YouTube merge of {} after {} s", output, seconds);
          Files.deleteIfExists(output);
          return Optional.empty();
        }
        final long size = Files.size(output);
        if (code != 0 || size == 0) {
          Files.deleteIfExists(output);
          throw new IOException(failureText(errors));
        }
        LOGGER.info("Joined YouTube video into {} ({} bytes, {} s)", output, size, seconds);

        return Optional.of(output);
      } catch (InterruptedException ex) {
        started.destroyForcibly();
        Files.deleteIfExists(output);
        if (this.cancelled.get()) {
          LOGGER.info("Cancelled YouTube merge of {}", output);
          return Optional.empty();
        }
        Thread.currentThread().interrupt();
        throw new IOException("Interrupted while preparing the YouTube video", ex);
      }
    }

    private void report(
        final Consumer<Status> progress,
        final Status status,
        final boolean force) {
      final long now = System.nanoTime();
      if (!force && now - this.reportedAt < 200_000_000L) {
        return;
      }
      this.reportedAt = now;
      progress.accept(status);
    }
  }
}
