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
package com.igormaznitsa.ravikoodi.screencast;

import ch.qos.logback.core.util.CloseUtil;
import com.igormaznitsa.ravikoodi.ApplicationPreferences;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.LineUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

public final class FfmpegWrapper implements ScreenGrabber.ScreenGrabberListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(FfmpegWrapper.class);
  @NonNull
  private static Optional<Thread> openSound(
          @NonNull
          final JavaSoundAdapter.SoundPort theSoundPort,
          @NonNull
          final Supplier<Boolean> workingFlag,
          @NonNull
          final Optional<LoopbackTcpWriter> soundWriter
  ) throws IOException {
    try {
      LOGGER.info("Opening target sound line: {}", theSoundPort);
      final AudioFormat aformat = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, true);
      theSoundPort.start(aformat);
      LOGGER.info("Target sound line started: {}", theSoundPort);

      final LoopbackTcpWriter destinationWriter = soundWriter.get();
      final Thread soundThread = new Thread(() -> {
        LOGGER.info("Started sound port reading thread");
        final byte[] buffer = new byte[4096];
        try {
          while (!Thread.currentThread().isInterrupted() && workingFlag.get()) {
            final int len = theSoundPort.read(buffer, 0, buffer.length);
            if (len < 0) {
              LOGGER.info("Sound stream is not active anymore");
              break;
            } else if (len > 0) {
              destinationWriter.add(Arrays.copyOf(buffer, len));
            }
          }
        } finally {
          LOGGER.info("Sound grabbing thread completed");
        }
      });
      soundThread.setDaemon(true);
      return Optional.of(soundThread);
    } catch (LineUnavailableException ex) {
      LOGGER.error("Can't start grab sound, error: {}", theSoundPort.getName(), ex);
      throw new IOException("Can't open sound line", ex);
    }
  }

  private final AtomicReference<Process> externalProcess = new AtomicReference<>();
  private final AtomicReference<FfmpegNetCommunicator> communicator = new AtomicReference<>();
  private volatile boolean active = true;
  private final PreemptiveBuffer output;
  private final ApplicationPreferences preferences;
  private final Optional<JavaSoundAdapter.SoundPort> soundPort;
  private final AtomicReference<Thread> soundGrabbingThread = new AtomicReference<>();

  public FfmpegWrapper(
          final ApplicationPreferences preferences,
          final JavaSoundAdapter soundAdapter,
          final PreemptiveBuffer output
  ) {
    this.output = Objects.requireNonNull(output);
    this.preferences = Objects.requireNonNull(preferences);
    this.soundPort = soundAdapter.findSoundPort(preferences.getSoundInput());
  }


  private void closeSound() {
    final Thread grabbingThread = this.soundGrabbingThread.getAndSet(null);
    soundPort.ifPresent(p -> {
      LOGGER.info("Closing sound line: {}", p.getName());
      p.close();
    });
    if (grabbingThread != null) {
      grabbingThread.interrupt();
      try {
        grabbingThread.join();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void consumeVideoStream(final Integer length, final byte[] data) {
    this.output.put(Arrays.copyOf(data, length));
  }

  @Override
  public void onStarted(final ScreenGrabber source) {
    if (this.externalProcess.get() == null) {
      final FfmpegNetCommunicator ffmpegCom;

      try {
        final Supplier<Boolean> activityIndicator = () -> this.active;
        ffmpegCom = new FfmpegNetCommunicator(
                this.soundPort.isPresent(),
                this::consumeVideoStream, activityIndicator,
                x -> {
                  CloseUtil.closeQuietly(source);
                    this.stopStartedExternalProcess();
                }
        );
        final Optional<Thread> soundPublisherThread = this.soundPort.isPresent() ? openSound(this.soundPort.get(), activityIndicator, ffmpegCom.getSoundWriter()) : Optional.empty();

        if (this.communicator.compareAndSet(null, ffmpegCom)) {
          ffmpegCom.start();
          soundPublisherThread.ifPresent(x -> {
            x.start();
            final Thread old = this.soundGrabbingThread.getAndSet(x);
            if (old != null) {
              old.interrupt();
            }
          });
        } else {
          LOGGER.info("Detected already provided communicator, may be some racing");
          ffmpegCom.close();
          return;
        }
      } catch (Exception ex) {
        LOGGER.info("Can't start ffmpeg communicator");
        throw new RuntimeException("Can't start internal communication streams", ex);
      }

      try {
        final Process processFfmpeg = startFfmpeg(
                this.preferences.getFfmpegPath(),
                "tcp://" + ffmpegCom.getReader().getAddress(),
                ffmpegCom.getSoundWriter().orElse(null),
                ffmpegCom.getScreenWriter(),
                source.getSnapsPerSecond(),
                source.getBounds().width,
                source.getBounds().height
        );
        if (this.externalProcess.compareAndSet(null, processFfmpeg)) {
          LOGGER.info("FFmpeg process is started");
        } else {
          try {
            processFfmpeg.destroyForcibly();
          } finally {
            CloseUtil.closeQuietly(ffmpegCom);
            throw new IOException("Detected already started FFmpeg");
          }
        }
      } catch (IOException ex) {
        LOGGER.error("Can't start ffmpeg process", ex);
        throw new RuntimeException(ex.getMessage(), ex);
      }
    } else {
      LOGGER.warn("detected already started TCP server");
    }
  }

  private Process startFfmpeg(
          final String ffmpegPath,
          final String output,
          final LoopbackTcpWriter soundWriter,
          final LoopbackTcpWriter videoWriter,
          final int snapsPerSecond,
          final int width,
          final int height
  ) throws IOException {
    final List<String> args = new ArrayList<>();

    args.add(ffmpegPath);
    args.add("-loglevel");
    args.add("info");
    args.add("-nostats");
    args.add("-hide_banner");

    args.add("-fflags");
    args.add("+flush_packets+genpts");
    args.add("-use_wallclock_as_timestamps");
    args.add("1");

    args.add("-f");
    args.add("rawvideo");

    args.add("-framerate");
    args.add(Integer.toString(snapsPerSecond));

    args.add("-video_size");
    args.add(String.format("%dX%d", width, height));
    args.add("-pixel_format");
    args.add("rgb24");

    args.add("-i");
    args.add("tcp://" + videoWriter.getAddress());

    if (soundWriter != null) {
      args.add("-ac");
      args.add("2");
      args.add("-ar");
      args.add("44100");
      args.add("-f");
      args.add("s16be");

      if (Float.compare(0.0f, this.preferences.getSoundOffset()) != 0) {
        args.add("-itsoffset");
        args.add(String.format(java.util.Locale.US, "%.2f", this.preferences.getSoundOffset()));
      }

      args.add("-i");
      args.add("tcp://" + soundWriter.getAddress());

      args.add("-c:a");
      args.add("ac3_fixed");
      args.add("-b:a");
      args.add("320k");

      args.add("-af");
      args.add("aresample=async=44100:first_pts=0");
    }

    args.add("-b:v");
    args.add(String.format("%dM", this.preferences.getBandwidth()));
    args.add("-maxrate");
    args.add(String.format("%dM", this.preferences.getBandwidth() + 1));
    args.add("-bufsize");
    args.add(String.format("%dM", Math.max(1, this.preferences.getBandwidth())));

    args.add("-filter:v");
    args.add("fps=fps=" + snapsPerSecond);
    args.add("-preset:v");
    args.add(this.preferences.getSpeedProfile().getFfmpegName());
    args.add("-tune");
    args.add("zerolatency");

    args.add("-qscale:v");
    args.add("1");
    args.add("-c:v");
    args.add("libx264");
    args.add("-qmin");
    args.add("5");
    args.add("-x264-params");
    args.add("keyint=24:min-keyint=24:no-scenecut");
    args.add("-qmax");
    args.add("50");
    args.add("-pix_fmt");
    args.add("yuv420p");
    args.add("-vf");
    args.add(String.format("scale=%s", this.preferences.getQuality().getFfmpegScale()));
    args.add("-sws_flags");
    args.add("fast_bilinear");
    args.add("-pass");
    args.add("1");
    args.add("-movflags");
    args.add("+faststart");

    args.add("-map");
    args.add("0:v");
    if (soundWriter != null) {
      args.add("-map");
      args.add("1:a");
    }

    args.add("-f");
    args.add("mpegts");
    args.add(output);

    LOGGER.info("Starting FFmpeg: {}", args);

    final Process newProcess = new ProcessBuilder(args).redirectError(ProcessBuilder.Redirect.INHERIT)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .start();
    return newProcess;
  }

  @NonNull
  private byte[] extractRgb(@NonNull final BufferedImage intTypeBufferedImage) {
    final int[] imageDataBuffer = ((DataBufferInt) intTypeBufferedImage.getRaster().getDataBuffer()).getData();
    final int bufferLen = imageDataBuffer.length;

    final byte[] result = new byte[bufferLen * 3];
    int dataIndex = 0;
    for (int i = 0; i < bufferLen; i++) {
      final int argb = imageDataBuffer[i];
      result[dataIndex++] = (byte) (argb >> 16);
      result[dataIndex++] = (byte) (argb >> 8);
      result[dataIndex++] = (byte) argb;
    }
    return result;
  }

  @Override
  public void onGrabbed(final ScreenGrabber source, final BufferedImage grabbedImage) {
    final FfmpegNetCommunicator currentCommunicator = this.communicator.get();
    if (currentCommunicator != null) {
      currentCommunicator.getScreenWriter().add(extractRgb(grabbedImage));
    }
  }

  @Override
  public void onError(final ScreenGrabber source, final Throwable error) {
    LOGGER.error("Detected error", error);
    CloseUtil.closeQuietly(source);
  }

  @Override
  public void onDisposed(final ScreenGrabber source) {
    this.active = false;
    CloseUtil.closeQuietly(this.communicator.getAndSet(null));
    try {
      this.stopStartedExternalProcess();
    } finally {
      closeSound();
    }
  }

  public void stopStartedExternalProcess() {
    final Process process = this.externalProcess.getAndSet(null);
    if (process != null) {
      LOGGER.info("Destroying forcibly started ffmpeg process");
      process.destroyForcibly();
    }
  }

}
