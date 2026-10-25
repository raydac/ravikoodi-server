package com.igormaznitsa.ravikoodi;

import java.awt.Component;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LinuxBrowserDropSite {

  private static final Logger LOGGER = LoggerFactory.getLogger(LinuxBrowserDropSite.class);
  private static final String X_TOOLKIT = "sun.awt.X11.XToolkit";
  private static final String CONTENT_WINDOW = "Content window";
  private static long proxyValueBuffer;

  private LinuxBrowserDropSite() {
  }

  static void publish(final Window window) {
    if (SwingUtilities.isEventDispatchThread()) {
      publishNow(window);
    } else {
      SwingUtilities.invokeLater(() -> publishNow(window));
    }
  }

  static Optional<ShellContent> findContentWindow(final String tree, final String title) {
    if (tree == null || title == null || title.isEmpty()) {
      return Optional.empty();
    }

    final String[] lines = tree.split("\n");
    for (int i = 0; i < lines.length; i++) {
      if (!title.equals(windowName(lines[i]))) {
        continue;
      }

      final long shell = windowId(lines[i]);
      final int indent = idColumn(lines[i]);
      if (shell == 0 || indent < 0) {
        continue;
      }

      for (int child = i + 1; child < lines.length; child++) {
        final int childIndent = idColumn(lines[child]);
        if (childIndent < 0) {
          continue;
        }
        if (childIndent <= indent) {
          break;
        }
        if (CONTENT_WINDOW.equals(windowName(lines[child]))) {
          final long content = windowId(lines[child]);
          if (content != 0 && content != shell) {
            return Optional.of(new ShellContent(shell, content));
          }
        }
      }
    }
    return Optional.empty();
  }

  private static void publishNow(final Window window) {
    if (!isX11Toolkit() || !window.isDisplayable()) {
      return;
    }
    try {
      if (publishWithToolkit(window)) {
        return;
      }
    } catch (final ReflectiveOperationException | RuntimeException ex) {
      LOGGER.debug("X11 toolkit drop site was not published: {}", ex.toString());
    }
    publishWithXprop(window);
  }

  private static boolean isX11Toolkit() {
    return X_TOOLKIT.equals(Toolkit.getDefaultToolkit().getClass().getName());
  }

  private static boolean publishWithToolkit(final Window window)
      throws ReflectiveOperationException {
    final Field peerField = Component.class.getDeclaredField("peer");
    peerField.setAccessible(true);
    final Object peer = peerField.get(window);
    if (peer == null) {
      return false;
    }

    final Method shellWindow = peer.getClass().getMethod("getWindow");
    shellWindow.setAccessible(true);
    final long shell = (Long) shellWindow.invoke(peer);
    final Method contentWindow = contentWindowMethod(peer.getClass());
    if (contentWindow == null || shell == 0) {
      return false;
    }

    contentWindow.setAccessible(true);
    final long content = (Long) contentWindow.invoke(peer);
    if (content == 0) {
      return false;
    }
    if (content == shell) {
      return true;
    }

    pointProxyAtShell(content, shell);
    LOGGER.info("Browser drops on the content window are forwarded to the shell");
    return true;
  }

  private static Method contentWindowMethod(final Class<?> peerType) {
    try {
      return peerType.getMethod("getContentWindow");
    } catch (final NoSuchMethodException ex) {
      return null;
    }
  }

  private static void pointProxyAtShell(final long content, final long shell)
      throws ReflectiveOperationException {
    final Class<?> atoms = Class.forName("sun.awt.X11.XAtom");
    final Class<?> nativeMemory = Class.forName("sun.awt.X11.Native");
    final Method internAtom = atoms.getMethod("get", String.class);
    internAtom.setAccessible(true);
    final Object proxyAtom = internAtom.invoke(null, "XdndProxy");
    final Field windowTypeField = atoms.getField("XA_WINDOW");
    windowTypeField.setAccessible(true);
    final long windowType = windowTypeField.getLong(null);
    final Method putLong =
        nativeMemory.getDeclaredMethod("putLong", long.class, int.class, long.class);
    putLong.setAccessible(true);
    final Method setAtomData =
        atoms.getMethod("setAtomData", long.class, long.class, long.class, int.class);
    setAtomData.setAccessible(true);
    final long buffer = proxyValueBuffer(nativeMemory);

    putLong.invoke(null, buffer, 0, shell);
    setAtomData.invoke(proxyAtom, content, windowType, buffer, 1);
    setAtomData.invoke(proxyAtom, shell, windowType, buffer, 1);
  }

  private static synchronized long proxyValueBuffer(final Class<?> nativeMemory)
      throws ReflectiveOperationException {
    if (proxyValueBuffer != 0L) {
      return proxyValueBuffer;
    }
    final Method allocate = nativeMemory.getDeclaredMethod("allocateLongArray", int.class);
    allocate.setAccessible(true);
    proxyValueBuffer = (Long) allocate.invoke(null, 1);
    return proxyValueBuffer;
  }

  private static void publishWithXprop(final Window window) {
    final String title = window instanceof Frame frame ? frame.getTitle() : "";
    if (title == null || title.isEmpty()) {
      return;
    }
    try {
      final String tree = readProcess("xwininfo", "-root", "-tree");
      final Optional<ShellContent> site = findContentWindow(tree, title);
      if (site.isEmpty()) {
        LOGGER.warn("No X11 content window found for browser drops");
        return;
      }

      final ShellContent windows = site.get();
      writeProxy(windows.content(), windows.shell());
      writeProxy(windows.shell(), windows.shell());
      LOGGER.info("Browser drops on the content window are forwarded to the shell");
    } catch (final InterruptedException ex) {
      Thread.currentThread().interrupt();
      LOGGER.warn("Cannot publish the X11 drop site: {}", ex.toString());
    } catch (final IOException ex) {
      LOGGER.warn("Cannot publish the X11 drop site: {}", ex.toString());
    }
  }

  private static void writeProxy(final long window, final long proxy)
      throws IOException, InterruptedException {
    final int status = runProcess(
        "xprop",
        "-id", hex(window),
        "-f", "XdndProxy", "32x",
        "-set", "XdndProxy", hex(proxy));
    if (status != 0) {
      throw new IOException("xprop failed for " + hex(window));
    }
  }

  private static String readProcess(final String... command)
      throws IOException, InterruptedException {
    final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    try (InputStream output = process.getInputStream()) {
      final String text = new String(output.readAllBytes(), StandardCharsets.UTF_8);
      if (!process.waitFor(5, TimeUnit.SECONDS) || process.exitValue() != 0) {
        process.destroyForcibly();
        throw new IOException(command[0] + " failed");
      }
      return text;
    }
  }

  private static int runProcess(final String... command) throws IOException, InterruptedException {
    final Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    try (InputStream output = process.getInputStream()) {
      output.readAllBytes();
    }
    if (!process.waitFor(5, TimeUnit.SECONDS)) {
      process.destroyForcibly();
      return 1;
    }
    return process.exitValue();
  }

  private static String hex(final long window) {
    return "0x" + Long.toHexString(window);
  }

  private static String windowName(final String line) {
    final int start = line.indexOf('"');
    final int end = start < 0 ? -1 : line.indexOf('"', start + 1);
    return end < 0 ? "" : line.substring(start + 1, end);
  }

  private static int idColumn(final String line) {
    return line.indexOf("0x");
  }

  private static long windowId(final String line) {
    final int start = idColumn(line);
    if (start < 0) {
      return 0L;
    }

    int end = start + 2;
    while (end < line.length() && isHex(line.charAt(end))) {
      end++;
    }
    try {
      return Long.parseUnsignedLong(line.substring(start + 2, end), 16);
    } catch (final NumberFormatException ex) {
      return 0L;
    }
  }

  private static boolean isHex(final char character) {
    return (character >= '0' && character <= '9')
        || (character >= 'a' && character <= 'f')
        || (character >= 'A' && character <= 'F');
  }

  record ShellContent(long shell, long content) {
  }
}
