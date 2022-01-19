/*
 * Copyright 2022 Igor Maznitsa.
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

import java.awt.Window;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class DialogNotifier implements GuiMessager {

    private final ApplicationContext applicationContext;

    public DialogNotifier(final ApplicationContext context) {
        this.applicationContext = context;
    }

    private Window findApplicationWindow() {
        return this.applicationContext.getBean(Window.class);
    }

    @Override
    public void showErrorMessage(@NonNull final String title, @NonNull final String message) {
        final Window window = this.findApplicationWindow();
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(window, message, title, JOptionPane.ERROR_MESSAGE);
        });
    }

    @Override
    public void showWarningMessage(@NonNull final String title, @NonNull final String message) {
        final Window window = this.findApplicationWindow();
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(window, message, title, JOptionPane.WARNING_MESSAGE);
        });
    }

    @Override
    public void showInfoMessage(@NonNull final String title, @NonNull final String message) {
        final Window window = this.findApplicationWindow();
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(window, message, title, JOptionPane.INFORMATION_MESSAGE);
        });
    }

}
