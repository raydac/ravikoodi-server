package com.igormaznitsa.ravikoodi;

import static java.util.Objects.requireNonNull;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jspecify.annotations.NonNull;

public final class OpeningFileInfoPanel extends JPanel {

  private JLabel labelMain;
  private JPanel panelContent;
  private JLabel videoCaption;
  private JProgressBar progressBar;
  private JLabel audioCaption;
  private JProgressBar audioBar;
  private JButton cancelButton;
  private Runnable onCancel;

  public OpeningFileInfoPanel() {
    initComponents();
    this.setBackground(new Color(0, 0, 0, 128));
    this.addKeyListener(new KeyListener() {
      @Override
      public void keyTyped(KeyEvent e) {
        e.consume();
      }

      @Override
      public void keyPressed(KeyEvent e) {
        e.consume();
      }

      @Override
      public void keyReleased(KeyEvent e) {
        e.consume();
      }
    });

    this.addMouseListener(new MouseListener() {
      @Override
      public void mouseClicked(MouseEvent e) {
        e.consume();
      }

      @Override
      public void mousePressed(MouseEvent e) {
        e.consume();
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        e.consume();
      }

      @Override
      public void mouseEntered(MouseEvent e) {
        e.consume();
      }

      @Override
      public void mouseExited(MouseEvent e) {
        e.consume();
      }
    });
  }

  private static JProgressBar trackBar() {
    final JProgressBar bar = new JProgressBar(0, 100);
    bar.setPreferredSize(new Dimension(360, 16));
    bar.setVisible(false);

    return bar;
  }

  public void setTextInfo(@NonNull final String text) {
    this.showText(text);
    this.hideTrack(this.videoCaption, this.progressBar);
    this.hideTrack(this.audioCaption, this.audioBar);
    this.dismissCancel();
  }

  public void setProgress(@NonNull final String text, final int percent) {
    this.setTracks(text, "", percent, null, -1);
  }

  public void setTracks(
      @NonNull final String title,
      final String first,
      final int firstPercent,
      final String second,
      final int secondPercent) {
    final boolean shapeChanged = this.progressBar.isVisible() != (first != null)
        || this.audioBar.isVisible() != (second != null);
    this.showText(title, true);
    this.showTrack(this.videoCaption, this.progressBar, first, firstPercent);
    this.showTrack(this.audioCaption, this.audioBar, second, secondPercent);
    if (shapeChanged) {
      this.revalidate();
    }
    this.repaint();
  }

  private void showTrack(
      final JLabel caption,
      final JProgressBar bar,
      final String text,
      final int percent) {
    if (text == null) {
      this.hideTrack(caption, bar);
      return;
    }
    caption.setText(text);
    caption.setVisible(!text.isBlank());
    bar.setVisible(true);
    if (percent < 0) {
      bar.setIndeterminate(true);
      return;
    }
    bar.setIndeterminate(false);
    bar.setValue(Math.max(0, Math.min(100, percent)));
  }

  private void showText(@NonNull final String text) {
    this.showText(text, false);
  }

  private void showText(@NonNull final String text, final boolean wide) {
    final String escaped = StringEscapeUtils.escapeHtml3(text);
    this.labelMain.setText(wide
        ? "<html><body style='width: 320px'><b>" + escaped + "</b></body></html>"
        : "<html><b>" + escaped + "</b></html>");
  }

  public void offerCancel(@NonNull final Runnable action) {
    this.onCancel = requireNonNull(action);
    this.cancelButton.setEnabled(true);
    this.cancelButton.setVisible(true);
    this.revalidate();
  }

  private void dismissCancel() {
    this.onCancel = null;
    if (this.cancelButton.isVisible()) {
      this.cancelButton.setVisible(false);
      this.revalidate();
    }
  }

  private void hideTrack(final JLabel caption, final JProgressBar bar) {
    caption.setVisible(false);
    bar.setVisible(false);
  }

  @SuppressWarnings("unchecked")
  private void initComponents() {
    GridBagConstraints gridBagConstraints;

    panelContent = new JPanel();
    labelMain = new JLabel();
    videoCaption = new JLabel();
    videoCaption.setVisible(false);
    progressBar = trackBar();
    audioCaption = new JLabel();
    audioCaption.setVisible(false);
    audioBar = trackBar();
    cancelButton = new JButton("Cancel");
    cancelButton.setVisible(false);
    cancelButton.addActionListener(event -> {
      final Runnable action = this.onCancel;
      if (action == null) {
        return;
      }
      this.cancelButton.setEnabled(false);
      action.run();
    });

    setBackground(new Color(0, 0, 0));
    setLayout(new GridBagLayout());

    panelContent.setOpaque(true);
    panelContent.setLayout(new GridBagLayout());
    panelContent.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createEtchedBorder(),
        BorderFactory.createEmptyBorder(16, 16, 16, 16)));

    labelMain.setIcon(new ImageIcon(
        requireNonNull(getClass().getResource("/icons/loader.gif"))));
    labelMain.setIconTextGap(12);
    labelMain.setText("...");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.weightx = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelContent.add(labelMain, gridBagConstraints);

    addTrack(videoCaption, 1, new Insets(10, 0, 0, 0));
    addTrack(progressBar, 2, new Insets(2, 0, 0, 0));
    addTrack(audioCaption, 3, new Insets(10, 0, 0, 0));
    addTrack(audioBar, 4, new Insets(2, 0, 0, 0));

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.insets = new Insets(12, 0, 0, 0);
    panelContent.add(cancelButton, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    add(panelContent, gridBagConstraints);
  }

  private void addTrack(final Component component, final int row, final Insets insets) {
    final GridBagConstraints constraints = new GridBagConstraints();
    constraints.gridx = 0;
    constraints.gridy = row;
    constraints.weightx = 1;
    constraints.fill = GridBagConstraints.HORIZONTAL;
    constraints.anchor = GridBagConstraints.WEST;
    constraints.insets = insets;
    this.panelContent.add(component, constraints);
  }
}
