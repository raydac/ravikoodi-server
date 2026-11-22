package com.igormaznitsa.ravikoodi;

import static java.util.Objects.requireNonNull;

import java.awt.Color;
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
  private JProgressBar progressBar;
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

  public void setTextInfo(@NonNull final String text) {
    this.showText(text);
    this.progressBar.setVisible(false);
    this.dismissCancel();
  }

  public void setProgress(@NonNull final String text, final int percent) {
    final boolean reveal = !this.progressBar.isVisible();
    this.showText(text, true);
    this.progressBar.setVisible(true);
    if (percent < 0) {
      this.progressBar.setIndeterminate(true);
    } else {
      this.progressBar.setIndeterminate(false);
      this.progressBar.setValue(Math.max(0, Math.min(100, percent)));
    }
    if (reveal) {
      this.revalidate();
    }
    this.repaint();
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

  @SuppressWarnings("unchecked")
  private void initComponents() {
    GridBagConstraints gridBagConstraints;

    panelContent = new JPanel();
    labelMain = new JLabel();
    progressBar = new JProgressBar(0, 100);
    progressBar.setPreferredSize(new Dimension(360, 18));
    progressBar.setVisible(false);
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

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(8, 0, 0, 0);
    panelContent.add(progressBar, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.insets = new Insets(8, 0, 0, 0);
    panelContent.add(cancelButton, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    add(panelContent, gridBagConstraints);
  }
}
