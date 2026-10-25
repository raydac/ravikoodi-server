package com.igormaznitsa.ravikoodi;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.SystemFlavorMap;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class DroppedLink {

  static final DataFlavor URI_LIST = flavor("text/uri-list;class=java.io.InputStream");
  static final DataFlavor MOZ_URL = flavor("text/x-moz-url;class=java.io.InputStream");
  static final DataFlavor HTML = flavor("text/html;class=java.io.InputStream");
  static final DataFlavor PLAIN_UTF8 = flavor("text/plain;charset=utf-8;class=java.lang.String");
  private static final Logger LOGGER = LoggerFactory.getLogger(DroppedLink.class);
  private static final Pattern HREF = Pattern.compile("(?i)href\\s*=\\s*[\"']([^\"']+)[\"']");
  private static final Pattern URL = Pattern.compile("(?i)\\b[a-z][a-z0-9+.-]*://[^\\s<>\"']+");
  private static final DataFlavor[] PREFERRED = {
      DataFlavor.stringFlavor,
      PLAIN_UTF8,
      URI_LIST,
      MOZ_URL,
      HTML
  };

  private DroppedLink() {
  }

  static void registerBrowserFlavors() {
    final SystemFlavorMap map = (SystemFlavorMap) SystemFlavorMap.getDefaultFlavorMap();
    register(map, "text/uri-list", URI_LIST);
    register(map, "text/x-moz-url", MOZ_URL);
    register(map, "text/html", HTML);
    register(map, "text/plain;charset=utf-8", PLAIN_UTF8);
    register(map, "text/plain", DataFlavor.stringFlavor);
    register(map, "UTF8_STRING", DataFlavor.stringFlavor);
    register(map, "STRING", DataFlavor.stringFlavor);
  }

  static DropTargetListener listener(final Consumer<DropTargetDropEvent> onDrop) {
    return new DropTargetAdapter() {
      @Override
      public void dragEnter(final DropTargetDragEvent event) {
        LOGGER.info("Drag entered: action={}, sourceActions={}", event.getDropAction(),
            event.getSourceActions());
        event.acceptDrag(DnDConstants.ACTION_COPY);
      }

      @Override
      public void dragOver(final DropTargetDragEvent event) {
        event.acceptDrag(DnDConstants.ACTION_COPY);
      }

      @Override
      public void dropActionChanged(final DropTargetDragEvent event) {
        event.acceptDrag(DnDConstants.ACTION_COPY);
      }

      @Override
      public void drop(final DropTargetDropEvent event) {
        onDrop.accept(event);
      }
    };
  }

  static Optional<String> findUrl(final Transferable transferable) {
    for (final DataFlavor flavor : PREFERRED) {
      final Optional<String> url = readUrl(transferable, flavor);
      if (url.isPresent()) {
        return url;
      }
    }
    for (final DataFlavor flavor : transferable.getTransferDataFlavors()) {
      if (DataFlavor.javaFileListFlavor.equals(flavor) || isPreferred(flavor)) {
        continue;
      }
      final Optional<String> url = readUrl(transferable, flavor);
      if (url.isPresent()) {
        return url;
      }
    }
    return Optional.empty();
  }

  static Optional<String> findUrlInText(final String raw) {
    if (raw == null || raw.isBlank()) {
      return Optional.empty();
    }

    final String text = raw.replace('\u0000', ' ');
    final Matcher href = HREF.matcher(text);
    while (href.find()) {
      final Optional<String> url = normalize(href.group(1));
      if (url.isPresent()) {
        return url;
      }
    }

    final Matcher matcher = URL.matcher(text);
    if (matcher.find()) {
      return Optional.of(stripTrailingPunctuation(matcher.group()));
    }

    final String firstLine = text.lines()
        .map(String::strip)
        .filter(line -> !line.isEmpty() && !line.startsWith("#"))
        .findFirst()
        .orElse("");
    if (firstLine.startsWith("www.")) {
      return Optional.of("https://" + stripTrailingPunctuation(firstLine));
    }
    if (!firstLine.contains(" ") && firstLine.contains(".")) {
      return Optional.of("https://" + stripTrailingPunctuation(firstLine));
    }
    return Optional.empty();
  }

  private static Optional<String> readUrl(final Transferable transferable,
                                          final DataFlavor flavor) {
    if (!transferable.isDataFlavorSupported(flavor)) {
      return Optional.empty();
    }
    try {
      return findUrlInText(readText(transferable.getTransferData(flavor)));
    } catch (final UnsupportedFlavorException | IOException ex) {
      LOGGER.debug("Drop flavor {} is unreadable: {}", flavor.getMimeType(), ex.toString());
      return Optional.empty();
    }
  }

  private static String readText(final Object data) throws IOException {
    if (data instanceof String text) {
      return text;
    }
    if (data instanceof InputStream stream) {
      try (InputStream in = stream) {
        return decode(in.readAllBytes());
      }
    }
    if (data instanceof byte[] bytes) {
      return decode(bytes);
    }
    return data == null ? "" : data.toString();
  }

  private static String decode(final byte[] bytes) {
    if (bytes.length >= 2 && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xFE) {
      return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
    }
    if (bytes.length >= 2 && bytes[0] == (byte) 0xFE && bytes[1] == (byte) 0xFF) {
      return new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
    }
    if (looksLikeUtf16Le(bytes)) {
      return new String(bytes, StandardCharsets.UTF_16LE);
    }
    return new String(bytes, StandardCharsets.UTF_8);
  }

  private static boolean looksLikeUtf16Le(final byte[] bytes) {
    if (bytes.length < 8 || bytes.length % 2 != 0) {
      return false;
    }
    int zeros = 0;
    final int samples = Math.min(bytes.length, 32);
    for (int i = 1; i < samples; i += 2) {
      if (bytes[i] == 0) {
        zeros++;
      }
    }
    return zeros >= 3;
  }

  private static Optional<String> normalize(final String value) {
    final String cleaned = stripTrailingPunctuation(value.strip());
    if (cleaned.contains("://") || cleaned.startsWith("www.")) {
      return Optional.of(cleaned.startsWith("www.") ? "https://" + cleaned : cleaned);
    }
    return Optional.empty();
  }

  private static String stripTrailingPunctuation(final String value) {
    int end = value.length();
    while (end > 0 && ".,);>\"'".indexOf(value.charAt(end - 1)) >= 0) {
      end--;
    }
    return value.substring(0, end);
  }

  private static boolean isPreferred(final DataFlavor flavor) {
    for (final DataFlavor preferred : PREFERRED) {
      if (preferred.equals(flavor)) {
        return true;
      }
    }
    return false;
  }

  private static void register(final SystemFlavorMap map, final String nativeName,
                               final DataFlavor flavor) {
    map.addFlavorForUnencodedNative(nativeName, flavor);
    map.addUnencodedNativeForFlavor(flavor, nativeName);
  }

  private static DataFlavor flavor(final String mime) {
    try {
      return new DataFlavor(mime);
    } catch (final ClassNotFoundException ex) {
      throw new IllegalStateException(ex);
    }
  }
}
