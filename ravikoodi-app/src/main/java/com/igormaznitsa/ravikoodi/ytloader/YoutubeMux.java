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
import java.io.InterruptedIOException;
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

  public static Optional<String> unavailable(final String ffmpeg) {
    final String command = ffmpeg == null ? "" : ffmpeg.strip();
    if (command.isEmpty()) {
      return Optional.of(explain("FFmpeg is not installed.", command));
    }

    final Path file = Path.of(command);
    final boolean direct = file.isAbsolute()
        || command.indexOf('/') >= 0
        || command.indexOf('\\') >= 0;
    if (direct && !Files.isRegularFile(file)) {
      return Optional.of(explain("FFmpeg is not installed.", command));
    }
    if (direct && !Files.isExecutable(file)) {
      return Optional.of(explain("FFmpeg is not allowed to run.", command));
    }

    final Process process;
    try {
      process = new ProcessBuilder(command, "-hide_banner", "-version")
          .redirectOutput(ProcessBuilder.Redirect.DISCARD)
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start();
    } catch (IOException ex) {
      final String detail = ex.getMessage() == null
          ? ""
          : ex.getMessage().toLowerCase(Locale.ROOT);
      final String reason = detail.contains("permission") || detail.contains("error=13")
          ? "FFmpeg is not allowed to run."
          : "FFmpeg is not installed.";
      LOGGER.info("FFmpeg command '{}' cannot start: {}", command, ex.toString());

      return Optional.of(explain(reason, command));
    }

    try {
      if (!process.waitFor(3, TimeUnit.SECONDS) || process.exitValue() != 0) {
        process.destroyForcibly();
        LOGGER.info("FFmpeg command '{}' did not report its version", command);

        return Optional.of(explain("FFmpeg is not installed.", command));
      }
    } catch (InterruptedException ex) {
      process.destroyForcibly();
      Thread.currentThread().interrupt();

      return Optional.of(explain("FFmpeg is not installed.", command));
    }

    return Optional.empty();
  }

  private static String explain(final String reason, final String command) {
    final String current = command.isEmpty() ? "" : "\n\nCurrent command: " + command;

    return reason
        + "\n\nJoining a picture and a soundtrack needs FFmpeg."
        + "\nInstall it from https://ffmpeg.org/download.html"
        + "\nand set FFmpeg command in Options."
        + current;
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
          progress.accept(Status.one("Merging video and audio, " + matcher.group(1)));
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

  public record Status(String title, Line video, Line audio) {

    static Status one(final String title) {
      return new Status(title, new Line("", -1), null);
    }

    public record Line(String text, int percent) {
    }
  }

  public static final class Join {

    private final AtomicReference<Thread> worker = new AtomicReference<>();
    private final AtomicReference<Thread> videoThread = new AtomicReference<>();
    private final AtomicReference<Thread> audioThread = new AtomicReference<>();
    private final AtomicReference<Process> process = new AtomicReference<>();
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final Object progressLock = new Object();
    private Status.Line videoLine = new Status.Line("Video", -1);
    private Status.Line audioLine = new Status.Line("Audio", -1);
    private long videoReportedAt;
    private long audioReportedAt;
    private long reportedAt;

    private static void await(final Thread thread) {
      while (thread.isAlive()) {
        try {
          thread.join(200);
        } catch (InterruptedException ex) {
          thread.interrupt();
          Thread.interrupted();
        }
      }
    }

    private static void interrupt(final AtomicReference<Thread> slot) {
      final Thread thread = slot.get();
      if (thread != null) {
        thread.interrupt();
      }
    }

    Optional<Path> execute(final List<String> args, final Path output) throws IOException {
      return this.execute(args, output, status -> {
      });
    }

    private static Throwable failure(final Throwable video, final Throwable audio) {
      if (video != null && !causedByStop(video)) {
        return video;
      }
      if (audio != null && !causedByStop(audio)) {
        return audio;
      }
      if (video == null && audio == null) {
        return null;
      }

      return new IOException("Can't download the YouTube stream");
    }

    private static boolean causedByStop(final Throwable error) {
      Throwable current = error;
      while (current != null) {
        if (current instanceof InterruptedException || current instanceof InterruptedIOException) {
          return true;
        }
        final String message = current.getMessage();
        if (message != null && message.toLowerCase(Locale.ROOT).contains("download cancelled")) {
          return true;
        }
        current = current.getCause();
      }

      return false;
    }

    public void cancel() {
      this.cancelled.set(true);
      interrupt(this.worker);
      interrupt(this.videoThread);
      interrupt(this.audioThread);
      final Process ffmpeg = this.process.get();
      if (ffmpeg != null) {
        ffmpeg.destroyForcibly();
      }
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
      final AtomicReference<Throwable> videoFailure = new AtomicReference<>();
      final AtomicReference<Throwable> audioFailure = new AtomicReference<>();
      try {
        this.publish("video", "Waiting", -1, true, progress);
        this.publish("audio", "Waiting", -1, true, progress);
        final Thread pictures = this.spawn(
            "youtube-video", this.videoThread, videoFailure,
            () -> this.download(loader, video, picture, "video", progress));
        final Thread sounds = this.spawn(
            "youtube-audio", this.audioThread, audioFailure,
            () -> this.download(loader, audio, sound, "audio", progress));
        await(pictures);
        await(sounds);
        if (this.cancelled.get()) {
          LOGGER.info("Cancelled YouTube download of {}", videoId);
          return Optional.empty();
        }
        final Throwable failed = failure(videoFailure.get(), audioFailure.get());
        if (failed != null) {
          throw failed instanceof IOException io
              ? io
              : new IOException(failed.getMessage() == null
              ? "Can't download the YouTube stream"
              : failed.getMessage(), failed);
        }
        this.report(progress, Status.one("Merging video and audio"), true);
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

    private Thread spawn(
        final String name,
        final AtomicReference<Thread> slot,
        final AtomicReference<Throwable> failure,
        final Runnable body) {
      final Thread thread = new Thread(() -> {
        slot.set(Thread.currentThread());
        if (this.cancelled.get()) {
          return;
        }
        try {
          body.run();
        } catch (Throwable ex) {
          failure.set(ex);
          interrupt(this.videoThread);
          interrupt(this.audioThread);
        }
      }, name);
      thread.setDaemon(true);
      thread.start();

      return thread;
    }

    private void download(
        final YoutubeDownloader loader,
        final Format format,
        final Path target,
        final String label,
        final Consumer<Status> progress) {
      LOGGER.info("Downloading YouTube {} into {}", label, target);
      this.publish(label, "Starting", -1, true, progress);
      loader.download(format, target, DownloadOptions.defaults().onProgress(update ->
          this.publish(label, sizeText(update), shownPercent(update), false, progress)));
    }

    private void publish(
        final String label,
        final String detail,
        final int percent,
        final boolean force,
        final Consumer<Status> progress) {
      final String caption = "video".equals(label) ? "Video" : "Audio";
      final Status.Line line = new Status.Line(caption + "  " + detail, percent);
      final Status status;
      synchronized (this.progressLock) {
        final long now = System.nanoTime();
        final boolean video = "video".equals(label);
        if (video) {
          this.videoLine = line;
        } else {
          this.audioLine = line;
        }
        final long previous = video ? this.videoReportedAt : this.audioReportedAt;
        if (!force && now - previous < 200_000_000L) {
          return;
        }
        if (video) {
          this.videoReportedAt = now;
        } else {
          this.audioReportedAt = now;
        }
        status = new Status("Downloading video and audio", this.videoLine, this.audioLine);
      }
      progress.accept(status);
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
