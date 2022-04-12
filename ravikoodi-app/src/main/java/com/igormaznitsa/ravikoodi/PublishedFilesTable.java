/*
 * Copyright 2020 Igor Maznitsa.
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

import com.igormaznitsa.ravikoodi.prefs.StaticResource;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import org.springframework.lang.NonNull;

public class PublishedFilesTable extends JPanel {

    private final JTable resourceTable;
    private final String staticResourcePathPrefix;

    public PublishedFilesTable(@NonNull final String urlPrefix, @NonNull final File dir, @NonNull final List<StaticResource> resources) {
        super(new BorderLayout());

        this.staticResourcePathPrefix = urlPrefix;

        this.resourceTable = new JTable();
        this.resourceTable.setShowVerticalLines(true);
        this.resourceTable.setShowHorizontalLines(true);
        this.resourceTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        this.add(new JScrollPane(this.resourceTable), BorderLayout.CENTER);

        final StaticResourcesTableModel model = new StaticResourcesTableModel(resources);

        this.resourceTable.setModel(model);

        this.resourceTable.getTableHeader().setReorderingAllowed(false);

        this.resourceTable.getColumnModel().getColumn(2).setCellRenderer(new FilePathCellRenderer());
        this.resourceTable.getColumnModel().getColumn(2).setCellEditor(new FilePathCellEditor(dir));

        final JPanel buttonPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0);

        final JButton addResource = new JButton("Add");
        final JButton removeResource = new JButton("Remove");
        final JButton removeAll = new JButton("Remove All");
        final JButton enableAll = new JButton("Enable All");
        final JButton disableAll = new JButton("Disable All");

        enableAll.addActionListener(e -> {
            ((StaticResourcesTableModel) this.resourceTable.getModel()).enableAll();
        });

        disableAll.addActionListener(e -> {
            ((StaticResourcesTableModel) this.resourceTable.getModel()).disableAll();
        });

        removeAll.addActionListener(e -> {
            if (this.resourceTable.getRowCount() > 0
                    && JOptionPane.showConfirmDialog(this, "Remove all resources?", "Remove resources", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                ((StaticResourcesTableModel) this.resourceTable.getModel()).clear();
            }
        });

        removeResource.addActionListener(e -> {
            final int[] indexes = this.resourceTable.getSelectedRows();
            if (indexes.length > 0
                    && JOptionPane.showConfirmDialog(this, String.format("Remove %d resource(s)?", indexes.length), "Remove resources", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                ((StaticResourcesTableModel) this.resourceTable.getModel()).removeIndexes(indexes);
            }
        });

        buttonPanel.add(addResource, gbc);
        buttonPanel.add(removeResource, gbc);
        buttonPanel.add(removeAll, gbc);
        buttonPanel.add(Box.createVerticalStrut(16), gbc);
        buttonPanel.add(enableAll, gbc);
        buttonPanel.add(disableAll, gbc);

        gbc.weighty = 10000;
        buttonPanel.add(Box.createVerticalGlue(), gbc);

        this.add(new JLabel(String.format("<html>File will be accessible through link <b><u>%s/{ID}</i></u></html>", this.staticResourcePathPrefix)), BorderLayout.NORTH);

        this.add(buttonPanel, BorderLayout.EAST);

        removeResource.setEnabled(false);
        removeAll.setEnabled(!resources.isEmpty());

        addResource.addActionListener(e -> {
            model.addResource(new StaticResource("rsrc" + (model.resources.size() + 1)));
        });

        this.resourceTable.getSelectionModel().addListSelectionListener(event -> {
            if (this.resourceTable.getSelectedRowCount() > 0) {
                removeResource.setEnabled(true);
            }
            removeAll.setEnabled(this.resourceTable.getRowCount() > 0);
            removeResource.setEnabled(this.resourceTable.getRowCount() != 0);
        });

        this.addHierarchyListener((HierarchyEvent e) -> {
            final Window window = SwingUtilities.getWindowAncestor(PublishedFilesTable.this);
            if (window instanceof JDialog) {
                ((JDialog) window).setResizable(true);
            }
        });
    }

    @NonNull
    public List<StaticResource> getResources() {
        return new ArrayList<>(((StaticResourcesTableModel) this.resourceTable.getModel()).resources);
    }

    private static final class StaticResourcesTableModel implements TableModel {

        private final List<TableModelListener> listeners = new CopyOnWriteArrayList<>();

        private final List<StaticResource> resources = new ArrayList<>();

        public StaticResourcesTableModel(final List<StaticResource> resources) {
            this.resources.addAll(resources);
            Collections.sort(this.resources);
        }

        @Override
        public void addTableModelListener(TableModelListener listener) {
            this.listeners.add(listener);
        }

        @Override
        public void removeTableModelListener(TableModelListener listener) {
            this.listeners.remove(listener);
        }

        @Override
        public int getRowCount() {
            return this.resources.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(final int column) {
            switch (column) {
                case 0:
                    return "Enabled";
                case 1:
                    return "ID";
                case 2:
                    return "Path";
                default:
                    throw new Error("Unexpected column: " + column);
            }
        }

        @Override
        public Class<?> getColumnClass(final int column) {
            switch (column) {
                case 0:
                    return Boolean.class;
                case 1:
                    return String.class;
                case 2:
                    return File.class;
                default:
                    throw new Error("Unexpected column: " + column);
            }
        }

        @Override
        public boolean isCellEditable(int row, int col) {
            return true;
        }

        @Override
        public Object getValueAt(int row, int col) {
            final StaticResource timer = this.resources.get(row);
            switch (col) {
                case 0:
                    return timer.isEnabled();
                case 1:
                    return timer.getId();
                case 2:
                    return timer.getResourcePath();
                default:
                    throw new Error("Unexpected column: " + col);
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            final StaticResource timer = this.resources.get(row);
            switch (col) {
                case 0:
                    timer.setEnabled((Boolean) value);
                    break;
                case 1:
                    timer.setId((String) value);
                    break;
                case 2:
                    timer.setResourcePath((File) value);
                    break;
                default:
                    throw new Error("Unexpected column: " + col);
            }
        }

        private void addResource(@NonNull final StaticResource resource) {
            this.resources.add(resource);
            this.listeners.forEach(x -> {
                x.tableChanged(new TableModelEvent(this));
            });
        }

        private void removeIndexes(@NonNull final int[] indexes) {
            for (int i = 0; i < indexes.length; i++) {
                this.resources.set(indexes[i], null);
            }
            this.resources.removeIf(x -> x == null);
            this.listeners.forEach(x -> {
                x.tableChanged(new TableModelEvent(this));
            });
        }

        private void clear() {
            this.resources.clear();
            this.listeners.forEach(x -> {
                x.tableChanged(new TableModelEvent(this));
            });

        }

        private void disableAll() {
            this.resources.forEach(x -> x.setEnabled(false));
            this.listeners.forEach(x -> {
                x.tableChanged(new TableModelEvent(this));
            });
        }

        private void enableAll() {
            this.resources.forEach(x -> x.setEnabled(true));
            this.listeners.forEach(x -> {
                x.tableChanged(new TableModelEvent(this));
            });
        }
    }

    private static final class FilePathCellRenderer extends DefaultTableCellRenderer {

        public FilePathCellRenderer() {
            super();
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            final String text;
            final boolean problem;
            if (value == null) {
                text = "";
                problem = true;
            } else {
                final File theFile = (File) value;
                text = theFile.getName();
                problem = !theFile.isFile();
            }
            final Component result = super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
            return result;
        }
    }

}
