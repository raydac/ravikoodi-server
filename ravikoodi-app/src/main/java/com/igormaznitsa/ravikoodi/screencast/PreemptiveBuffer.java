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

import java.util.ArrayDeque;
import java.util.Deque;
import org.jspecify.annotations.Nullable;

public final class PreemptiveBuffer {

  private final Deque<byte[]> chunks;
  private final int max;
  private volatile boolean started = false;

  public PreemptiveBuffer(final int max) {
    this.max = max;
    this.chunks = new ArrayDeque<>(max);
  }

  public boolean isStarted() {
    return this.started;
  }

  public void start() {
    this.started = true;
  }

  public void suspend() {
    this.started = false;
  }

  public int size() {
    synchronized (this.chunks) {
      return this.chunks.size();
    }
  }

  public void clear() {
    synchronized (this.chunks) {
      this.chunks.clear();
      this.chunks.notifyAll();
    }
  }

  @Nullable
  public byte[] poll(final long waitMillis) {
    synchronized (this.chunks) {
      final long deadline = System.currentTimeMillis() + Math.max(0L, waitMillis);
      while (this.chunks.isEmpty()) {
        final long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 0L) {
          return null;
        }
        try {
          this.chunks.wait(remaining);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          return null;
        }
      }
      return this.chunks.removeFirst();
    }
  }

  public void put(final byte[] data) {
    synchronized (this.chunks) {
      if (this.started) {
        if (this.chunks.size() == this.max) {
          this.chunks.removeLast();
        }
      } else if (this.chunks.size() == this.max) {
        this.chunks.removeFirst();
      }
      this.chunks.addLast(data);
      this.chunks.notifyAll();
    }
  }
}
