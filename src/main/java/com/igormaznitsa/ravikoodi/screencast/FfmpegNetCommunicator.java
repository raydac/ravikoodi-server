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
import java.io.Closeable;
import java.io.IOException;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class FfmpegNetCommunicator implements Closeable, NetConnection.NetConnectionListener {

  private static final Logger LOGGER = LoggerFactory.getLogger(FfmpegNetCommunicator.class);

  private static final Duration MAX_DATAFLOW_TIMEOUT = Duration.ofSeconds(7);

  private final Optional<LoopbackTcpWriter> soundWriter;
  private final LoopbackTcpWriter screenWriter;
  private final LoopbackTcpReader resultReader;
  private final Consumer<FfmpegNetCommunicator> closeSignalConsumer;

  private final AtomicBoolean closed = new AtomicBoolean();
  private final AtomicBoolean started = new AtomicBoolean();

  public FfmpegNetCommunicator(
          final boolean supportSound,
          @NonNull
          final BiConsumer<Integer, byte[]> resultDataConsumer,
          @NonNull
          final Supplier<Boolean> activityFlagSupplier,
          @Nullable
          final Consumer<FfmpegNetCommunicator> closeSignalConsumer
  ) throws IOException {
    if (supportSound) {
      this.soundWriter = Optional.of(new LoopbackTcpWriter("tcp-sound-writer", MAX_DATAFLOW_TIMEOUT, 3, activityFlagSupplier));
    } else {
      this.soundWriter = Optional.empty();
    }
    this.closeSignalConsumer = closeSignalConsumer;
    this.screenWriter = new LoopbackTcpWriter("tcp-screen-writer", MAX_DATAFLOW_TIMEOUT, 3, activityFlagSupplier);
    this.resultReader = new LoopbackTcpReader("tcp-result-reader", MAX_DATAFLOW_TIMEOUT, 512 * 1024, resultDataConsumer, activityFlagSupplier);
  }

  @Override
  public void onDataFlowTimeout(@NonNull final NetConnection source, @NonNull final Duration timeout) {
    LOGGER.warn("Detected too long inactivity for connection: {}", source);
    CloseUtil.closeQuietly(this);
  }

  @Override
  public void onCompleted(@NonNull final NetConnection source) {
    if (source == this.screenWriter || source == this.resultReader) {
      LOGGER.info("Detected completion of important part, stopping: {}", source);
      try {
        this.close();
      } catch (IOException ex) {
        LOGGER.error("Error during close", ex);
      }
    }
  }

  @NonNull
  public LoopbackTcpReader getReader() {
    return this.resultReader;
  }

  @NonNull
  public Optional<LoopbackTcpWriter> getSoundWriter() {
    return this.soundWriter;
  }

  @NonNull
  public LoopbackTcpWriter getScreenWriter() {
    return this.screenWriter;
  }

  public void start() {
    if (this.closed.get()) {
      throw new IllegalStateException("Already closed");
    } else {
      if (this.started.compareAndSet(false, true)) {
        try {
          this.soundWriter.ifPresent(x -> {
            x.addNetConectionListener(this);
            x.start();
          });
          this.screenWriter.addNetConectionListener(this);
          this.screenWriter.start();
          this.resultReader.addNetConectionListener(this);
          this.resultReader.start();
        } catch (RuntimeException ex) {
          CloseUtil.closeQuietly(this.soundWriter.orElse(null));
          CloseUtil.closeQuietly(this.screenWriter);
          CloseUtil.closeQuietly(this.resultReader);
          throw ex;
        }
      }
    }
  }

  @Override
  public void close() throws IOException {
    if (this.closed.compareAndSet(false, true)) {
      try {
        CloseUtil.closeQuietly(this.resultReader);
        CloseUtil.closeQuietly(this.screenWriter);
        CloseUtil.closeQuietly(this.soundWriter.orElse(null));
      } finally {
        if (this.closeSignalConsumer != null) {
          this.closeSignalConsumer.accept(this);
        }
      }
    }
  }
}
