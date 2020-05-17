/*
 * Copyright 2020 Igor Maznitsa.
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
package com.igormaznitsa.ravikoodi.screencast.screensrc;

import ch.qos.logback.core.util.CloseUtil;
import com.igormaznitsa.ravikoodi.ApplicationPreferences;
import com.igormaznitsa.ravikoodi.screencast.LoopbackTcpReader;
import static com.igormaznitsa.ravikoodi.screencast.screensrc.AbstractScreenSource.MOUSE_ICON;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Exchanger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class FfmpegScreenSource extends AbstractScreenSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(FfmpegScreenSource.class);

  private final GraphicsDevice sourceDevice;
  private final Rectangle screenBounds;
  private final Rectangle targetSize;
  private final LoopbackTcpReader resultReader;
  private final byte[] targetRgbBuffer;
  private final Process ffmpegProcess;
  private final Exchanger<byte[]> dataExchanger = new Exchanger<>();
  private final double aspectWidth;
  private final double aspectHeight;
  private final int[] scaledMouseCursorARGB;
  private final int scaledMouseCursorWidth;
  private final int scaledMouseCursorHeight;
  private final double scaleX;
  private final double scaleY;
  private final int captureDeviceIndex;

  public FfmpegScreenSource(final ApplicationPreferences preferences, final boolean showPointer) throws IOException {
    super(showPointer);
    this.sourceDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    final AffineTransform affineTransform = this.sourceDevice.getDefaultConfiguration().getDefaultTransform();
    this.scaleX = affineTransform.getScaleX();
    this.scaleY = affineTransform.getScaleY();
    this.screenBounds = scale(this.sourceDevice.getDefaultConfiguration().getBounds(), this.scaleX, this.scaleY);
    this.targetSize = calculateTargetSize(preferences.getQuality(), this.screenBounds);
    this.aspectWidth = (double) this.targetSize.width / (double) this.screenBounds.width;
    this.aspectHeight = (double) this.targetSize.height / (double) this.screenBounds.height;
    this.targetRgbBuffer = new byte[this.targetSize.width * this.targetSize.height * 3];

    if (SystemUtils.IS_OS_MAC) {
      this.captureDeviceIndex = findMacCaptureDeviceIndex(preferences);
    } else {
      this.captureDeviceIndex = 0;
    }

    final BufferedImage scaledMouseCursor = makeScaledMousePointer(this.aspectWidth, this.aspectHeight);
    this.scaledMouseCursorWidth = scaledMouseCursor.getWidth();
    this.scaledMouseCursorHeight = scaledMouseCursor.getHeight();

    this.scaledMouseCursorARGB = Arrays.copyOf(((DataBufferInt) scaledMouseCursor.getRaster().getDataBuffer()).getData(), this.scaledMouseCursorWidth * this.scaledMouseCursorHeight);

    LoopbackTcpReader reader = null;
    try {
      reader = new LoopbackTcpReader(
              "ffmpeg-scr-capture-reader",
              Duration.ofSeconds(2),
              0x10000,
              this::consumeInData,
              () -> !this.isDisposed());
      reader.start();
      LOGGER.info("started tcp loopback reader: {}", reader.getAddress());
    } catch (IOException ex) {
      LOGGER.error("error during tcp reader start", ex);
      CloseUtil.closeQuietly(reader);
      throw new IllegalStateException("Can't start result reader", ex);
    }

    this.resultReader = reader;

    try {
      this.ffmpegProcess = startFfmpeg(
              preferences.getFfmpegPath(),
              this.sourceDevice.getIDstring(),
              preferences.getSnapsPerSecond(),
              reader,
              this.screenBounds,
              this.targetSize
      );
      LOGGER.info("started ffmpeg capturing process");
    } catch (Exception ex) {
      LOGGER.error("error during ffmpeg start", ex);
      CloseUtil.closeQuietly(this.resultReader);
      throw new IllegalStateException("Can't start ffmpeg capture", ex);
    }
    LOGGER.info("created ffmpeg grabber, device={}, bounds={}, target_size={}x{}", this.sourceDevice.getIDstring(), this.screenBounds, this.targetSize.width, this.targetSize.height);
  }

  private int findMacCaptureDeviceIndex(final ApplicationPreferences preferences) {
    final List<String> cmd = new ArrayList<>();
    cmd.add(preferences.getFfmpegPath());
    cmd.add("-nostats");
    cmd.add("-hide_banner");
    cmd.add("-list_devices");
    cmd.add("true");
    cmd.add("-f");
    cmd.add("avfoundation");
    cmd.add("-i");
    cmd.add("dummy");

    LOGGER.info("executing FFmpeg command line: {}", cmd.stream().collect(Collectors.joining(" ")));

    final Process process;
    try {
      process = new ProcessBuilder(cmd)
              .redirectError(ProcessBuilder.Redirect.PIPE)
              .redirectOutput(ProcessBuilder.Redirect.INHERIT)
              .start();
    } catch (IOException ex) {
      LOGGER.error("can't start external process", ex);
      return 0;
    }

    final String outputData;
    try {
      outputData = new String(readAllButes(process.getErrorStream()), Charset.defaultCharset());
    } catch (IOException ex) {
      LOGGER.error("can't read output data for started ffmpeg process", ex);
      return 0;
    }

    LOGGER.info("got response of process: {}", outputData);

    try {
      process.waitFor();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return 0;
    }

    final Pattern CAPTURE_SCREEN = Pattern.compile("^.*\\[(\\d+)]\\s+capture\\s+screen\\s+(\\d+)\\s*$", Pattern.CASE_INSENSITIVE);
    int foundDeviceId = -1;
    final String[] lines = outputData.split("\\r?\\n");
    LOGGER.info("avfoundation generated output {} lines", lines.length);
    for (final String s : lines) {
      final Matcher matcher = CAPTURE_SCREEN.matcher(s);
      if (matcher.find()) {
        foundDeviceId = Integer.parseInt(matcher.group(1));
        final int scrNumber = Integer.parseInt(matcher.group(2));
        LOGGER.info("detected capture screen {}:{} at '{}'", foundDeviceId, scrNumber, s);
        break;
      } else {
        LOGGER.debug("No match: {}", s);
      }
    }
    if (foundDeviceId < 0) {
      LOGGER.warn("Can't find any capture screen id among, trying use 0: {}", Arrays.toString(lines));
      return 0;
    } else {
      return foundDeviceId;
    }
  }

  @NonNull
  private static byte[] readAllButes(@NonNull final InputStream stream) throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream(16384);
    while (!Thread.currentThread().isInterrupted()) {
      final int next = stream.read();
      if (next < 0) {
        break;
      }
      buffer.write(next);
    }
    buffer.close();
    return buffer.toByteArray();
  }

  @Override
  public double getScaleX() {
    return this.scaleX;
  }

  @Override
  public double getScaleY() {
    return this.scaleY;
  }

  @NonNull
  private BufferedImage makeScaledMousePointer(final double aspectWidth, final double aspectHeight) {
    final int scaledWidth = (int) Math.round(aspectWidth * MOUSE_ICON.getWidth(null));
    final int scaledHeight = (int) Math.round(aspectHeight * MOUSE_ICON.getHeight(null));
    final BufferedImage result = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_ARGB);
    final Graphics2D gfx = result.createGraphics();
    try {
      gfx.drawImage(MOUSE_ICON, AffineTransform.getScaleInstance(aspectWidth, aspectHeight), null);
    } finally {
      gfx.dispose();
    }
    return result;
  }

  private Process startFfmpeg(
          final String ffmpegPath,
          final String deviceId,
          final int snapsPerSecond,
          final LoopbackTcpReader reader,
          final Rectangle screenBounds,
          final Rectangle targetSize
  ) throws IOException {
    final List<String> args = new ArrayList<>();

    args.add(ffmpegPath);

    args.add("-loglevel");
    args.add("error");
    args.add("-nostats");
    args.add("-hide_banner");

    args.add("-video_size");
    args.add(String.format("%dx%d", screenBounds.width, screenBounds.height));

    args.add("-framerate");
    args.add(Integer.toString(snapsPerSecond));

    args.add("-f");
    if (SystemUtils.IS_OS_MAC) {
      args.add("avfoundation");
      args.add("-i");
      args.add(Integer.toString(this.captureDeviceIndex));
    } else if (SystemUtils.IS_OS_WINDOWS) {
      args.add("gdigrab");
      args.add("-draw_mouse");
      args.add("0");
      args.add("-offset_x");
      args.add(Integer.toString(screenBounds.x));
      args.add("-offset_y");
      args.add(Integer.toString(screenBounds.y));
      args.add("-i");
      args.add("desktop");
    } else {
      args.add("x11grab");
      args.add("-i");
      args.add(String.format("%s+%d,%d", deviceId, screenBounds.x, screenBounds.y));
    }

    args.add("-s");
    args.add(String.format("%dx%d", targetSize.width, targetSize.height));
    args.add("-sws_flags");
    args.add("fast_bilinear");

    args.add("-vcodec");
    args.add("rawvideo");
    args.add("-pix_fmt");
    args.add("rgb24");

    args.add("-f");
    args.add("rawvideo");

    args.add(String.format("tcp://%s", reader.getAddress()));

    LOGGER.info("prepared ffmpeg command: {}", args.stream().collect(Collectors.joining(" ")));

    return new ProcessBuilder(args)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start();
  }

  private int rgbBufferPositon = 0;

  private void consumeInData(@NonNull final Integer length, @NonNull final byte[] data) {
    int len = length;

    final int dataToWrite = Math.min(len, this.targetRgbBuffer.length - this.rgbBufferPositon);
    System.arraycopy(data, 0, this.targetRgbBuffer, this.rgbBufferPositon, Math.min(len, dataToWrite));
    this.rgbBufferPositon += dataToWrite;
    len -= dataToWrite;
    if (this.rgbBufferPositon >= this.targetRgbBuffer.length) {
      this.rgbBufferPositon = 0;
      try {
        this.dataExchanger.exchange(Arrays.copyOf(this.targetRgbBuffer, this.targetRgbBuffer.length));
        if (len > 0) {
          System.arraycopy(data, dataToWrite, this.targetRgbBuffer, 0, len);
          this.rgbBufferPositon = len;
        }
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static Rectangle calculateTargetSize(final ApplicationPreferences.Quality quality, final Rectangle screenSize) {
    final double aspect = (double) quality.getHeight() / (double) screenSize.height;
    return new Rectangle((int) Math.round(screenSize.width * aspect) & 0xFFFFFFFE, quality.getHeight());
  }

  @Override
  @NonNull
  public GraphicsDevice getSourceDevice() {
    return this.sourceDevice;
  }

  @Override
  @NonNull
  public Rectangle getBounds() {
    return this.targetSize;
  }

  @NonNull
  public Point getPointer() {
    Point result = null;
    final GraphicsDevice device = this.getSourceDevice();
    final PointerInfo info = MouseInfo.getPointerInfo();
    if (device.getIDstring().equals(info.getDevice().getIDstring())) {
      result = scale(info.getLocation(), this.scaleX, this.scaleY);
    }
    if (result == null) {
      result = new Point(this.targetSize.width, this.targetSize.height);
    } else {
      result = new Point((int) Math.round(result.x * this.aspectWidth), (int) Math.round(result.y * this.aspectWidth));
    }
    return result;
  }

  private void drawPointer(final Point mousePoint, final byte[] rgbArray) {
    final int visibleWidth = Math.min(this.targetSize.width - mousePoint.x, this.scaledMouseCursorWidth);
    final int visibleHeight = Math.min(this.targetSize.height - mousePoint.y, this.scaledMouseCursorHeight);

    int scry = mousePoint.y;
    for (int y = 0; y < visibleHeight; y++) {
      if (scry < 0 || scry >= this.targetSize.height) {
        scry++;
        continue;
      }
      int scrx = mousePoint.x;
      for (int x = 0; x < visibleWidth; x++) {
        if (scrx < 0 || scrx >= this.targetSize.width) {
          scrx++;
          continue;
        }
        final int posAtCursor = y * this.scaledMouseCursorWidth + x;
        int targetPos = scry * this.targetSize.width * 3 + scrx * 3;

        final int argb = this.scaledMouseCursorARGB[posAtCursor];
        if (argb != 0) {
          final byte r = (byte) (argb >>> 16);
          final byte g = (byte) (argb >>> 8);
          final byte b = (byte) argb;
          rgbArray[targetPos++] = r;
          rgbArray[targetPos++] = g;
          rgbArray[targetPos] = b;
        }

        scrx++;
      }
      scry++;
    }

  }

  @Override
  @Nullable
  public byte[] grabRgb() {
    final byte[] resultRgbArray;
    try {
      resultRgbArray = this.dataExchanger.exchange(null);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      return null;
    }

    if (this.isShowPointer()) {
      this.drawPointer(this.getPointer(), resultRgbArray);
    }

    return resultRgbArray;
  }

  @Override
  protected void onDispose() {
    LOGGER.info("closing result reader");
    CloseUtil.closeQuietly(this.resultReader);
    try {
      LOGGER.info("destroying ffmpeg process");
      this.ffmpegProcess.destroyForcibly();
    } catch (Exception ex) {
      LOGGER.error("Unexpected error during ffmpeg process destroy", ex);
    }
  }

}
