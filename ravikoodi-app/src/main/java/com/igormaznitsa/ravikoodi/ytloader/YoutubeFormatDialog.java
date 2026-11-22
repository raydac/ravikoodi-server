/*
 * Copyright 2026 Igor Maznitsa.
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

package com.igormaznitsa.ravikoodi.ytloader;

import static java.util.Objects.requireNonNull;

import com.github.kiulian.downloader.model.videos.formats.Format;
import com.igormaznitsa.ravikoodi.ytloader.YoutubeVariants.Choice;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import org.apache.commons.lang3.StringEscapeUtils;

public final class YoutubeFormatDialog {

  private YoutubeFormatDialog() {
  }

  public static boolean available(final List<Format> formats, final YtVideoType required) {
    final List<Format> provided = formats == null ? List.of() : formats;

    return !YoutubeVariants.withSound(provided, required).isEmpty()
        || !YoutubeVariants.separate(provided, required).isEmpty();
  }

  public static Optional<Choice> choose(
      final Window owner,
      final List<Format> formats,
      final YtQuality quality,
      final YtVideoType required) {
    requireNonNull(owner, "owner");
    requireNonNull(quality, "quality");
    requireNonNull(required, "required");

    final List<Format> provided = formats == null ? List.of() : formats;
    final List<Choice> ready = YoutubeVariants.withSound(provided, required);
    final List<Choice> separate = YoutubeVariants.separate(provided, required);
    if (ready.isEmpty() && separate.isEmpty()) {
      return Optional.empty();
    }
    if (ready.size() == 1 && separate.isEmpty()) {
      return Optional.of(ready.get(0));
    }

    final Chooser chooser = new Chooser(ready, separate, quality, required);
    final int answer = JOptionPane.showConfirmDialog(
        owner,
        chooser,
        "Choose a YouTube video",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);
    if (answer != JOptionPane.OK_OPTION) {
      return Optional.empty();
    }

    return chooser.selected();
  }

  public static boolean confirmJoin(final Window owner, final Choice choice) {
    requireNonNull(owner, "owner");
    requireNonNull(choice, "choice");

    final JLabel notice = new JLabel(
        "<html>This downloads the picture and the soundtrack, then joins them."
            + "<br>It can take a while and uses disk space in the video folder.</html>");
    final JLabel picked = new JLabel("<html><b>"
        + escape(choice.pictureLine())
        + "</b><br>"
        + escape(choice.soundtrackLine())
        + "</html>");
    picked.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createEtchedBorder(),
        BorderFactory.createEmptyBorder(8, 10, 8, 10)));

    final JPanel panel = new JPanel(new BorderLayout(0, 12));
    panel.add(notice, BorderLayout.NORTH);
    panel.add(picked, BorderLayout.CENTER);
    final int answer = JOptionPane.showConfirmDialog(
        owner,
        panel,
        "Download and join",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE);

    return answer == JOptionPane.OK_OPTION;
  }

  private static String escape(final String text) {
    return StringEscapeUtils.escapeHtml3(text == null ? "" : text);
  }

  private static final class Chooser extends JPanel {

    private final List<Choice> ready;
    private final List<Choice> separate;
    private final YtQuality quality;
    private final YtVideoType required;
    private final JList<Choice> rows = new JList<>();
    private final JScrollPane scroll = new JScrollPane();
    private final JCheckBox join = new JCheckBox("Download and join a separate soundtrack");
    private final JLabel hint =
        new JLabel("Picture-only videos are downloaded into the video folder and then merged.");
    private final JLabel empty = new JLabel("No video includes a soundtrack.");

    private Chooser(
        final List<Choice> ready,
        final List<Choice> separate,
        final YtQuality quality,
        final YtVideoType required) {
      super(new BorderLayout(0, 10));
      this.ready = List.copyOf(ready);
      this.separate = List.copyOf(separate);
      this.quality = quality;
      this.required = required;
      this.rows.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
      this.rows.setFixedCellHeight(48);
      this.rows.setVisibleRowCount(6);
      this.rows.setCellRenderer(new RowRenderer());
      this.join.setEnabled(!this.separate.isEmpty());
      this.join.setSelected(this.ready.isEmpty());
      this.join.addActionListener(event -> this.showRows(this.rows.getSelectedValue()));
      this.hint.setBorder(BorderFactory.createEmptyBorder(0, 24, 0, 0));
      this.hint.setEnabled(this.join.isEnabled());
      this.empty.setHorizontalAlignment(SwingConstants.CENTER);
      this.empty.setPreferredSize(new Dimension(480, 120));

      final JLabel intro = new JLabel("Select a version to open.");
      final Dimension introSize = intro.getPreferredSize();
      intro.setPreferredSize(new Dimension(Math.max(480, introSize.width), introSize.height));
      this.add(intro, BorderLayout.NORTH);
      this.add(this.scroll, BorderLayout.CENTER);
      final JPanel options = new JPanel(new BorderLayout(0, 2));
      options.add(this.join, BorderLayout.NORTH);
      options.add(this.hint, BorderLayout.SOUTH);
      this.add(options, BorderLayout.SOUTH);
      this.showRows(null);
    }

    private void showRows(final Choice keep) {
      final List<Choice> shown = new ArrayList<>(this.ready);
      if (this.join.isSelected()) {
        shown.addAll(this.separate);
      }

      final DefaultListModel<Choice> model = new DefaultListModel<>();
      shown.forEach(model::addElement);
      this.rows.setModel(model);
      this.rows.setVisibleRowCount(shown.isEmpty() ? 4 : Math.min(6, shown.size()));
      this.scroll.setViewportView(shown.isEmpty() ? this.empty : this.rows);
      this.scroll.setPreferredSize(null);
      final Choice selected = shown.contains(keep)
          ? keep
          : YoutubeVariants.preferred(shown, this.quality, this.required).orElse(null);
      if (selected != null) {
        this.rows.setSelectedValue(selected, true);
      }
    }

    private Optional<Choice> selected() {
      final Choice choice = this.rows.getSelectedValue();

      return Optional.ofNullable(choice);
    }
  }

  private static final class RowRenderer extends DefaultListCellRenderer {

    private static Color mixed(final Color ink, final Color paper) {
      final Color from = ink == null ? Color.BLACK : ink;
      final Color to = paper == null ? Color.WHITE : paper;

      return new Color(
          (from.getRed() * 3 + to.getRed()) / 4,
          (from.getGreen() * 3 + to.getGreen()) / 4,
          (from.getBlue() * 3 + to.getBlue()) / 4);
    }

    private static String hex(final Color color) {
      return String.format(
          Locale.ROOT, "#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    @Override
    public Component getListCellRendererComponent(
        final JList<?> list,
        final Object value,
        final int index,
        final boolean isSelected,
        final boolean cellHasFocus) {
      final JLabel label = (JLabel) super.getListCellRendererComponent(
          list, "", index, isSelected, cellHasFocus);
      if (!(value instanceof Choice choice)) {
        return label;
      }

      final Color secondary = isSelected
          ? list.getSelectionForeground()
          : mixed(list.getForeground(), list.getBackground());
      final String extra = choice.audio().isPresent() ? " · download and join" : "";
      label.setText("<html><b>" + escape(choice.pictureLine()) + "</b><br><span style='color: "
          + hex(secondary) + "'>" + escape(choice.soundtrackLine()) + extra + "</span></html>");
      label.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

      return label;
    }
  }
}
