package com.igormaznitsa.ravikoodi;

import static java.util.Objects.requireNonNull;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jspecify.annotations.NonNull;

public final class OpeningFileInfoPanel extends JPanel {

  private JLabel labelMain;
  private JPanel panelContent;

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
    this.labelMain.setText("<html><b>" + StringEscapeUtils.escapeHtml3(text) + "</b></html>");
  }

  @SuppressWarnings("unchecked")
  private void initComponents() {
    GridBagConstraints gridBagConstraints;

    panelContent = new JPanel();
    labelMain = new JLabel();

    setBackground(new Color(0, 0, 0));
    setLayout(new GridBagLayout());

    panelContent.setLayout(new GridBagLayout());

    labelMain.setIcon(new ImageIcon(
        requireNonNull(getClass().getResource("/icons/loader.gif"))));
    labelMain.setText("...");
    labelMain.setBorder(BorderFactory.createCompoundBorder(
        BorderFactory.createEtchedBorder(),
        BorderFactory.createEmptyBorder(16, 16, 16, 16)));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    panelContent.add(labelMain, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    add(panelContent, gridBagConstraints);
  }
}
