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
package com.igormaznitsa.ravikoodi.timers;

import com.igormaznitsa.ravikoodi.ApplicationPreferences.Timer;
import static com.igormaznitsa.ravikoodi.Utils.isBlank;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.HierarchyEvent;
import java.io.File;
import java.text.ParseException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFormattedTextField;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.text.MaskFormatter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class TimersTable extends JPanel {

    protected static final DateTimeFormatter HHMMSS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTable timersTable;

    private static File lastFolder = null;
    
    static final class LocalTimeEditor extends JFormattedTextField {

        private static final MaskFormatter FORMAT;

        static {
            try {
                FORMAT = new MaskFormatter("##:##:##");
            } catch (ParseException ex) {
                throw new Error(ex);
            }
        }

        public LocalTimeEditor(final LocalTime value) {
            super("00:00:00");
            this.setBorder(new EmptyBorder(0, 0, 0, 0));
            this.setFormatter(FORMAT);
            this.setTime(value);
        }

        public void setTime(final LocalTime time) {
            if (time == null) {
                this.setText("");
            } else {

                final StringBuilder buffer = new StringBuilder();
                if (time.getHour() < 10) {
                    buffer.append('0');
                }
                buffer.append(time.getHour());
                buffer.append(':');
                if (time.getMinute() < 10) {
                    buffer.append('0');
                }
                buffer.append(time.getMinute());
                buffer.append(':');
                if (time.getSecond() < 10) {
                    buffer.append('0');
                }
                buffer.append(time.getSecond());

                this.setText(buffer.toString());
            }
        }

        @Nullable
        public LocalTime getTime() {
            final String text = this.getText();
            if (isBlank(text) || text.equals("  :  :  ")) {
                return null;
            }
            return LocalTime.parse(this.getText(), HHMMSS_FORMATTER);
        }
    }

    static final class FilePathEditor extends JPanel {

        private final JTextField text;
        private final JButton button;

        public FilePathEditor() {
            super(new GridBagLayout());
            final GridBagConstraints gbc = new GridBagConstraints(GridBagConstraints.RELATIVE, 0, 1, 1, 10000, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);

            this.text = new JTextField();
            this.text.setBorder(new EmptyBorder(0, 0, 0, 0));
            this.text.setColumns(1);
            this.add(this.text, gbc);
            gbc.weightx = 1;
            this.button = new JButton("...");
            this.button.setFocusable(false);
            
            this.button.addActionListener(e -> {
                final JFileChooser chooser = new JFileChooser(lastFolder);
                chooser.setAcceptAllFileFilterUsed(true);
                chooser.setDialogTitle("Select media resource");
                chooser.setMultiSelectionEnabled(false);
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                
                if (chooser.showOpenDialog(SwingUtilities.getWindowAncestor(this)) == JFileChooser.APPROVE_OPTION){
                    final File file = chooser.getSelectedFile();
                    lastFolder = file.getParentFile();
                    this.text.setText(file.getAbsolutePath());
                }
            });
            
            this.button.setBackground(UIManager.getColor("ComboBox.buttonBackground"));
            this.button.setBorder(UIManager.getBorder("ComboBox[button].border"));
            this.add(this.button, gbc);
            this.setBorder(new EmptyBorder(0, 0, 0, 0));
        }

        public void setPath(final File value) {
            this.text.setText(value == null ? "" : value.getAbsolutePath());
        }

        public File getPath() {
            return isBlank(this.text.getText()) ? null : new File(this.text.getText());
        }

    }

    static final class FilePathCellEditor extends AbstractCellEditor implements TableCellEditor {

        private final FilePathEditor editor = new FilePathEditor();

        public FilePathCellEditor() {
            super();
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

    static final class LocalTimeCellEditor extends AbstractCellEditor implements TableCellEditor {

        private final LocalTimeEditor edit = new LocalTimeEditor(LocalTime.now());

        public LocalTimeCellEditor() {
            super();
            edit.addActionListener(x -> {
                if (isBlank(this.edit.getText())) {
                    this.stopCellEditing();
                }
                try {
                    this.edit.commitEdit();
                    this.stopCellEditing();
                } catch (ParseException ex) {
                    // DO NOTHING
                }
            });
        }

        @Override
        public Object getCellEditorValue() {
            return edit.getTime();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            edit.setTime((LocalTime) value);
            return edit;
        }

    }

    @NonNull
    public List<Timer> getTimers() {
        return new ArrayList<>(((TimersTableModel) this.timersTable.getModel()).timers);
    }

    public TimersTable(@NonNull final List<Timer> timers) {
        super(new BorderLayout());

        this.timersTable = new JTable();
        this.timersTable.setShowVerticalLines(true);
        this.timersTable.setShowHorizontalLines(true);
        this.timersTable.setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
        
        this.add(new JScrollPane(this.timersTable), BorderLayout.CENTER);

        final TimersTableModel model = new TimersTableModel(timers);

        this.timersTable.setModel(model);
        this.timersTable.getColumnModel().getColumn(0).setCellRenderer(new BooleanCellRenderer());
        this.timersTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer());
        this.timersTable.getColumnModel().getColumn(2).setCellRenderer(new LocalTimeCellRenderer());
        this.timersTable.getColumnModel().getColumn(2).setCellEditor(new LocalTimeCellEditor());
        this.timersTable.getColumnModel().getColumn(3).setCellRenderer(new LocalTimeCellRenderer());
        this.timersTable.getColumnModel().getColumn(3).setCellEditor(new LocalTimeCellEditor());
        this.timersTable.getColumnModel().getColumn(4).setCellRenderer(new FilePathCellRenderer());
        this.timersTable.getColumnModel().getColumn(4).setCellEditor(new FilePathCellEditor());

        final JPanel buttonPanel = new JPanel(new GridBagLayout());

        GridBagConstraints gbc = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1, 1, GridBagConstraints.WEST, GridBagConstraints.HORIZONTAL, new Insets(1, 1, 1, 1), 0, 0);

        final JButton addTimer = new JButton("Add");
        final JButton removeTimer = new JButton("Remove");
        final JButton removeAll = new JButton("Remove All");
        final JButton enableAll = new JButton("Enable All");
        final JButton disableAll = new JButton("Disable All");

        enableAll.addActionListener(e -> {
            ((TimersTableModel) this.timersTable.getModel()).enableAll();
        });

        disableAll.addActionListener(e -> {
            ((TimersTableModel) this.timersTable.getModel()).disableAll();
        });
        
        removeAll.addActionListener(e -> {
            if (this.timersTable.getRowCount()>0
                && JOptionPane.showConfirmDialog(this, "Remove all timers?", "Remove timers", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                ((TimersTableModel)this.timersTable.getModel()).clear();
            }
        });
        
        removeTimer.addActionListener(e -> {
            final int[] indexes = this.timersTable.getSelectedRows();
            if (indexes.length > 0 
                && JOptionPane.showConfirmDialog(this, String.format("Remove %d timer(s)?", indexes.length), "Remove timers", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                ((TimersTableModel)this.timersTable.getModel()).removeIndexes(indexes);
            }
        });

        buttonPanel.add(addTimer, gbc);
        buttonPanel.add(removeTimer, gbc);
        buttonPanel.add(removeAll, gbc);
        buttonPanel.add(Box.createVerticalStrut(16), gbc);
        buttonPanel.add(enableAll, gbc);
        buttonPanel.add(disableAll, gbc);

        gbc.weighty = 10000;
        buttonPanel.add(Box.createVerticalGlue(), gbc);

        this.add(buttonPanel, BorderLayout.EAST);

        removeTimer.setEnabled(false);
        removeAll.setEnabled(!timers.isEmpty());

        addTimer.addActionListener(e -> {
            model.addTimer(new Timer("Nonamed"));
        });

        this.timersTable.getSelectionModel().addListSelectionListener(event -> {
            if (this.timersTable.getSelectedRowCount() > 0) {
                removeTimer.setEnabled(true);
            }
            removeAll.setEnabled(this.timersTable.getRowCount() > 0);
            removeTimer.setEnabled(this.timersTable.getRowCount() != 0);
        });
        
        this.addHierarchyListener((HierarchyEvent e) -> {
            final Window window = SwingUtilities.getWindowAncestor(TimersTable.this);
            if (window instanceof JDialog) {
                ((JDialog)window).setResizable(true);
            }
        });
    }

    private static final class TimersTableModel implements TableModel {

        private final List<TableModelListener> listeners = new CopyOnWriteArrayList<>();

        private final List<Timer> timers = new ArrayList<>();

        public TimersTableModel(final List<Timer> timers) {
            this.timers.addAll(timers);
            Collections.sort(this.timers);
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
            return this.timers.size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(final int column) {
            switch (column) {
                case 0:
                    return "Active";
                case 1:
                    return "Name";
                case 2:
                    return "From";
                case 3:
                    return "To";
                case 4:
                    return "Resource";
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
                    return LocalTime.class;
                case 3:
                    return LocalTime.class;
                case 4:
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
            final Timer timer = this.timers.get(row);
            switch (col) {
                case 0:
                    return timer.isEnabled();
                case 1:
                    return timer.getName();
                case 2:
                    return timer.getFrom();
                case 3:
                    return timer.getTo();
                case 4:
                    return timer.getResourcePath();
                default:
                    throw new Error("Unexpected column: " + col);
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            final Timer timer = this.timers.get(row);
            switch (col) {
                case 0:
                    timer.setEnabled((Boolean) value);
                    break;
                case 1:
                    timer.setName((String) value);
                    break;
                case 2:
                    timer.setFrom((LocalTime) value);
                    break;
                case 3:
                    timer.setTo((LocalTime) value);
                    break;
                case 4:
                    timer.setResourcePath((File) value);
                    break;
                default:
                    throw new Error("Unexpected column: " + col);
            }
        }

        private void addTimer(@NonNull final Timer timer) {
            this.timers.add(timer);
            this.listeners.forEach(x -> {
                x.tableChanged(new TableModelEvent(this));
            });
        }

        private void removeIndexes(@NonNull final int[] indexes) {
            for(int i=0;i<indexes.length;i++){
                this.timers.set(indexes[i],null);
            }
            this.timers.removeIf(x -> x == null);
            this.listeners.forEach(x -> {
                x.tableChanged(new TableModelEvent(this));
            });
        }

        private void clear() {
            this.timers.clear();
            this.listeners.forEach(x -> {
                x.tableChanged(new TableModelEvent(this));
            });
            
        }

        private void disableAll() {
            this.timers.forEach(x -> x.setEnabled(false));
            this.listeners.forEach(x -> {
                x.tableChanged(new TableModelEvent(this));
            });
        }

        private void enableAll() {
            this.timers.forEach(x -> x.setEnabled(true));
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
            if (value == null) {
                text = "";
            } else {
                text = ((File) value).getAbsolutePath();
            }
            return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
        }
    }

    private static final class LocalTimeCellRenderer extends DefaultTableCellRenderer {

        public LocalTimeCellRenderer() {
            super();
        }

        @NonNull
        @Override
        public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column
        ) {
            final String text;
            if (value == null) {
                text = "";
            } else {
                text = ((LocalTime) value).format(HHMMSS_FORMATTER);
            }
            return super.getTableCellRendererComponent(table, text, isSelected, hasFocus, row, column);
        }
    }

    private static final class BooleanCellRenderer extends JCheckBox implements TableCellRenderer {

        private static final Border NO_FOCUS_BORDER = new EmptyBorder(1, 1, 1, 1);

        public BooleanCellRenderer() {
            super();
            setHorizontalAlignment(JLabel.CENTER);
            setBorderPainted(true);
        }

        @NonNull
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
            boolean selected, boolean hasFocus, int row, int column) {
            if (selected) {
                this.setOpaque(true);
                this.setForeground(table.getSelectionForeground());
                this.setBackground(table.getSelectionBackground());
            } else {
                this.setForeground(table.getForeground());
                this.setBackground(table.getBackground());
                this.setOpaque(false);
            }
            this.setSelected((value != null && ((Boolean) value)));

            if (hasFocus) {
                this.setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
            } else {
                this.setBorder(NO_FOCUS_BORDER);
            }

            return this;
        }
    }

}
