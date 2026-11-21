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

import com.igormaznitsa.ravikoodi.Utils;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

final class YoutubeSignInDialog extends JDialog {

  private final YoutubeDeviceLogin login;
  private final SessionSaver saver;
  private final AtomicBoolean cancelled = new AtomicBoolean();
  private final JTextField codeField = new JTextField(" ");
  private final JLabel status = new JLabel("Requesting a code from Google…");
  private final JButton openPage = new JButton("Open google.com/device");
  private volatile Thread worker;
  private volatile boolean authorized;
  private volatile URI verificationUri = URI.create("https://www.google.com/device");

  private YoutubeSignInDialog(final Window owner, final YoutubeDeviceLogin login,
                              final SessionSaver saver) {
    super(owner, "Sign in to YouTube", ModalityType.APPLICATION_MODAL);
    this.login = login;
    this.saver = saver;
    this.build();
  }

  static boolean show(final Window owner, final YoutubeDeviceLogin login,
                      final SessionSaver saver) {
    final YoutubeSignInDialog dialog = new YoutubeSignInDialog(owner, login, saver);
    dialog.start();
    dialog.setVisible(true);
    return dialog.authorized;
  }

  private void build() {
    this.codeField.setEditable(false);
    this.codeField.setFont(new Font(Font.MONOSPACED, Font.BOLD, 28));
    this.codeField.setHorizontalAlignment(JTextField.CENTER);
    this.openPage.setEnabled(false);
    this.openPage.addActionListener(event -> this.openVerificationPage());

    final JButton close = new JButton("Cancel");
    close.addActionListener(event -> this.cancel());

    final JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
    buttons.add(this.openPage);
    buttons.add(close);

    final JPanel root = new JPanel(new BorderLayout(0, 12));
    root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    root.add(new JLabel(
            "<html>Enter this code at <b>google.com/device</b> while signed in to Google.</html>"),
        BorderLayout.NORTH);
    root.add(this.codeField, BorderLayout.CENTER);
    final JPanel south = new JPanel(new BorderLayout());
    south.add(this.status, BorderLayout.CENTER);
    south.add(buttons, BorderLayout.SOUTH);
    root.add(south, BorderLayout.SOUTH);

    this.setContentPane(root);
    this.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent event) {
        YoutubeSignInDialog.this.cancel();
      }
    });
    this.pack();
    this.setMinimumSize(this.getSize());
    this.setLocationRelativeTo(this.getOwner());
  }

  private void start() {
    final Thread thread = new Thread(this::requestAndPoll, "youtube-device-login");
    thread.setDaemon(true);
    this.worker = thread;
    thread.start();
  }

  private void requestAndPoll() {
    final YoutubeDeviceCode code;
    try {
      code = this.login.requestCode();
    } catch (IOException ex) {
      this.showStatus(ex.getMessage() == null ? "Can't request a YouTube code" : ex.getMessage());
      return;
    }
    if (this.cancelled.get()) {
      return;
    }
    this.verificationUri = URI.create(code.verificationUrl());
    SwingUtilities.invokeLater(() -> {
      this.codeField.setText(code.userCode());
      this.codeField.selectAll();
      this.openPage.setEnabled(true);
      this.status.setText("Waiting for Google…");
    });
    this.poll(code);
  }

  private void poll(final YoutubeDeviceCode code) {
    int interval = code.intervalSeconds();
    while (!this.cancelled.get() && System.currentTimeMillis() < code.expiresAtEpochMillis()) {
      final YoutubeDevicePoll result;
      try {
        result = this.login.poll(code.deviceCode());
      } catch (IOException ex) {
        this.showStatus(ex.getMessage() == null ? "Can't reach Google" : ex.getMessage());
        return;
      }
      if (this.cancelled.get()) {
        return;
      }
      if (result instanceof YoutubeDevicePoll.Approved approved) {
        this.accept(approved.session());
        return;
      }
      if (result instanceof YoutubeDevicePoll.Rejected rejected) {
        this.showStatus(rejected.message());
        return;
      }
      if (result instanceof YoutubeDevicePoll.Pending pending && pending.slowDown()) {
        interval = Math.min(60, interval + 5);
      }
      try {
        Thread.sleep(interval * 1000L);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        return;
      }
    }
    if (!this.cancelled.get()) {
      this.showStatus("The code expired. Try again.");
    }
  }

  private void accept(final YoutubeSession session) {
    try {
      this.saver.save(session);
    } catch (IOException ex) {
      this.showStatus("Can't save the YouTube sign-in");
      return;
    }
    this.authorized = true;
    SwingUtilities.invokeLater(this::dispose);
  }

  private void showStatus(final String message) {
    SwingUtilities.invokeLater(() -> this.status.setText(message));
  }

  private void openVerificationPage() {
    try {
      Utils.showURLExternal(this.verificationUri.toURL());
    } catch (IOException ex) {
      this.status.setText("Can't open google.com/device");
    }
  }

  private void cancel() {
    this.cancelled.set(true);
    final Thread current = this.worker;
    if (current != null) {
      current.interrupt();
    }
    this.dispose();
  }

  @FunctionalInterface
  interface SessionSaver {
    void save(YoutubeSession session) throws IOException;
  }
}
