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

package com.igormaznitsa.ravikoodi;

import static java.util.Locale.ROOT;

import com.sun.jna.platform.FileUtils;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Stream;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DesktopTrash {

  private static final Logger LOGGER = LoggerFactory.getLogger(DesktopTrash.class);
  private static final long WAIT_SECONDS = 10L;
  private static final List<String> KIO_CLIENTS = List.of("kioclient6", "kioclient5", "kioclient");

  private final boolean javaDesktopTrash;
  private final DesktopMove javaDesktop;
  private final JnaTrash jnaTrash;
  private final CommandTrash commandTrash;

  DesktopTrash(
      final boolean javaDesktopTrash,
      final DesktopMove javaDesktop,
      final JnaTrash jnaTrash,
      final CommandTrash commandTrash) {
    this.javaDesktopTrash = javaDesktopTrash;
    this.javaDesktop = javaDesktop;
    this.jnaTrash = jnaTrash;
    this.commandTrash = commandTrash;
  }

  static DesktopTrash system() {
    return new DesktopTrash(
        javaDesktopTrashSupported(),
        DesktopTrash::javaDesktopMove,
        JnaTrash.platform(),
        CommandTrash.platform());
  }

  static Optional<List<String>> desktopCommand(
      final boolean linux,
      final boolean kde,
      final Predicate<String> executable,
      final String path) {
    if (!linux) {
      return Optional.empty();
    }
    if (path.startsWith("-")) {
      throw new IllegalStateException("Refusing to trash a path that looks like an option");
    }
    if (kde) {
      final Optional<String> kioClient = KIO_CLIENTS.stream().filter(executable).findFirst();
      if (kioClient.isPresent()) {
        return Optional.of(List.of(kioClient.get(), "--noninteractive", "move", path, "trash:/"));
      }
    }
    return executable.test("gio") ? Optional.of(List.of("gio", "trash", path)) : Optional.empty();
  }

  static boolean kdeSession(final Map<String, String> environment) {
    if ("true".equalsIgnoreCase(environment.get("KDE_FULL_SESSION"))) {
      return true;
    }
    return Stream.of("XDG_CURRENT_DESKTOP", "XDG_SESSION_DESKTOP", "DESKTOP_SESSION")
        .map(environment::get)
        .filter(Objects::nonNull)
        .flatMap(value -> Arrays.stream(value.split(":")))
        .map(token -> token.strip().toUpperCase(ROOT))
        .anyMatch(token -> "KDE".equals(token) || token.startsWith("PLASMA"));
  }

  private static boolean javaDesktopMove(final File file) {
    return Desktop.getDesktop().moveToTrash(file);
  }

  private static boolean javaDesktopTrashSupported() {
    return Desktop.isDesktopSupported()
        && Desktop.getDesktop().isSupported(Desktop.Action.MOVE_TO_TRASH);
  }

  private static boolean installed(final String command) {
    final String path = System.getenv("PATH");
    if (path == null || path.isBlank()) {
      return false;
    }
    return Arrays.stream(path.split(File.pathSeparator))
        .map(dir -> Path.of(dir, command))
        .anyMatch(Files::isExecutable);
  }

  private static int runCommand(final List<String> command)
      throws IOException, InterruptedException {
    final Process process = new ProcessBuilder(command)
        .redirectOutput(ProcessBuilder.Redirect.DISCARD)
        .redirectError(ProcessBuilder.Redirect.DISCARD)
        .start();
    if (!process.waitFor(WAIT_SECONDS, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      throw new IllegalStateException("Moving to trash timed out");
    }
    return process.exitValue();
  }

  boolean move(final Path file) {
    final File target = Objects.requireNonNull(file, "file must not be null").toFile();
    if (this.javaDesktopTrash) {
      return this.moveWithJavaDesktop(target);
    }
    if (this.jnaTrash.hasTrash()) {
      return this.moveWithJna(target);
    }
    LOGGER.info("JNA has no trash, using the desktop session trash for '{}'", target);
    return this.moveWithDesktopCommand(target);
  }

  private boolean moveWithJavaDesktop(final File file) {
    LOGGER.info("Moving '{}' to trash with the Java desktop", file);
    final boolean moved = this.javaDesktop.move(file);
    LOGGER.info("Java desktop trash of '{}' finished with result {}", file, moved);
    return moved;
  }

  private boolean moveWithJna(final File file) {
    LOGGER.info("Moving '{}' to trash with JNA", file);
    try {
      this.jnaTrash.moveToTrash(file);
    } catch (IOException ex) {
      throw new IllegalStateException(ex.getMessage(), ex);
    }
    final boolean removed = !file.exists();
    LOGGER.info("JNA trash of '{}' finished with the file removed: {}", file, removed);
    return removed;
  }

  private boolean moveWithDesktopCommand(final File file) {
    final List<String> command = this.commandTrash.commandFor(file).orElseThrow(this::unsupported);
    final String tool = command.get(0);
    LOGGER.info("Moving '{}' to trash with {}", file, String.join(" ", command));
    final int exitCode = this.run(tool, command);
    LOGGER.info("{} trash of '{}' finished with exit {}", tool, file, exitCode);
    if (exitCode != 0) {
      throw new IllegalStateException(tool + " trash exited with " + exitCode);
    }
    return !file.exists();
  }

  private int run(final String tool, final List<String> command) {
    try {
      return this.commandTrash.run(command);
    } catch (IOException ex) {
      throw new IllegalStateException("Can't start " + tool + " trash: " + ex.getMessage(), ex);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Moving to trash was interrupted", ex);
    }
  }

  private UnsupportedOperationException unsupported() {
    return new UnsupportedOperationException("Move to trash is not supported on this system");
  }

  @FunctionalInterface
  interface DesktopMove {
    boolean move(File file);
  }

  interface JnaTrash {
    static JnaTrash platform() {
      return new JnaTrash() {
        @Override
        public boolean hasTrash() {
          return FileUtils.getInstance().hasTrash();
        }

        @Override
        public void moveToTrash(final File file) throws IOException {
          FileUtils.getInstance().moveToTrash(file);
        }
      };
    }

    boolean hasTrash();

    void moveToTrash(File file) throws IOException;
  }

  interface CommandTrash {
    static CommandTrash platform() {
      final boolean linux = SystemUtils.IS_OS_LINUX;
      final boolean kde = kdeSession(System.getenv());
      return new CommandTrash() {
        @Override
        public Optional<List<String>> commandFor(final File file) {
          return desktopCommand(linux, kde, DesktopTrash::installed, file.getAbsolutePath());
        }

        @Override
        public int run(final List<String> command) throws IOException, InterruptedException {
          return runCommand(command);
        }
      };
    }

    Optional<List<String>> commandFor(File file);

    int run(List<String> command) throws IOException, InterruptedException;
  }
}
