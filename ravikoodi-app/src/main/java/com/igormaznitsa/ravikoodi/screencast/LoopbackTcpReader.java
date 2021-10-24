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
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

public class LoopbackTcpReader extends NetConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoopbackTcpReader.class);

  private final ServerSocket serverSocket;
  private final Thread thread;
  private final BiConsumer<Integer, byte[]> dataConsumer;
  private final int bufferSize;
  private final InetAddress serverAddress;
  private final int serverPort;
  private final AtomicReference<Socket> socketRef = new AtomicReference<>();
  private final CountDownLatch latch = new CountDownLatch(1);
  private final AtomicBoolean disposed = new AtomicBoolean();
  private final Supplier<Boolean> active;

  public LoopbackTcpReader(
          @NonNull
          final String id,
          @NonNull
          final Duration dataFlowTimeout,
          final int readBufferSize,
          @NonNull
          final BiConsumer<Integer, byte[]> consumer,
          @NonNull
          final Supplier<Boolean> active
  ) throws IOException {
    super(id, dataFlowTimeout);
    this.active = Objects.requireNonNull(active);
    this.serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    this.thread = new Thread(this::doWork, id + this.serverSocket.getLocalPort());
    this.thread.setDaemon(true);
    this.dataConsumer = Objects.requireNonNull(consumer);
    this.bufferSize = readBufferSize;
    this.serverAddress = this.serverSocket.getInetAddress();
    this.serverPort = this.serverSocket.getLocalPort();
  }

  @NonNull
  @Override
  public String getAddress() {
    return this.serverAddress.getHostAddress() + ':' + this.serverPort;
  }

  @Override
  public void start() {
    if (this.disposed.get()) {
      throw new IllegalStateException("Disposed");
    }
    LOGGER.info("starting on " + this.getAddress());
    this.thread.start();
    try {
      this.latch.await();
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void dispose() {
    if (this.disposed.compareAndSet(false, true)) {
      LOGGER.info("Disposing start");
      this.thread.interrupt();
      CloseUtil.closeQuietly(this.serverSocket);
      final Socket currentSocket = this.socketRef.getAndSet(null);
      if (currentSocket != null) {
        try {
          currentSocket.shutdownInput();
        } catch (IOException ex) {
          LOGGER.error("IOException", ex);
        }
      }
      CloseUtil.closeQuietly(currentSocket);
      try {
        this.thread.join();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void doWork() {
    LOGGER.info("Start work: {}", this.serverSocket);
    this.latch.countDown();
    Socket socket = null;
    try {
      socket = this.serverSocket.accept();
      this.socketRef.set(socket);
      CloseUtil.closeQuietly(this.serverSocket);

      LOGGER.info("Incomming connection: {}", socket);

      final InputStream inStream = socket.getInputStream();

      final long timeInterval = this.dataFlowTimeout.toMillis();

      final byte[] buffer = new byte[this.bufferSize];
      long maxTimeWithoutActivity = System.currentTimeMillis() + timeInterval;
      while (!Thread.currentThread().isInterrupted() && this.active.get() && !this.disposed.get()) {
        final int read = inStream.read(buffer);
        if (read < 0 || this.disposed.get()) {
          break;
        } else if (read > 0) {
          if (maxTimeWithoutActivity < System.currentTimeMillis()) {
            this.listeners.forEach(x -> x.onDataFlowTimeout(this, this.dataFlowTimeout));
          }
          this.dataConsumer.accept(read, buffer);
          maxTimeWithoutActivity = System.currentTimeMillis() + timeInterval;
        }
      }
    } catch (IOException ex) {
      if (!this.disposed.get()) {
        LOGGER.error("IOException", ex);
      }
    } finally {
      LOGGER.info("Completed work: {}", this.serverSocket);
      CloseUtil.closeQuietly(this.serverSocket);
      CloseUtil.closeQuietly(socket);
      this.listeners.forEach(x -> x.onCompleted(this));
    }
  }

  @Override
  public void close() throws IOException {
    this.dispose();
  }
}
