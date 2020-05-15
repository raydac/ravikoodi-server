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
import java.awt.Graphics2D;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.awt.image.DataBufferShort;
import java.util.function.Consumer;
import org.springframework.lang.NonNull;

public final class RobotScreenSource extends AbstractScreenSource {

  private final Toolkit toolkit;
  private final GraphicsDevice sourceDevice;
  private final Robot robot;
  private final Rectangle screenBounds;

  public RobotScreenSource(final boolean grabPointer) throws AWTException {
    super(grabPointer);
    this.toolkit = Toolkit.getDefaultToolkit();
    this.sourceDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
    this.screenBounds = this.sourceDevice.getDefaultConfiguration().getBounds();
    this.robot = new Robot(this.sourceDevice);
  }

  @Override
  @NonNull
  public GraphicsDevice getSourceDevice() {
    assertNotDisposed();
    return this.sourceDevice;
  }

  @Override
  @NonNull
  public Rectangle getBounds() {
    assertNotDisposed();
    return this.screenBounds;
  }

  @Override
  public byte[] grabRgb() {
    assertNotDisposed();
    final BufferedImage image = this.robot.createScreenCapture(this.screenBounds);

    if (this.isGrabPointer()) {
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

      switch (image.getType()) {
        case BufferedImage.TYPE_INT_BGR: {
          for (int i = 0; i < bufferLen; i++) {
            final int bgr = imageDataBuffer[i];
            result[dataIndex++] = (byte) bgr;
            result[dataIndex++] = (byte) (bgr >> 8);
            result[dataIndex++] = (byte) (bgr >> 16);
          }
        }
        break;
        case BufferedImage.TYPE_INT_ARGB:
        case BufferedImage.TYPE_INT_ARGB_PRE:
        case BufferedImage.TYPE_INT_RGB: {
          for (int i = 0; i < bufferLen; i++) {
            final int argb = imageDataBuffer[i];
            result[dataIndex++] = (byte) (argb >> 16);
            result[dataIndex++] = (byte) (argb >> 8);
            result[dataIndex++] = (byte) argb;
          }
        }
        break;
      }
    } else if (dataBuffer instanceof DataBufferShort) {
      final short[] imageDataBuffer = ((DataBufferShort) image.getRaster().getDataBuffer()).getData();
      final int bufferLen = imageDataBuffer.length;

      result = new byte[bufferLen * 3];
      int dataIndex = 0;

      switch (image.getType()) {
        case BufferedImage.TYPE_USHORT_555_RGB: {
          for (int i = 0; i < bufferLen; i++) {
            final int rgb = imageDataBuffer[i];
            result[dataIndex++] = (byte) (((rgb >> 10) & 0b11111) << 3);
            result[dataIndex++] = (byte) (((rgb >> 5) & 0b11111) << 3);
            result[dataIndex++] = (byte) ((rgb & 0b11111) << 3);
          }
        }
        break;
        case BufferedImage.TYPE_USHORT_565_RGB: {
          for (int i = 0; i < bufferLen; i++) {
            final int rgb = imageDataBuffer[i];
            result[dataIndex++] = (byte) (((rgb >> 11) & 0b11111) << 3);
            result[dataIndex++] = (byte) (((rgb >> 5) & 0b111111) << 2);
            result[dataIndex++] = (byte) ((rgb & 0b11111) << 3);
          }
        }
        break;
      }
    } else if (dataBuffer instanceof DataBufferByte) {
      final byte[] imageDataBuffer = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
      final int bufferLen = imageDataBuffer.length;

      int dataIndex = 0;

      switch (image.getType()) {
        case BufferedImage.TYPE_3BYTE_BGR: {
          result = new byte[imageDataBuffer.length];
          for (int i = 0; i < bufferLen;) {
            final byte b = imageDataBuffer[i++];
            final byte g = imageDataBuffer[i++];
            final byte r = imageDataBuffer[i++];
            result[dataIndex++] = r;
            result[dataIndex++] = g;
            result[dataIndex++] = b;
          }
        }
        break;
        case BufferedImage.TYPE_BYTE_GRAY: {
          result = new byte[imageDataBuffer.length * 3];
          for (int i = 0; i < bufferLen; i++) {
            final byte level = imageDataBuffer[i];
            result[dataIndex++] = level;
            result[dataIndex++] = level;
            result[dataIndex++] = level;
          }
        }
        break;
        case BufferedImage.TYPE_BYTE_BINARY: {
          final int imageWidth = image.getWidth();
          final int imageHeight = image.getHeight();
          result = new byte[imageWidth * imageHeight * 3];
          for (int y = 0; y < imageHeight; y++) {
            for (int x = 0; x < imageWidth; x++) {
              final int rgb = image.getRGB(x, y);
              result[dataIndex++] = (byte) (rgb >> 16);
              result[dataIndex++] = (byte) (rgb >> 8);
              result[dataIndex++] = (byte) rgb;
            }
          }
        }
        break;
        case BufferedImage.TYPE_BYTE_INDEXED: {
          result = new byte[bufferLen * 3];
          final ColorModel model = image.getColorModel();
          for (int i = 0; i < bufferLen; i++) {
            final int rgb = model.getRGB(i);
            result[dataIndex++] = (byte) (rgb >> 16);
            result[dataIndex++] = (byte) (rgb >> 8);
            result[dataIndex++] = (byte) rgb;
          }
        }
        break;
        case BufferedImage.TYPE_4BYTE_ABGR_PRE:
        case BufferedImage.TYPE_4BYTE_ABGR: {
          result = new byte[(bufferLen >> 2) * 3];
          for (int i = 0; i < bufferLen; i++) {
            final int rgb = imageDataBuffer[i];
            result[dataIndex++] = (byte) (((rgb >> 11) & 0b11111) << 3);
            result[dataIndex++] = (byte) (((rgb >> 5) & 0b111111) << 2);
            result[dataIndex++] = (byte) ((rgb & 0b11111) << 3);
          }
        }
        break;
        default: {
          result = new byte[this.screenBounds.width * this.screenBounds.height * 3];
        }
        break;
      }
    } else {
      result = new byte[this.screenBounds.width * this.screenBounds.height * 3];
    }
    return result;
  }

  @Override
  public String toString() {
    return RobotScreenSource.class.getSimpleName();
  }

}
