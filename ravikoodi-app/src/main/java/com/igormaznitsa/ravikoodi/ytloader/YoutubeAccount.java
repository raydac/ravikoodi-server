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

import java.awt.Window;
import java.util.concurrent.ExecutorService;
import javax.swing.JOptionPane;
import org.springframework.stereotype.Component;

@Component
public class YoutubeAccount {

  private final YoutubeDeviceLogin login;
  private final YoutubeSessionStore store;
  private final YtLinkExtractor extractor;
  private final ExecutorService executor;

  public YoutubeAccount(
      final YoutubeDeviceLogin login,
      final YoutubeSessionStore store,
      final YtLinkExtractor extractor,
      final ExecutorService executor) {
    this.login = login;
    this.store = store;
    this.extractor = extractor;
    this.executor = executor;
  }

  public boolean isSignedIn() {
    return this.store.hasSession();
  }

  public boolean signIn(final Window owner, final boolean replaceExisting) {
    if (!replaceExisting && this.store.hasSession()) {
      return this.confirmSignedIn(owner);
    }
    final boolean authorized = YoutubeSignInDialog.show(owner, this.login, this.store::save);
    if (authorized) {
      this.extractor.reload();
    }
    return authorized;
  }

  public void signOut() {
    final String refreshToken = this.store.takeRefreshToken();
    this.extractor.reload();
    this.executor.execute(() -> this.login.revokeQuietly(refreshToken));
  }

  private boolean confirmSignedIn(final Window owner) {
    final Object[] options = {"Sign out", "Sign in again", "Close"};
    final int choice = JOptionPane.showOptionDialog(
        owner,
        "This device is signed in to YouTube.",
        "YouTube account",
        JOptionPane.DEFAULT_OPTION,
        JOptionPane.PLAIN_MESSAGE,
        null,
        options,
        options[2]);
    if (choice == 0) {
      this.signOut();
    }
    return choice == 1 && this.signIn(owner, true);
  }
}
