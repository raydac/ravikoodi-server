package com.igormaznitsa.ravikoodi;

import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import org.apache.commons.lang3.StringEscapeUtils;
import org.jspecify.annotations.NonNull;

public final class OpeningFileInfoPanel extends javax.swing.JPanel {

  private javax.swing.JLabel labelMain;
  private javax.swing.JPanel panelContent;

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
    java.awt.GridBagConstraints gridBagConstraints;

    panelContent = new javax.swing.JPanel();
    labelMain = new javax.swing.JLabel();

    setBackground(new java.awt.Color(0, 0, 0));
    setLayout(new java.awt.GridBagLayout());

    panelContent.setLayout(new java.awt.GridBagLayout());

    labelMain.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/loader.gif")));
    labelMain.setText("...");
    labelMain.setBorder(javax.swing.BorderFactory.createCompoundBorder(
        javax.swing.BorderFactory.createEtchedBorder(),
        javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16)));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    panelContent.add(labelMain, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    add(panelContent, gridBagConstraints);
  }
}
