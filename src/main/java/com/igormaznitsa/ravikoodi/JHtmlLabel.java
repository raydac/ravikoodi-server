/* 
 * Copyright (C) 2018 Igor Maznitsa.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package com.igormaznitsa.ravikoodi;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.View;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * Small adaptation of javax.swing.JLabel to catch clicks on HTML links in the
 * label.
 * <b>The Component supports only HTML text and if the text is not into &lt;html&gt;
 * tags then such tags will be added automatically.</b>
 *
 * @author Igor Maznitsa (http://www.igormaznitsa.com)
 *
 * @version 1.00
 */
public class JHtmlLabel extends JLabel {

  private static final long serialVersionUID = -166975925687523220L;

  /**
   * Listener to get notification about activation of a link.
   */
  public interface LinkListener {
    /**
     * Called if detected activation of a link placed on the label.
     * @param source the label, must not be null
     * @param link the link to be processed, must not be null
     */
    void onLinkActivated(@NonNull JHtmlLabel source, @NonNull String link);
  }

  /**
   * Internal auxiliary class to keep cached parameters of found link elements.
   */
  private static final class HtmlLinkAddress {

    private final String address;
    private final int start;
    private final int end;

    HtmlLinkAddress(@NonNull final String address, final int startOffset, final int endOffset) {
      this.address = address;
      this.start = startOffset;
      this.end = endOffset;
    }

    @NonNull
    String getHREF() {
      return this.address;
    }

    boolean checkPosition(final int position) {
      return position >= this.start && position < this.end;
    }
  }

  /**
   * Inside cache of detected link elements.
   */
  private transient List<HtmlLinkAddress> linkCache = null;

  private final transient List<LinkListener> linkListeners = new CopyOnWriteArrayList<>();
  private boolean showLinkAddressInToolTip = false;
  private int minClickCountToActivateLink = 1;

  public JHtmlLabel(@NonNull final String text, @Nullable final Icon icon, final int horizontalAlignment) {
    super(text, icon, horizontalAlignment);

    final JHtmlLabel theInstance = this;

    final MouseAdapter mouseAdapter = new MouseAdapter() {
      @Override
      public void mouseMoved(@NonNull final MouseEvent e) {
        final String link = getLinkAtPosition(e.getPoint());
        if (link == null) {
          if (showLinkAddressInToolTip) {
            setToolTipText(null);
          }
          setCursor(Cursor.getDefaultCursor());
        }
        else {
          if (showLinkAddressInToolTip) {
            setToolTipText(link);
          }
          setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
      }

      @Override
      public void mouseClicked(@NonNull final MouseEvent e) {
        if (e.getClickCount() >= minClickCountToActivateLink) {
          final String link = getLinkAtPosition(e.getPoint());
          if (link != null) {
            linkListeners.forEach((l) -> {
              l.onLinkActivated(theInstance, link);
            });
          }
        }
      }
    };

    this.addMouseListener(mouseAdapter);
    this.addMouseMotionListener(mouseAdapter);
  }

  public JHtmlLabel(@Nullable final String text, final int horizontalAlignment) {
    this(text, null, horizontalAlignment);
  }

  public JHtmlLabel(@Nullable final String text) {
    this(text, null, LEADING);
  }

  public JHtmlLabel(@Nullable final Icon image, final int horizontalAlignment) {
    this(null, image, horizontalAlignment);
  }

  public JHtmlLabel(@Nullable final Icon image) {
    this(null, image, CENTER);
  }

  public JHtmlLabel() {
    this("", null, LEADING); //NOI18N
  }

  public int getMinClickCountToActivateLink(){
    return this.minClickCountToActivateLink;
  }

  public void setMinClickCountToActivateLink(final int clickNumber){
    this.minClickCountToActivateLink = Math.max(1, clickNumber);
  }
  
  public boolean isShowLinkAddressInTooltip() {
    return this.showLinkAddressInToolTip;
  }

  public void setShowLinkAddressInTooltip(final boolean flag) {
    if (this.showLinkAddressInToolTip) {
      if (!flag) {
        this.setToolTipText(null);
      }
    }
    this.showLinkAddressInToolTip = flag;
  }

  public void addLinkListener(@NonNull final LinkListener l) {
    this.linkListeners.add(Objects.requireNonNull(l));
  }

  public void removeLinkListener(@NonNull final LinkListener l) {
    this.linkListeners.remove(Objects.requireNonNull(l));
  }

  public void replaceMacroses(@NonNull final Properties properties) {
    String text = this.getText();
    for (final String k : properties.stringPropertyNames()) {
      text = text.replace("${" + k + "}", properties.getProperty(k)); //NOI18N
    }
    this.setText(text);
  }

  @Override
  public void setText(@NonNull final String text) {
    super.setText(text.toLowerCase(Locale.ENGLISH).trim().startsWith("<html>") ?  //NOI18N
        text : "<html>" + text + "</html>"); //NOI18N
    this.linkCache = null;
  }

  private void cacheLinkElements() {
    this.linkCache = new ArrayList<>();
    final View view = (View) this.getClientProperty("html"); //NOI18N
    if (view != null) {
      final HTMLDocument doc = (HTMLDocument) view.getDocument();
      final HTMLDocument.Iterator it = doc.getIterator(HTML.Tag.A);
      while (it.isValid()) {
        final SimpleAttributeSet s = (SimpleAttributeSet) it.getAttributes();
        final String link = (String) s.getAttribute(HTML.Attribute.HREF);
        if (link != null) {
          this.linkCache.add(new HtmlLinkAddress(link, it.getStartOffset(), it.getEndOffset()));
        }
        it.next();
      }
    }
  }

  @Nullable
  private String getLinkAtPosition(@NonNull final Point point) {
    if (this.linkCache == null) {
      cacheLinkElements();
    }

    final AccessibleJLabel accessibleJLabel = (AccessibleJLabel) this.getAccessibleContext().getAccessibleComponent();
    final int textIndex = accessibleJLabel.getIndexAtPoint(point);
    for (final HtmlLinkAddress l : this.linkCache) {
      if (l.checkPosition(textIndex)) {
        return l.getHREF();
      }
    }
    return null;
  }
}
