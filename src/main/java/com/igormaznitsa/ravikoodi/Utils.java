package com.igormaznitsa.ravikoodi;

import java.awt.Desktop;
import java.awt.Image;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class Utils {

  private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

  private Utils() {
  }

  public static long calculateNextKodiSpeedValue(final long currentSpeed, final boolean increase) {
    final long nextValue;

    if (currentSpeed == 0) {
      nextValue = increase ? 1 : -1;
    } else if (currentSpeed < 0) {
      nextValue = 0 - Math.min(32, increase ? Math.abs(currentSpeed) >> 1 : Math.abs(currentSpeed) << 1);
    } else {
      nextValue = Math.min(32, increase ? Math.abs(currentSpeed) << 1 : Math.abs(currentSpeed) >> 1);
    }
    return nextValue;
  }

  @NonNull
  public static Image loadImage(@NonNull final String imageName) {
    try (InputStream in = Utils.class.getClassLoader().getResourceAsStream("icons/" + imageName)) {
      return ImageIO.read(in);
    } catch (Exception ex) {
      throw new Error("Can't load image : " + imageName, ex);
    }
  }

  private static String getExtension(@NonNull final String fileName) {
    final int dot = fileName.lastIndexOf('.');
    return dot < 0 ? "" : fileName.substring(dot + 1);
  }

  @NonNull
  public static String getFileExtension(@NonNull final Path path) {
    final String fileName = path.getFileName() == null ? "" : path.getFileName().toString();
    return getExtension(fileName);
  }

  @NonNull
  public static String getFileExtension(@NonNull final File file) {
    return getExtension(file.getName());
  }

  public static boolean isBlank(@Nullable final String sequence) {
      return sequence == null || sequence.length() == 0 || sequence.trim().length() == 0;
  }
  
  public static void showURLExternal(@NonNull final URL url) {
    if (Desktop.isDesktopSupported()) {
      final Desktop desktop = Desktop.getDesktop();
      if (desktop.isSupported(Desktop.Action.BROWSE)) {
        try {
          desktop.browse(url.toURI());
        } catch (Exception x) {
          LOGGER.error("Can't browse URL in Desktop", x); //NOI18N
        }
      } else if (SystemUtils.IS_OS_LINUX) {
        final Runtime runtime = Runtime.getRuntime();
        try {
          runtime.exec("xdg-open " + url); //NOI18N
        } catch (Exception e) {
          LOGGER.error("Can't browse URL under Linux", e); //NOI18N
        }
      } else if (SystemUtils.IS_OS_MAC) {
        final Runtime runtime = Runtime.getRuntime();
        try {
          runtime.exec("open " + url); //NOI18N
        } catch (Exception e) {
          LOGGER.error("Can't browse URL on MAC", e); //NOI18N
        }
      }
    }
  }

}
