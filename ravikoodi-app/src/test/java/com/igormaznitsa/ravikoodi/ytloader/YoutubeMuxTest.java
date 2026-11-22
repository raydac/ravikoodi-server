package com.igormaznitsa.ravikoodi.ytloader;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class YoutubeMuxTest {

  @Test
  public void cancelStopsTheRunningJoin() throws Exception {
    final Path output = Files.createTempFile("ravikoodi-yt-", ".mkv");
    final YoutubeMux.Join join = new YoutubeMux.Join();
    final AtomicReference<Optional<Path>> result = new AtomicReference<>();
    final AtomicReference<Throwable> error = new AtomicReference<>();
    final Thread worker = new Thread(() -> {
      try {
        result.set(join.execute(List.of("sleep", "30"), output));
      } catch (Throwable ex) {
        error.set(ex);
      }
    });
    worker.start();
    Thread.sleep(300);
    join.cancel();
    worker.join(5000);

    assertFalse(worker.isAlive());
    assertNull(error.get());
    assertTrue(result.get().isEmpty());
    assertFalse(Files.exists(output));
  }

  @Test
  public void freePathDoesNotReplaceAnExistingFile() throws Exception {
    final Path folder = Files.createTempDirectory("ravikoodi-yt");
    final Path existing = folder.resolve("Clip 360p.mkv");
    Files.writeString(existing, "old");
    final Path next = YoutubeMux.freePath(folder, "Clip 360p.mkv");

    assertEquals(folder.resolve("Clip 360p (2).mkv"), next);
    assertEquals("old", Files.readString(existing));
  }

  @Test
  public void freePathKeepsTheNameInsideTheFolder() throws Exception {
    final Path folder = Files.createTempDirectory("ravikoodi-yt");
    final Path directory = folder.toAbsolutePath().normalize();
    final Path escaped = YoutubeMux.freePath(folder, "../../etc/passwd");
    final Path dots = YoutubeMux.freePath(folder, "..");
    final Path controls = YoutubeMux.freePath(folder, "a/b:c\u0000d*.mkv");

    assertEquals(directory, escaped.getParent());
    assertEquals(directory, dots.getParent());
    assertEquals(directory, controls.getParent());
    assertEquals("video", dots.getFileName().toString());
    assertFalse(escaped.getFileName().toString().contains("/"));
    assertFalse(controls.getFileName().toString().contains(":"));
    assertFalse(controls.getFileName().toString().contains("*"));
  }

  @Test
  public void missingFfmpegExplainsWhatIsNeeded() {
    final String message = YoutubeMux.unavailable("ravikoodi-missing-ffmpeg").orElseThrow();

    assertTrue(message.contains("not installed"));
    assertTrue(message.contains("https://ffmpeg.org/download.html"));
    assertTrue(message.contains("Options"));
    assertTrue(message.contains("ravikoodi-missing-ffmpeg"));
  }

  @Test
  public void ffmpegWithoutPermissionExplainsThatItCannotRun() throws Exception {
    final Path command = Files.createTempFile("ravikoodi-ffmpeg", ".bin");
    command.toFile().setExecutable(false);
    final String message = YoutubeMux.unavailable(command.toString()).orElseThrow();

    assertTrue(message.contains("not allowed"));
    assertTrue(message.contains("https://ffmpeg.org/download.html"));
    assertTrue(message.contains(command.toString()));
  }
}
