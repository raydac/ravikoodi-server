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

import java.awt.Component;
import java.io.File;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.AbstractCellEditor;
import javax.swing.JTable;
import javax.swing.table.TableCellEditor;

public class FilePathCellEditor extends AbstractCellEditor implements TableCellEditor {
    
    private final AtomicReference<File> dirRef = new AtomicReference<>();
    private final FilePathEditor editor;

    public FilePathCellEditor(final File file) {
        super();
        this.editor = new FilePathEditor(FilePathCellEditor.this, dirRef);
        this.dirRef.set(file);
        editor.text.addActionListener(a -> {
            this.stopCellEditing();
        });
    }

    @Override
    public Object getCellEditorValue() {
        return this.editor.getPath();
    }

    @Override
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
        this.editor.setPath((File) value);
        return this.editor;
    }
    
}
