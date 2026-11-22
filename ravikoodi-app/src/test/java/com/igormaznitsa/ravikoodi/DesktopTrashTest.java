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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.igormaznitsa.ravikoodi.DesktopTrash.CommandTrash;
import com.igormaznitsa.ravikoodi.DesktopTrash.DesktopMove;
import com.igormaznitsa.ravikoodi.DesktopTrash.JnaTrash;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

public class DesktopTrashTest {

  private static DesktopTrash trash(
      final boolean javaDesktopTrash,
      final DesktopMove javaDesktop,
      final JnaTrash jnaTrash,
      final CommandTrash commandTrash) {
    return new DesktopTrash(javaDesktopTrash, javaDesktop, jnaTrash, commandTrash);
  }

  private static JnaTrash countingJna(final AtomicInteger calls, final boolean available) {
    return new JnaTrash() {
      @Override
      public boolean hasTrash() {
        calls.incrementAndGet();
        return available;
      }

      @Override
      public void moveToTrash(final File file) {
        calls.incrementAndGet();
      }
    };
  }

  private static JnaTrash unavailableJna() {
    return new JnaTrash() {
      @Override
      public boolean hasTrash() {
        return false;
      }

      @Override
      public void moveToTrash(final File file) {
        throw new AssertionError("jna trash was called");
      }
    };
  }

  private static CommandTrash unusedCommand() {
    return new CommandTrash() {
      @Override
      public Optional<List<String>> commandFor(final File file) {
        throw new AssertionError("desktop command was called");
      }

      @Override
      public int run(final List<String> command) {
        throw new AssertionError("desktop command was called");
      }
    };
  }

  private static CommandTrash emptyCommand() {
    return new CommandTrash() {
      @Override
      public Optional<List<String>> commandFor(final File file) {
        return Optional.empty();
      }

      @Override
      public int run(final List<String> command) {
        throw new AssertionError("desktop command was called");
      }
    };
  }

  @Test
  public void desktopMoveIsUsedWhenThePlatformProvidesIt() throws Exception {
    final Path file = Files.createTempFile("ravikoodi-trash-", ".tmp");
    final AtomicInteger jnaCalls = new AtomicInteger();
    try {
      final boolean moved = trash(true, target -> {
        assertEquals(file.toFile(), target);
        return true;
      }, countingJna(jnaCalls, true), unusedCommand()).move(file);

      assertTrue(moved);
      assertEquals(0, jnaCalls.get());
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  public void jnaMoveIsUsedWhenTheDesktopActionIsMissing() throws Exception {
    final Path file = Files.createTempFile("ravikoodi-trash-", ".tmp");
    try {
      final boolean moved = trash(false, target -> {
        throw new AssertionError("desktop trash was called");
      }, new JnaTrash() {
        @Override
        public boolean hasTrash() {
          return true;
        }

        @Override
        public void moveToTrash(final File target) throws IOException {
          assertEquals(file.toFile(), target);
          Files.delete(file);
        }
      }, unusedCommand()).move(file);

      assertTrue(moved);
      assertFalse(Files.exists(file));
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  public void sessionTrashIsUsedWhenJnaHasNoTrash() throws Exception {
    final Path file = Files.createTempFile("ravikoodi-trash-", ".tmp");
    try {
      final boolean moved = trash(false, target -> true, unavailableJna(), new CommandTrash() {
        @Override
        public Optional<List<String>> commandFor(final File target) {
          return Optional.of(List.of("gio", "trash", target.getAbsolutePath()));
        }

        @Override
        public int run(final List<String> command) throws IOException {
          assertEquals("gio", command.get(0));
          Files.delete(Path.of(command.get(command.size() - 1)));
          return 0;
        }
      }).move(file);

      assertTrue(moved);
      assertFalse(Files.exists(file));
    } finally {
      Files.deleteIfExists(file);
    }
  }

  @Test
  public void kdeUsesKioclientAndGnomeUsesGio() {
    final Set<String> both = Set.of("kioclient6", "gio");

    assertEquals(
        List.of("kioclient6", "--noninteractive", "move", "/films/clip.mp4", "trash:/"),
        DesktopTrash.desktopCommand(true, true, both::contains, "/films/clip.mp4").orElseThrow());
    assertEquals(
        List.of("gio", "trash", "/films/clip.mp4"),
        DesktopTrash.desktopCommand(true, false, both::contains, "/films/clip.mp4").orElseThrow());
  }

  @Test
  public void kdeUsesKioclient5WhenThatIsTheInstalledClient() {
    assertEquals(
        List.of("kioclient5", "--noninteractive", "move", "/films/clip.mp4", "trash:/"),
        DesktopTrash.desktopCommand(
                true, true, Set.of("kioclient5", "kioclient", "gio")::contains, "/films/clip.mp4")
            .orElseThrow());
  }

  @Test
  public void kdeWithoutKioclientUsesGio() {
    assertEquals(
        List.of("gio", "trash", "/films/clip.mp4"),
        DesktopTrash.desktopCommand(true, true, Set.of("gio")::contains, "/films/clip.mp4")
            .orElseThrow());
  }

  @Test
  public void plasmaSessionIsKde() {
    assertTrue(DesktopTrash.kdeSession(Map.of(
        "XDG_CURRENT_DESKTOP", "KDE",
        "XDG_SESSION_DESKTOP", "plasma")));
    assertFalse(DesktopTrash.kdeSession(Map.of(
        "XDG_CURRENT_DESKTOP", "ubuntu:GNOME",
        "DESKTOP_SESSION", "ubuntu-xorg")));
  }

  @Test
  public void missingTrashToolsAreReported() {
    final UnsupportedOperationException failure = assertThrows(UnsupportedOperationException.class,
        () -> trash(false, target -> true, unavailableJna(), emptyCommand())
            .move(Path.of("clip.mp4")));

    assertEquals("Move to trash is not supported on this system", failure.getMessage());
  }

  @Test
  public void jnaFailureKeepsTheFileAndReportsTheError() throws Exception {
    final Path file = Files.createTempFile("ravikoodi-trash-", ".tmp");
    try {
      final IllegalStateException failure = assertThrows(IllegalStateException.class, () ->
          trash(false, target -> true, new JnaTrash() {
            @Override
            public boolean hasTrash() {
              return true;
            }

            @Override
            public void moveToTrash(final File target) throws IOException {
              throw new IOException("trash folder is not writable");
            }
          }, unusedCommand()).move(file));

      assertEquals("trash folder is not writable", failure.getMessage());
      assertTrue(Files.exists(file));
    } finally {
      Files.deleteIfExists(file);
    }
  }
}
