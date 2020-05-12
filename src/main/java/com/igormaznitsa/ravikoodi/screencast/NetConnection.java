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

import java.io.Closeable;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import org.springframework.lang.NonNull;

public abstract class NetConnection implements Closeable {

  protected final List<NetConnectionListener> listeners = new CopyOnWriteArrayList<>();

  public void addNetConectionListener(@NonNull final NetConnectionListener listener) {
    this.listeners.add(listener);
  }

  public void removeNetConnectionListener(@NonNull final NetConnectionListener listener) {
    this.listeners.remove(listener);
  }

  protected final Duration dataFlowTimeout;
  protected final String id;
  
  public NetConnection(@NonNull final String id, @NonNull final Duration dataFlowTimeout) {
    this.id = Objects.requireNonNull(id);
    this.dataFlowTimeout = Objects.requireNonNull(dataFlowTimeout);
  }
  
  @NonNull
  public String getId() {
    return this.id;
  }
  
  @Override
  @NonNull
  public String toString() {
    return this.getClass().getSimpleName()+'('+this.id+')';
  }
  
  @NonNull
  abstract String getAddress();

  abstract void start();

  abstract void dispose();
  
  public interface NetConnectionListener {
    
    void onDataFlowTimeout(@NonNull NetConnection source, @NonNull Duration timeout);
    
    void onCompleted(@NonNull NetConnection source);
  }
}
