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

import com.igormaznitsa.ravikoodi.ApplicationPreferences;
import com.igormaznitsa.ravikoodi.Utils;
import java.awt.AWTException;
import java.awt.Cursor;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ScreenGrabber implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScreenGrabber.class);

  private static final Image MOUSE_ICON = Utils.loadImage("64_mouse_pointer.png");
  
  private final Robot robot;
  private final Rectangle screenBounds;
  private final AtomicBoolean disposed = new AtomicBoolean();
  private final AtomicReference<Thread> grabbingThread = new AtomicReference<>();
  private final int snapsPerSecond;
  private final boolean showCursor;
  private final ImageType imageType;
  private final List<ScreenGrabberListener> listeners = new CopyOnWriteArrayList<>();


  public ScreenGrabber(
          final ApplicationPreferences preferences,
          final ImageType format
  ) throws AWTException, HeadlessException {
    this.imageType = Objects.requireNonNull(format);
    this.robot = new Robot();
    final Toolkit toolkit = Toolkit.getDefaultToolkit();
    this.screenBounds = new Rectangle(toolkit.getScreenSize());
    this.showCursor = preferences.isGrabCursor();
    this.snapsPerSecond = preferences.getSnapsPerSecond();
    LOGGER.info("Prepared screen grabber for {}x{}, show cursor = {}, {} snapshots per second", this.screenBounds.width, this.screenBounds.height, this.showCursor, this.snapsPerSecond);
  }

  public int getSnapsPerSecond() {
    return this.snapsPerSecond;
  }

  public void addGrabbingListener(final ScreenGrabberListener listener) {
    this.listeners.add(Objects.requireNonNull(listener));
  }

  public void removeGrabbingListener(final ScreenGrabberListener listener) {
    this.listeners.remove(Objects.requireNonNull(listener));
  }

  public Rectangle getBounds() {
    return this.screenBounds;
  }

  public boolean isShowCursor() {
    return this.showCursor;
  }

  private BufferedImage alignFormat(final BufferedImage image) {
    if (image.getType() == this.imageType.getValue()) {
      return image;
    } else {
      final BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), this.imageType.getValue());
      final Graphics2D gfx = newImage.createGraphics();
      try {
        gfx.drawImage(image, 0, 0, null);
      } finally {
        gfx.dispose();
      }
      return newImage;
    }
  }

  public void start() {
    if (this.disposed.get()) {
      throw new IllegalStateException("Disposed screen grabber");
    } else {
      final Thread screenGrabberThread = new Thread(this::run, "screen-grabbing-thread-" + System.nanoTime());
      screenGrabberThread.setDaemon(true);
      if (this.grabbingThread.compareAndSet(null, screenGrabberThread)) {
        screenGrabberThread.start();
        try{
          this.listeners.forEach(x -> x.onStarted(this));
        }catch(Exception ex){
          this.listeners.forEach(x -> x.onError(this, ex));
        }
      }
    }
  }

  private BufferedImage doCaptureScreen() {
    try {
      final BufferedImage image = this.robot.createScreenCapture(this.screenBounds);
      if (this.showCursor) {
        final Graphics2D graph = (Graphics2D) image.createGraphics();
        try {
          final Point mousePosition = MouseInfo.getPointerInfo().getLocation();
          graph.drawImage(MOUSE_ICON, mousePosition.x, mousePosition.y, null);
        } finally {
          graph.dispose();
        }
      }
      return image;
    } catch (RuntimeException ex) {
      LOGGER.error("Can't capture screen for error", ex);
      throw ex;
    }
  }

  private void run() {
    LOGGER.info("Screen grabber thread '{}' has been started", Thread.currentThread().getName());
    final long delayBetweenFrames = 1000L / this.snapsPerSecond;
    try {
      while (!Thread.currentThread().isInterrupted() && !this.disposed.get()) {
        final long start = System.currentTimeMillis();
        try{
          this.listeners.forEach(x -> x.onGrabbed(this, alignFormat(doCaptureScreen())));
        }catch(Exception ex){
          this.listeners.forEach(x -> x.onError(this, ex));
        }
        final long restTime = Math.max(0L, (start + delayBetweenFrames) - System.currentTimeMillis());
        if (restTime > 0L) {
          try {
            Thread.sleep(restTime);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            break;
          }
        }
      }
    } finally {
      LOGGER.info("Screen grabber thread '{}' has been completed", Thread.currentThread().getName());
    }
  }

  public void dispose() {
    if (this.disposed.compareAndSet(false, true)) {
      final Thread currentGrabbingThread = this.grabbingThread.getAndSet(null);
      currentGrabbingThread.interrupt();
      try {
        currentGrabbingThread.join();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      } finally {
        this.listeners.forEach(x -> x.onDisposed(this));
      }
    }
  }

  @Override
  public void close() throws IOException {
    this.dispose();
  }
  public enum ImageType {
    TYPE_3BYTE_BGR(BufferedImage.TYPE_3BYTE_BGR),
    TYPE_4BYTE_ABGR(BufferedImage.TYPE_4BYTE_ABGR),
    TYPE_4BYTE_ABGR_PRE(BufferedImage.TYPE_4BYTE_ABGR_PRE),
    TYPE_BYTE_BINARY(BufferedImage.TYPE_BYTE_BINARY),
    TYPE_BYTE_GRAY(BufferedImage.TYPE_BYTE_GRAY),
    TYPE_INT_ARGB(BufferedImage.TYPE_INT_ARGB),
    TYPE_INT_ARGB_PRE(BufferedImage.TYPE_INT_ARGB_PRE),
    TYPE_INT_BGR(BufferedImage.TYPE_INT_BGR),
    TYPE_INT_RGB(BufferedImage.TYPE_INT_RGB),
    TYPE_USHORT_555_RGB(BufferedImage.TYPE_USHORT_555_RGB),
    TYPE_USHORT_565_RGB(BufferedImage.TYPE_USHORT_565_RGB),
    TYPE_USHORT_GRAY(BufferedImage.TYPE_USHORT_GRAY);
    
    private final int value;
    
    private ImageType(final int value) {
      this.value = value;
    }
    
    public int getValue() {
      return this.value;
    }
    
  }
  public interface ScreenGrabberListener {
    
    default void onStarted(ScreenGrabber source){
      
    }
    
    default void onGrabbed(ScreenGrabber source, BufferedImage grabbedImage){
      
    }
    
    default void onDisposed(ScreenGrabber source) {
      
    }
    
    default void onError(ScreenGrabber source, Throwable error) {
      
    }
  }
}
