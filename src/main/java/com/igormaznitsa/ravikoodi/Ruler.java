/*
 * Copyright 2018 Igor Maznitsa.
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
package com.igormaznitsa.ravikoodi;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import javax.swing.UIManager;

public class Ruler extends JComponent {

  private boolean down = true;

  public boolean getDown() {
    return this.down;
  }

  public void setDown(final boolean down) {
    this.down = down;
    this.repaint();
  }

  public Ruler() {
    super();
    this.setForeground(UIManager.getColor("Label.foreground"));
  }

  @Override
  protected void paintComponent(final Graphics g) {
    final Graphics2D gfx = (Graphics2D) g;

    final Color color = this.getForeground();
    gfx.setColor(color);

    final int STEPS = 100;

    final int offset = 5;

    final int width = this.getWidth() - (offset << 1);
    final int height = this.getHeight();

    if (width == 0 || height == 0) {
      return;
    }

    final double step = (double) width / STEPS;

    gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    final int starty = this.down ? 0 : height - 1;

    for (int i = 0; i <= STEPS; i++) {
      final int x = offset + (int) (Math.round(step * i));

      if (i % 10 == 0) {
        gfx.drawRect(x, 0, 1, height);
      } else {
        g.drawLine(x, starty, x, height / 2);
      }
    }
  }

}
