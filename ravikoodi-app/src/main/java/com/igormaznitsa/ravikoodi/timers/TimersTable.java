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

import com.igormaznitsa.ravikoodi.FilePathCellEditor;
import com.igormaznitsa.ravikoodi.TableLookupButton;
import static com.igormaznitsa.ravikoodi.Utils.isBlank;
import com.igormaznitsa.ravikoodi.prefs.TimerResource;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.swing.AbstractCellEditor;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFormattedTextField;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import javax.swing.text.DateFormatter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class TimersTable extends JPanel {

    protected static final DateTimeFormatter HHMMSS_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final JTable timersTable;

    public TimersTable(@NonNull final File dir, @NonNull final List<TimerResource> timers) {
        super(new BorderLayout());

        this.timersTable = new JTable();
        this.timersTable.setShowVerticalLines(true);
        this.timersTable.setShowHorizontalLines(true);
        this.timersTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);

        this.add(new JScrollPane(this.timersTable), BorderLayout.CENTER);

        final TimersTableModel model = new TimersTableModel(timers);

        this.timersTable.setModel(model);

        this.timersTable.getTableHeader().setReorderingAllowed(false);

        this.timersTable.getColumnModel().getColumn(2).setCellRenderer(new LocalTimeCellRenderer());
        this.timersTable.getColumnModel().getColumn(2).setCellEditor(new LocalTimeCellEditor());

        this.timersTable.getColumnModel().getColumn(3).setCellRenderer(new LocalTimeCellRenderer());
        this.timersTable.getColumnModel().getColumn(3).setCellEditor(new LocalTimeCellEditor());

        this.timersTable.getColumnModel().getColumn(4).setCellRenderer(new FilePathCellRenderer());
        this.timersTable.getColumnModel().getColumn(4).setCellEditor(new FilePathCellEditor(dir));

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
            if (this.timersTable.getRowCount() > 0
                    && JOptionPane.showConfirmDialog(this, "Remove all timers?", "Remove timers", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                ((TimersTableModel) this.timersTable.getModel()).clear();
            }
        });

        removeTimer.addActionListener(e -> {
            final int[] indexes = this.timersTable.getSelectedRows();
            if (indexes.length > 0
                    && JOptionPane.showConfirmDialog(this, String.format("Remove %d timer(s)?", indexes.length), "Remove timers", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                ((TimersTableModel) this.timersTable.getModel()).removeIndexes(indexes);
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
            model.addTimer(new TimerResource("Unnamed"));
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
                ((JDialog) window).setResizable(true);
            }
        });
    }

    @NonNull
    public List<TimerResource> getTimers() {
        return new ArrayList<>(((TimersTableModel) this.timersTable.getModel()).timers);
    }

    private static final class LocalTimeEditor extends JPanel {

        private final JFormattedTextField textField;
        private final TableLookupButton button;

        public LocalTimeEditor(final LocalTime value) {
            super(new BorderLayout(0, 0));
            this.textField = new JFormattedTextField(new DateFormatter(new SimpleDateFormat("HH:mm:ss")));
            this.textField.setBorder(new EmptyBorder(0, 0, 0, 0));
            this.setTime(value);

            this.button = new TableLookupButton("X");

            this.button.addActionListener(e -> {
                this.textField.setText("");
                for (final ActionListener l : textField.getActionListeners()) {
                    l.actionPerformed(new ActionEvent(textField, 0, "ENTER"));
                }
            });

            this.textField.addKeyListener(new KeyAdapter() {
                private boolean onKey(final int keyCode) {
                    if (keyCode == KeyEvent.VK_ENTER && isBlank(textField.getText())) {
                        for (final ActionListener l : textField.getActionListeners()) {
                            l.actionPerformed(new ActionEvent(textField, 0, "ENTER"));
                        }
                        return true;
                    }
                    return false;
                }

                @Override
                public void keyReleased(KeyEvent e) {
                    if (onKey(e.getKeyCode())) {
                        e.consume();
                    }
                }

                @Override
                public void keyPressed(KeyEvent e) {
                    if (onKey(e.getKeyCode())) {
                        e.consume();
                    }
                }

            });

            this.add(this.textField, BorderLayout.CENTER);
            this.add(this.button, BorderLayout.EAST);

        }

        public void setTime(final LocalTime time) {
            final LocalTime value = time == null ? LocalTime.of(0, 0, 0) : time;
            this.textField.setValue(Date.from(value.atDate(LocalDate.now()).atZone(ZoneId.systemDefault()).toInstant()));
            this.textField.setCaretPosition(this.textField.getText().length());
        }

        @Nullable
        public LocalTime getTime() {
            if (isBlank(this.textField.getText())) {
                return null;
            }
            return LocalTime.from(((Date) this.textField.getValue()).toInstant().atZone(ZoneId.systemDefault()));
        }

        @NonNull
        public JFormattedTextField getTextField() {
            return this.textField;
        }
    }

    static final class LocalTimeCellEditor extends AbstractCellEditor implements TableCellEditor {

        private final LocalTimeEditor edit = new LocalTimeEditor(LocalTime.of(0, 0, 0));

        @Override
        public boolean stopCellEditing() {
            if (isBlank(this.edit.getTextField().getText())) {
                return super.stopCellEditing();
            }
            try {
                this.edit.getTextField().commitEdit();
                return super.stopCellEditing();
            } catch (ParseException ex) {
                // DO NOTHING
                return false;
            }
        }

        public LocalTimeCellEditor() {
            super();
            this.edit.getTextField().addActionListener(x -> {
                this.stopCellEditing();
            });
        }

        @Override
        public Object getCellEditorValue() {
            return this.edit.getTime();
        }

        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            this.edit.setBackground(table.getBackground());
            this.edit.setForeground(table.getForeground());
            this.edit.setFont(table.getFont());
            this.edit.setTime((LocalTime) value);
            return this.edit;
        }

    }

    private static final class TimersTableModel implements TableModel {

        private final List<TableModelListener> listeners = new CopyOnWriteArrayList<>();

        private final List<TimerResource> timers = new ArrayList<>();

        public TimersTableModel(final List<TimerResource> timers) {
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
            return 6;
        }

        @Override
        public String getColumnName(final int column) {
            switch (column) {
                case 0:
                    return "Enabled";
                case 1:
                    return "Name";
                case 2:
                    return "Start time";
                case 3:
                    return "Stop time";
                case 4:
                    return "File";
                case 5:
                    return "Replay";
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
                case 5:
                    return Boolean.class;
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
            final TimerResource timer = this.timers.get(row);
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
                case 5:
                    return timer.isReplay();
                default:
                    throw new Error("Unexpected column: " + col);
            }
        }

        @Override
        public void setValueAt(Object value, int row, int col) {
            final TimerResource timer = this.timers.get(row);
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
                case 5:
                    timer.setReplay((Boolean) value);
                    break;
                default:
                    throw new Error("Unexpected column: " + col);
            }
        }

        private void addTimer(@NonNull final TimerResource timer) {
            this.timers.add(timer);
            this.listeners.forEach(x -> {
                x.tableChanged(new TableModelEvent(this));
            });
        }

        private void removeIndexes(@NonNull final int[] indexes) {
            for (int i = 0; i < indexes.length; i++) {
                this.timers.set(indexes[i], null);
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

}
