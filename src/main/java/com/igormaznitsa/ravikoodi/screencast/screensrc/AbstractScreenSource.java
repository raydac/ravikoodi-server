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

import com.igormaznitsa.ravikoodi.Utils;
import java.awt.GraphicsDevice;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public abstract class AbstractScreenSource {

  protected static final Image MOUSE_ICON = Utils.loadImage("64_mouse_pointer.png");

  private final AtomicBoolean disposed = new AtomicBoolean();

  private final boolean showPointer;
  
  public AbstractScreenSource(final boolean showPointer) {
    this.showPointer = showPointer;
  }
  
  public boolean isShowPointer() {
    return this.showPointer;
  }
  
  @NonNull
  public abstract GraphicsDevice getSourceDevice();

  @NonNull
  public Point getPointer() {
    Point result = null;
    final GraphicsDevice device = this.getSourceDevice();
    final PointerInfo info = MouseInfo.getPointerInfo();
    if (device.getIDstring().equals(info.getDevice().getIDstring())) {
      result = info.getLocation();
    }
    if (result == null) {
      final Rectangle bounds = this.getBounds();
      result = new Point(bounds.width, bounds.height);
    }
    return result;
  }

  public abstract Rectangle getBounds();

  @Nullable
  public abstract byte[] grabRgb();

  public final void dispose() {
    if (this.disposed.compareAndSet(false, true)) {
      this.onDispose();
    }
  }

  public final boolean isDisposed() {
    return this.disposed.get();
  }

  protected void onDispose() {

  }
}
