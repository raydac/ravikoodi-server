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
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

public class LoopbackTcpWriter extends NetConnection {

  private static final Logger LOGGER = LoggerFactory.getLogger(LoopbackTcpWriter.class);

  private final PreemptiveBuffer buffer;
  private final ServerSocket serverSocket;
  private final AtomicReference<Socket> currentSocket = new AtomicReference<>();
  private final Thread thread;
  private final AtomicBoolean disposed = new AtomicBoolean();
  private final CountDownLatch latch = new CountDownLatch(1);
  private final Supplier<Boolean> active;

  public LoopbackTcpWriter(
          @NonNull
          final String id,
          @NonNull
          final Duration dataFlowTimeout,
          final int maxQueuedItems,
          @NonNull
          final Supplier<Boolean> active
  ) throws IOException {
    super(id, dataFlowTimeout);
    this.active = Objects.requireNonNull(active);
    this.buffer = new PreemptiveBuffer(maxQueuedItems);
    this.serverSocket = new ServerSocket(0, 1, InetAddress.getLoopbackAddress());
    this.thread = new Thread(this::doWork, id + this.serverSocket.getLocalPort());
  }

  public void add(@NonNull final byte[] data) {
    this.buffer.put(data);
  }

  @NonNull
  public InetAddress getInetAddress() {
    return this.serverSocket.getInetAddress();
  }

  public int getPort() {
    return this.serverSocket.getLocalPort();
  }

  @NonNull
  public String getAddress() {
    return String.format("%s:%d", this.getInetAddress().getHostAddress(), this.getPort());
  }

  @Override
  public void start() {
    if (this.disposed.get()) {
      throw new IllegalStateException("disposed");
    } else {
      this.thread.start();
      try {
        this.latch.await();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  @Override
  public void dispose() {
    if (this.disposed.compareAndSet(false, true)) {
      final Socket socket = this.currentSocket.getAndSet(null);
      if (socket!=null){
        try{
          socket.shutdownOutput();
        }catch(IOException ex){
          LOGGER.error("IOException", ex);
        }
      }
      CloseUtil.closeQuietly(socket);
      CloseUtil.closeQuietly(this.serverSocket);
      this.thread.interrupt();
      try {
        this.thread.join();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private void doWork() {
    try {
      LOGGER.info("starting tcp-writer-thread");
      this.latch.countDown();
      while (!Thread.currentThread().isInterrupted() && !this.disposed.get()) {
        LOGGER.info("Wating incoming connection: {}:{}", this.serverSocket.getInetAddress(), this.serverSocket.getLocalPort());
        final Socket socket = this.serverSocket.accept();
        LOGGER.info("Incoming connection: {}", socket);
        this.currentSocket.set(socket);
        final OutputStream outStream = socket.getOutputStream();
        this.buffer.clear();
        this.buffer.start();
        try {
          final long watchTimeDelayMs = this.dataFlowTimeout.toMillis();
          long nextWatchTime = System.currentTimeMillis() + watchTimeDelayMs;

          while (!Thread.currentThread().isInterrupted() && !this.disposed.get() && this.active.get()) {
            final byte[] portion = this.buffer.next();
            if (portion == null) {
              if (nextWatchTime < System.currentTimeMillis()) {
                this.listeners.forEach(x -> x.onDataFlowTimeout(this, this.dataFlowTimeout));
                nextWatchTime = System.currentTimeMillis() + watchTimeDelayMs;
              }
              Thread.yield();
            } else {
              outStream.write(portion);
              outStream.flush();
              nextWatchTime = System.currentTimeMillis() + watchTimeDelayMs;
            }
          }
        } catch (IOException ex) {
          if (!this.disposed.get()) {
            LOGGER.error("Error during write", ex);
          }
        } finally {
          CloseUtil.closeQuietly(outStream);
          this.currentSocket.set(null);
          CloseUtil.closeQuietly(socket);
          this.listeners.forEach(x -> x.onCompleted(this));
        }
      }
    } catch (IOException ex) {
      if (!this.disposed.get()) {
        LOGGER.error("IOError during accept", ex);
      }
    } finally {
      CloseUtil.closeQuietly(this.serverSocket);
      LOGGER.info("tcp writer thread completed: {}", this);
    }
  }

  @Override
  public void close() throws IOException {
    if (!this.disposed.get()) {
      this.dispose();
    }
  }
}
