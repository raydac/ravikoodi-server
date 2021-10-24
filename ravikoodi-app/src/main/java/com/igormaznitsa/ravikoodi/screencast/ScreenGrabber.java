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
import com.igormaznitsa.ravikoodi.ApplicationPreferences.GrabberType;
import com.igormaznitsa.ravikoodi.screencast.screensrc.AbstractScreenSource;
import com.igormaznitsa.ravikoodi.screencast.screensrc.FfmpegScreenSource;
import com.igormaznitsa.ravikoodi.screencast.screensrc.RobotScreenSource;
import java.awt.AWTException;
import java.awt.Rectangle;
import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;

public final class ScreenGrabber implements Closeable {

  private static final Logger LOGGER = LoggerFactory.getLogger(ScreenGrabber.class);

  private final AtomicBoolean disposed = new AtomicBoolean();
  private final AtomicReference<Thread> grabbingThread = new AtomicReference<>();
  private final int snapsPerSecond;
  private final boolean showCursor;
  private final List<ScreenGrabberListener> listeners = new CopyOnWriteArrayList<>();
  private final Rectangle targetSize;
  private final AbstractScreenSource screenSource;
  
  public ScreenGrabber(final ApplicationPreferences preferences) throws AWTException {
    this.showCursor = preferences.isGrabCursor();
    this.snapsPerSecond = preferences.getSnapsPerSecond();

    final GrabberType grabberType = preferences.getGrabberType();
    
    AbstractScreenSource srcSource = null;
    switch(grabberType) {
      case AUTO : {
        srcSource = makeFastRobotGrabber(this.showCursor);
        if (srcSource == null) {
          srcSource = makeRobotGrabber(this.showCursor);
        }
      }break;
      case ROBOT : {
        srcSource = makeRobotGrabber(this.showCursor);
      }break;
      case ROBOT_FAST : {
        srcSource = makeFastRobotGrabber(this.showCursor);
      }break;
      case FFMPEG : {
        srcSource = makeFfmpegGrabber(preferences, this.showCursor);
      }break;
    }

    if (srcSource == null) {
      throw new RuntimeException("Can't create grabber "+ grabberType.name());
    } else {
      this.screenSource = srcSource;
    }
    
    this.targetSize = this.screenSource.getBounds();
    
    LOGGER.info("Prepared screen grabber {} for {}x{}, device {}, show cursor = {}, {} snapshots per second", this.screenSource, this.targetSize.width, this.targetSize.height, this.screenSource.getSourceDevice().getIDstring(), this.showCursor, this.snapsPerSecond);
  }
  
  @Nullable
  private AbstractScreenSource makeFfmpegGrabber(final ApplicationPreferences preferences, final boolean showPointer) {
    AbstractScreenSource result;
    try {
      result = new FfmpegScreenSource(preferences, showPointer);
    } catch (Throwable ex) {
      LOGGER.warn("Can't create ffmpeg grabber", ex);
      result = null;
    }
    return result;
  }
  
  @Nullable
  private AbstractScreenSource makeRobotGrabber(final boolean showPointer) {
    AbstractScreenSource result;
    try {
      result = new RobotScreenSource(showPointer);
    } catch (Throwable ex) {
      LOGGER.warn("Can't create robot grabber", ex);
      result = null;
    }
    return result;
  }
  
  @Nullable
  private AbstractScreenSource makeFastRobotGrabber(final boolean showPointer) {
    AbstractScreenSource result;
    try {
      final Class<?> fastRobot = Class.forName("com.igormaznitsa.ravikoodi.screencast.screensrc.FastRobotScreenSource");
      result = (AbstractScreenSource) fastRobot.getConstructor(boolean.class).newInstance(this.showCursor);
    } catch (Throwable ex) {
      LOGGER.warn("Can't create fast robot grabber", ex);
      result = null;
    }
    return result;
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
    return this.targetSize;
  }

  public boolean isShowCursor() {
    return this.showCursor;
  }

  public void start() {
    if (this.disposed.get()) {
      throw new IllegalStateException("Disposed screen grabber");
    } else {
      final Thread screenGrabberThread = new Thread(this::run, "screen-grabbing-thread-" + System.nanoTime());
      screenGrabberThread.setDaemon(true);
      if (this.grabbingThread.compareAndSet(null, screenGrabberThread)) {
        screenGrabberThread.start();
        try {
          this.listeners.forEach(x -> x.onStarted(this));
        } catch (Exception ex) {
          this.listeners.forEach(x -> x.onError(this, ex));
        }
      }
    }
  }

  private void run() {
    LOGGER.info("Screen grabber thread '{}' has been started", Thread.currentThread().getName());
    final long delayBetweenFrames = 1000L / this.snapsPerSecond;
    try {
      while (!Thread.currentThread().isInterrupted() && !this.disposed.get()) {
        final long start = System.currentTimeMillis();
        try {
          this.listeners.forEach(x -> x.onGrabbed(this, this.screenSource.grabRgb()));
        } catch (Exception ex) {
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
        try {
          this.screenSource.dispose();
        } finally {
          this.listeners.forEach(x -> x.onDisposed(this));
        }
      }
    }
  }

  @Override
  public void close() throws IOException {
    this.dispose();
  }

  public interface ScreenGrabberListener {

    default void onStarted(ScreenGrabber source) {

    }

    default void onGrabbed(ScreenGrabber source, byte[] rgbImageData) {

    }

    default void onDisposed(ScreenGrabber source) {

    }

    default void onError(ScreenGrabber source, Throwable error) {

    }
  }
}
