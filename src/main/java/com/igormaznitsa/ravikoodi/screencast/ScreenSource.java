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
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

public interface ScreenSource {

  static final Image MOUSE_ICON = Utils.loadImage("64_mouse_pointer.png");

  Point getPointer();

  Rectangle getBounds();

  byte[] grabRgb();

  void dispose();
}
