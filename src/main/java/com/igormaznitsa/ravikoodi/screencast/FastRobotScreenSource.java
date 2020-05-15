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

import java.awt.AWTException;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.PixelGrabber;
import java.awt.peer.RobotPeer;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;

public final class FastRobotScreenSource extends AbstractScreenSource {

  private final Toolkit toolkit;
  private final GraphicsDevice sourceDevice;
  private final Robot robot;
  private final Rectangle screenBounds;
  private final RobotPeer robotPeer;
  private final int cursorWidth;
  private final int cursorHeight;
  private final int [] cursorPixels;
  
  private final AtomicBoolean disposed = new AtomicBoolean();

  public FastRobotScreenSource(final boolean grabPointer) throws AWTException {
    super(grabPointer);
    this.toolkit = Toolkit.getDefaultToolkit();
    this.sourceDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    this.screenBounds = this.sourceDevice.getDefaultConfiguration().getBounds();
    this.robot = new Robot(this.sourceDevice);

    this.cursorWidth = MOUSE_ICON.getWidth(null);
    this.cursorHeight = MOUSE_ICON.getHeight(null);
    
    this.cursorPixels = new int[this.cursorWidth * this.cursorHeight];
    PixelGrabber grabber = new PixelGrabber(MOUSE_ICON, 0, 0, this.cursorWidth, this.cursorHeight, this.cursorPixels, 0, this.cursorWidth);
    try{
      grabber.grabPixels();
    }catch(InterruptedException e){
      Thread.currentThread().interrupt();
    }
    
    Field robotPeerField = null;
    for (final Field f : this.robot.getClass().getDeclaredFields()) {
      if (f.getType().isAssignableFrom(RobotPeer.class)) {
        f.setAccessible(true);
        robotPeerField = f;
        break;
      }
    }

    if (robotPeerField == null) {
      throw new IllegalStateException("Can't find robot peer field");
    } else {
      try {
        this.robotPeer = (RobotPeer) robotPeerField.get(this.robot);
      } catch (Exception ex) {
        throw new IllegalStateException("Can't grab robot peer field data", ex);
      }
    }
  }

  @Override
  public GraphicsDevice getSourceDevice() {
    this.assertNotDisposed();
    return this.sourceDevice;
  }

  @Override
  public Rectangle getBounds() {
    this.assertNotDisposed();
    return this.screenBounds;
  }

  @Override
  public synchronized byte[] grabRgb() {
    this.assertNotDisposed();
      final int[] grabbed = this.robotPeer.getRGBPixels(this.screenBounds);
      final int grabbedLen = grabbed.length;

      if (this.isGrabPointer()) {
        final Point mousePoint = this.getPointer();
        final int visibleWidth = Math.min(this.screenBounds.width - mousePoint.x, this.cursorWidth);
        final int visibleHeight = Math.min(this.screenBounds.height - mousePoint.y, this.cursorHeight);
        
        int scry = mousePoint.y;
        for(int y = 0; y < visibleHeight; y++) {
          if (scry < 0 || scry >= this.screenBounds.height) {
            scry ++;
            continue;
          }
          int scrx = mousePoint.x;
          for(int x = 0; x < visibleWidth; x++) {
            if (scrx < 0 || scrx >= this.screenBounds.width){
              scrx ++;
              continue;
            }
            final int cursorpos = y * this.cursorWidth + x;
            final int scrpos = scry * this.screenBounds.width + scrx;
            
            final int cursorValue = this.cursorPixels[cursorpos];
            if (cursorValue!=0) {
              grabbed[scrpos] = cursorValue;
            }
            
            scrx ++;
          }
          scry ++;
        }
      }

      final byte[] result = new byte[grabbedLen * 3];
      int dataIndex = 0;
      for (int i = 0; i < grabbedLen; i++) {
        final int rgb = grabbed[i];
        result[dataIndex++] = (byte) (rgb >> 16);
        result[dataIndex++] = (byte) (rgb >> 8);
        result[dataIndex++] = (byte) rgb;
      }
      return result;
  }

  @Override
  public String toString() {
    return FastRobotScreenSource.class.getSimpleName();
  }

}
