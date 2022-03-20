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

import java.awt.BorderLayout;
import java.awt.Font;
import java.io.File;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellEditor;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class FilePathEditor extends JPanel {

    final JTextField text;
    private final TableLookupButton button;

    public FilePathEditor(@NonNull final TableCellEditor editor, @Nullable final AtomicReference<File> dir) {
        super(new BorderLayout(0, 0));
        this.text = new JTextField();
        this.text.setBorder(new EmptyBorder(0, 0, 0, 0));
        this.text.setColumns(1);
        this.add(this.text, BorderLayout.CENTER);
        this.button = new TableLookupButton("...");
        this.button.addActionListener(e -> {
            final JFileChooser chooser = new JFileChooser(dir.get());
            chooser.setAcceptAllFileFilterUsed(true);
            chooser.setDialogTitle("Select media resource");
            chooser.setMultiSelectionEnabled(false);
            chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            if (chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this)) == JFileChooser.APPROVE_OPTION) {
                final File file = chooser.getSelectedFile();
                dir.set(file.getParentFile());
                this.text.setText(file.getAbsolutePath());
                editor.stopCellEditing();
            }
        });
        this.add(this.button, BorderLayout.EAST);
        this.setBorder(new EmptyBorder(0, 0, 0, 0));
    }

    public void setPath(final File value) {
        this.text.setText(value == null ? "" : value.getAbsolutePath());
    }

    public File getPath() {
        return Utils.isBlank(this.text.getText()) ? null : new File(this.text.getText());
    }


}
