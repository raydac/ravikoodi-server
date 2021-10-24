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

import java.util.ArrayList;
import java.util.List;
import org.springframework.lang.Nullable;

public final class PreemptiveBuffer {

  private final List<byte[]> list;
  private final int max;
  private volatile boolean started = false;
  
  public PreemptiveBuffer(final int max) {
    this.max = max;
    this.list = new ArrayList<>(max);
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
    synchronized (this.list) {
      return this.list.size();
    }
  }

  public void clear() {
    synchronized (this.list) {
      this.list.clear();
    }
  }

  @Nullable
  public byte[] next() {
    synchronized (this.list) {
      return this.list.isEmpty() ? null : this.list.remove(0);
    }
  }

  public void put(final byte[] data) {
    synchronized (this.list) {
      if (this.started) {
        if (this.list.size() == this.max) {
          this.list.set(this.max-1, data);
        } else {
          this.list.add(data);
        }
      } else {
        if (this.list.size() == this.max) {
          this.list.remove(0);
        }
        this.list.add(data);
      }
    }
  }
}
