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

import com.igormaznitsa.ravikoodi.Utils;
import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

public final class RobotScreenSource implements ScreenSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(RobotScreenSource.class);
  private static final Image MOUSE_ICON = Utils.loadImage("64_mouse_pointer.png");

  private final Toolkit toolkit;
  private final GraphicsDevice sourceDevice;
  private final Robot robot;
  private final Rectangle screenBounds;
  private final boolean showCursor;

  public RobotScreenSource(final boolean showCursor) throws AWTException {
    this.showCursor = showCursor;
    this.toolkit = Toolkit.getDefaultToolkit();
    this.sourceDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    this.screenBounds = this.sourceDevice.getDefaultConfiguration().getBounds();
    this.robot = new Robot(this.sourceDevice);
  }

  @Override
  public Point getPointer() {
    final PointerInfo info = MouseInfo.getPointerInfo();
    if (this.sourceDevice.getIDstring().equals(info.getDevice().getIDstring())) {
      return info.getLocation();
    } else {
      return new Point(this.screenBounds.width, this.screenBounds.height);
    }
  }

  @Override
  public Rectangle getBounds() {
    return this.screenBounds;
  }

  @Override
  public byte[] grabRgb() {
    final BufferedImage image = this.robot.createScreenCapture(this.screenBounds);
    
    if (this.showCursor) {
      final Graphics2D graph = (Graphics2D) image.createGraphics();
      try {
        final Point pointerPosition = this.getPointer();
        graph.drawImage(MOUSE_ICON, pointerPosition.x, pointerPosition.y, null);
      } finally {
        graph.dispose();
      }
    }

    final DataBuffer dataBuffer = image.getRaster().getDataBuffer();

    final byte[] result;
    if (dataBuffer instanceof DataBufferInt) {
      final int[] imageDataBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
      final int bufferLen = imageDataBuffer.length;

      result = new byte[bufferLen * 3];
      int dataIndex = 0;
     
      switch(image.getType()) {
        case BufferedImage.TYPE_INT_BGR: {
          for (int i = 0; i < bufferLen; i++) {
            final int bgr = imageDataBuffer[i];
            result[dataIndex++] = (byte) bgr;
            result[dataIndex++] = (byte) (bgr >> 8);
            result[dataIndex++] = (byte) (bgr >> 16);
          }
        }break;
        case BufferedImage.TYPE_INT_ARGB:
        case BufferedImage.TYPE_INT_ARGB_PRE:
        case BufferedImage.TYPE_INT_RGB:{
          for (int i = 0; i < bufferLen; i++) {
            final int argb = imageDataBuffer[i];
            result[dataIndex++] = (byte) (argb >> 16);
            result[dataIndex++] = (byte) (argb >> 8);
            result[dataIndex++] = (byte) argb;
          }
        }break;
        default: {
          LOGGER.error("Unsupported buffered image int format: " + image.getType());
        }break;
      }
    } else if (dataBuffer instanceof DataBufferShort) {
      final short[] imageDataBuffer = ((DataBufferShort) image.getRaster().getDataBuffer()).getData();
      final int bufferLen = imageDataBuffer.length;

      result = new byte[bufferLen * 3];
      int dataIndex = 0;
      
      switch(image.getType()) {
        case BufferedImage.TYPE_USHORT_555_RGB: {
          for (int i = 0; i < bufferLen; i++) {
            final int rgb = imageDataBuffer[i];
            result[dataIndex++] = (byte) (((rgb >> 10) & 0b11111)<<3);
            result[dataIndex++] = (byte) (((rgb >> 5) & 0b11111)<<3);
            result[dataIndex++] = (byte) ((rgb& 0b11111)<<3);
          }
        }break;
        case BufferedImage.TYPE_USHORT_565_RGB: {
          for (int i = 0; i < bufferLen; i++) {
            final int rgb = imageDataBuffer[i];
            result[dataIndex++] = (byte) (((rgb >> 11) & 0b11111) << 3);
            result[dataIndex++] = (byte) (((rgb >> 5) & 0b111111) << 2);
            result[dataIndex++] = (byte) ((rgb & 0b11111) << 3);
          }
        }break;
        default: {
          LOGGER.error("Unsupported buffered image ushort format: " + image.getType());
        }
        break;
      }
    } else {
      LOGGER.error("Unknown buffered image format: " + image.getType());
      result = new byte[this.screenBounds.width * this.screenBounds.height * 3];
    }
    return result;
  }

  @Override
  public void dispose() {

  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
  
}
