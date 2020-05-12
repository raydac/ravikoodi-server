package com.igormaznitsa.ravikoodi;

import com.igormaznitsa.ravikoodi.kodijsonapi.ApplicationProperties;
import com.igormaznitsa.ravikoodi.kodijsonapi.ExecuteAction;
import com.igormaznitsa.ravikoodi.kodijsonapi.KodiService;
import java.awt.Component;
import java.util.Hashtable;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicSliderUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

public class ApplicationStatusPanel extends javax.swing.JPanel {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationStatusPanel.class);

  private final ScheduledExecutorService scheduledExecutor;
  private final ApplicationPreferences preferences;

  private final Icon APP_ICON_NOTCONNECTED = new ImageIcon(Utils.loadImage("64_app_icon_red.png"));
  private final Icon APP_ICON_CONNECTED = new ImageIcon(Utils.loadImage("64_app_icon_green.png"));
  
  private volatile boolean allowListenersProcessing = true;
  private volatile boolean ignoreRefresh = false;
  private final AtomicLong updateStatusCounter = new AtomicLong();
  private final MainFrame parent;

  public ApplicationStatusPanel(
          @NonNull final MainFrame parent,
          @NonNull final ScheduledExecutorService executors,
          @NonNull final ApplicationPreferences preferences) {
    this.parent = parent;
    this.scheduledExecutor = executors;
    this.preferences = preferences;

    initComponents();
    final Hashtable labels = new Hashtable();

    labels.put(0, new JLabel("0%"));
    labels.put(25, new JLabel("25%"));
    labels.put(50, new JLabel("50%"));
    labels.put(75, new JLabel("75%"));
    labels.put(100, new JLabel("100%"));

    this.sliderVolume.setLabelTable(labels);

    setEnableComponents(false);

    this.scheduledExecutor.scheduleWithFixedDelay(this::refresh, 1L, 3L, TimeUnit.SECONDS);

    final ApplicationStatusPanel thePanel = this;

    final ChangeListener sliderListener = new ChangeListener() {
      @Override
      public void stateChanged(ChangeEvent e) {
        thePanel.ignoreRefresh = true;
        try {
          if (allowListenersProcessing) {
            thePanel.updateStatusCounter.incrementAndGet();
            if (!thePanel.sliderVolume.getValueIsAdjusting()) {
              final int volume = thePanel.sliderVolume.getValue();
              LOGGER.info("Change application volume: {}%", volume);

              thePanel.sliderVolume.removeChangeListener(this);
              try {
                updateStatusCounter.incrementAndGet();
                final long agreedValue = new KodiService(thePanel.makeKodiAddress()).setApplicationVolume(volume);
                LOGGER.info("Player agreed volume {}%", agreedValue);
                thePanel.sliderVolume.setValue((int) agreedValue);
              } catch (Throwable thr) {
                LOGGER.error("Can't change volume : " + thr.getMessage());
              } finally {
                thePanel.sliderVolume.addChangeListener(this);
              }
            }
          }
        } finally {
          thePanel.ignoreRefresh = false;
        }
      }
    };

    this.sliderVolume.addChangeListener(sliderListener);

    this.buttonMute.addActionListener(x -> {
      if (this.allowListenersProcessing) {
        final boolean muted = this.buttonMute.isSelected();
        try {
          new KodiService(makeKodiAddress()).setApplicationMute(muted);
        } catch (Throwable thr) {
          LOGGER.error("Can't mute : " + thr.getMessage());
        }
      }
    });

    this.buttonReboot.addActionListener(x -> {
      if (JOptionPane.showConfirmDialog(this.parent, "Do you really want reboot?", "Reboot", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        try {
          new KodiService(makeKodiAddress()).doSystemReboot();
        } catch (Throwable thr) {
          LOGGER.error("Can't reboot : " + thr.getMessage());
        }
      }
    });

    this.buttonShutdown.addActionListener(x -> {
      if (JOptionPane.showConfirmDialog(this.parent, "Do you really want shutdown?", "Shutdown", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        try {
          new KodiService(makeKodiAddress()).doSystemShutdown();
        } catch (Throwable thr) {
          LOGGER.error("Can't shutdown : " + thr.getMessage());
        }
      }
    });
  }

  private KodiAddress makeKodiAddress() {
    return new KodiAddress(
            this.preferences.getKodiAddress(),
            this.preferences.getKodiPort(),
            this.preferences.getKodiName(),
            this.preferences.getKodiPassword(),
            this.preferences.isKodiSsl()
    );
  }

  public void refresh() {
    if (this.ignoreRefresh) {
      return;
    }
    final long counterValue = this.updateStatusCounter.get();

    final KodiAddress kodiAddress = makeKodiAddress();

    this.scheduledExecutor.submit(() -> {
      if (this.updateStatusCounter.get() == counterValue) {
        final KodiService kodiService;
        try {
          kodiService = new KodiService(kodiAddress);
        } catch (Exception ex) {
          SwingUtilities.invokeLater(() -> {
            SwingUtilities.invokeLater(() -> {
              labelApplicationName.setText("Wrong address");
              ApplicationStatusPanel.this.setEnabled(false);
            });
          });
          return;
        }

        try {
          final ApplicationProperties properties = kodiService.getAllApplicationProperties();
          SwingUtilities.invokeLater(() -> {
            if (this.updateStatusCounter.getAndIncrement() == counterValue) {
              this.allowListenersProcessing = false;
              try {
                if (!sliderVolume.getValueIsAdjusting()) {
                  labelApplicationName.setIcon(APP_ICON_CONNECTED);
                  labelApplicationName.setToolTipText("KODI is connected");
                  setEnableComponents(true);
                  sliderVolume.setValue((int) properties.getVolume());
                }
                buttonMute.setSelected(properties.isMuted());
              } finally {
                this.allowListenersProcessing = true;
              }
            }
          });
        } catch (Throwable thr) {
          LOGGER.warn("Can't get application properties: {}", thr.getMessage());
          SwingUtilities.invokeLater(() -> {
            labelApplicationName.setIcon(APP_ICON_NOTCONNECTED);
            labelApplicationName.setToolTipText("KODI is not connected");
            setEnableComponents(false);
          });
        }
      }
    });
  }

  private void setEnableComponents(final boolean enable) {
    this.sliderVolume.setEnabled(enable);
    this.buttonMute.setEnabled(enable);
    this.buttonReboot.setEnabled(enable);
    this.buttonShutdown.setEnabled(enable);

    for (final Component c : this.panelControlButtons.getComponents()) {
      if (c instanceof JButton) {
        c.setEnabled(enable);
      }
    }

    for (final Component c : this.panelCommonControlButtons.getComponents()) {
      if (c instanceof JButton) {
        c.setEnabled(enable);
      }
    }
  }

  private void sendInputExecuteAction(final ExecuteAction action) {
    try {
      new KodiService(makeKodiAddress()).sendInputExecuteAction(action);
    } catch (Throwable thr) {
      LOGGER.error("Can't send input execute action '" + action + "' : " + thr.getMessage());
    }
  }

  private void sendControlEvent(final KodiService.Control event, final Object... args) {
    try {
      new KodiService(makeKodiAddress()).sendControlEvent(event, args);
    } catch (Throwable thr) {
      LOGGER.error("Can't send control event '" + event + "' : " + thr.getMessage());
    }
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    labelApplicationName = new javax.swing.JLabel();
    sliderVolume = new javax.swing.JSlider();
    jPanel1 = new javax.swing.JPanel();
    buttonMute = new javax.swing.JToggleButton();
    filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(32, 0), new java.awt.Dimension(32, 0), new java.awt.Dimension(32, 32767));
    panelControlButtons = new javax.swing.JPanel();
    buttonSendText = new javax.swing.JButton();
    buttonUp = new javax.swing.JButton();
    buttonShowContextMenu = new javax.swing.JButton();
    buttonLeft = new javax.swing.JButton();
    buttonSelect = new javax.swing.JButton();
    buttonRight = new javax.swing.JButton();
    buttonBack = new javax.swing.JButton();
    buttonDown = new javax.swing.JButton();
    buttonHome = new javax.swing.JButton();
    panelCommonControlButtons = new javax.swing.JPanel();
    buttonChangeRatio = new javax.swing.JButton();
    buttonZoomIn = new javax.swing.JButton();
    buttonZoomOut = new javax.swing.JButton();
    filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(32, 0), new java.awt.Dimension(32, 0), new java.awt.Dimension(32, 32767));
    buttonReboot = new javax.swing.JButton();
    buttonShutdown = new javax.swing.JButton();

    setBorder(new javax.swing.border.SoftBevelBorder(javax.swing.border.BevelBorder.RAISED));
    setLayout(new java.awt.GridBagLayout());

    labelApplicationName.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    labelApplicationName.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/64_app_icon.png"))); // NOI18N
    labelApplicationName.setToolTipText("Checking");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 8);
    add(labelApplicationName, gridBagConstraints);

    sliderVolume.setMinorTickSpacing(5);
    sliderVolume.setPaintLabels(true);
    sliderVolume.setPaintTicks(true);
    sliderVolume.setValue(100);
    sliderVolume.setBorder(javax.swing.BorderFactory.createTitledBorder("Volume"));
    sliderVolume.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mousePressed(java.awt.event.MouseEvent evt) {
        sliderVolumeMousePressed(evt);
      }
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        sliderVolumeMouseClicked(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.weightx = 1000.0;
    add(sliderVolume, gridBagConstraints);

    jPanel1.setOpaque(false);

    buttonMute.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/32_sound.png"))); // NOI18N
    buttonMute.setToolTipText("Mute");
    buttonMute.setRolloverEnabled(false);
    buttonMute.setSelectedIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/32_sound_mute.png"))); // NOI18N
    jPanel1.add(buttonMute);
    jPanel1.add(filler1);

    panelControlButtons.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    panelControlButtons.setLayout(new java.awt.GridBagLayout());

    buttonSendText.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/text.png"))); // NOI18N
    buttonSendText.setToolTipText("SEND TEXT");
    buttonSendText.setFocusable(false);
    buttonSendText.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonSendTextActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    panelControlButtons.add(buttonSendText, gridBagConstraints);

    buttonUp.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/bullet_arrow_up.png"))); // NOI18N
    buttonUp.setToolTipText("UP");
    buttonUp.setFocusable(false);
    buttonUp.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonUpActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    panelControlButtons.add(buttonUp, gridBagConstraints);

    buttonShowContextMenu.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/menu.png"))); // NOI18N
    buttonShowContextMenu.setToolTipText("CONTEXT MENU");
    buttonShowContextMenu.setFocusable(false);
    buttonShowContextMenu.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonShowContextMenuActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    panelControlButtons.add(buttonShowContextMenu, gridBagConstraints);

    buttonLeft.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/bullet_arrow_left.png"))); // NOI18N
    buttonLeft.setToolTipText("LEFT");
    buttonLeft.setFocusable(false);
    buttonLeft.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonLeftActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    panelControlButtons.add(buttonLeft, gridBagConstraints);

    buttonSelect.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/bullet_green.png"))); // NOI18N
    buttonSelect.setToolTipText("SELECT");
    buttonSelect.setFocusable(false);
    buttonSelect.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonSelectActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    panelControlButtons.add(buttonSelect, gridBagConstraints);

    buttonRight.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/bullet_arrow_right.png"))); // NOI18N
    buttonRight.setToolTipText("RIGHT");
    buttonRight.setFocusable(false);
    buttonRight.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonRightActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    panelControlButtons.add(buttonRight, gridBagConstraints);

    buttonBack.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/button_navigation_back.png"))); // NOI18N
    buttonBack.setToolTipText("BACK");
    buttonBack.setFocusable(false);
    buttonBack.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonBackActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    panelControlButtons.add(buttonBack, gridBagConstraints);

    buttonDown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/bullet_arrow_down.png"))); // NOI18N
    buttonDown.setToolTipText("DOWN");
    buttonDown.setFocusable(false);
    buttonDown.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonDownActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    panelControlButtons.add(buttonDown, gridBagConstraints);

    buttonHome.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/home_page.png"))); // NOI18N
    buttonHome.setToolTipText("HOME");
    buttonHome.setFocusable(false);
    buttonHome.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonHomeActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 2;
    panelControlButtons.add(buttonHome, gridBagConstraints);

    jPanel1.add(panelControlButtons);

    panelCommonControlButtons.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    panelCommonControlButtons.setLayout(new java.awt.GridBagLayout());

    buttonChangeRatio.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_ico_ratio.png"))); // NOI18N
    buttonChangeRatio.setToolTipText("Change aspect ratio");
    buttonChangeRatio.setFocusable(false);
    buttonChangeRatio.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonChangeRatioActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    panelCommonControlButtons.add(buttonChangeRatio, gridBagConstraints);

    buttonZoomIn.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_zoom_in.png"))); // NOI18N
    buttonZoomIn.setToolTipText("Zoom In");
    buttonZoomIn.setFocusable(false);
    buttonZoomIn.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonZoomInActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    panelCommonControlButtons.add(buttonZoomIn, gridBagConstraints);

    buttonZoomOut.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_zoom_out.png"))); // NOI18N
    buttonZoomOut.setToolTipText("Zoom Out");
    buttonZoomOut.setFocusable(false);
    buttonZoomOut.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonZoomOutActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    panelCommonControlButtons.add(buttonZoomOut, gridBagConstraints);

    jPanel1.add(panelCommonControlButtons);
    jPanel1.add(filler2);

    buttonReboot.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/32_control_repeat_blue.png"))); // NOI18N
    buttonReboot.setToolTipText("Reboot");
    buttonReboot.setFocusable(false);
    buttonReboot.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonReboot.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    jPanel1.add(buttonReboot);

    buttonShutdown.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/32_control_power_blue.png"))); // NOI18N
    buttonShutdown.setToolTipText("Shutdown");
    buttonShutdown.setFocusable(false);
    buttonShutdown.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
    buttonShutdown.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
    jPanel1.add(buttonShutdown);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    add(jPanel1, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

  private void buttonSendTextActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSendTextActionPerformed
    final String text = JOptionPane.showInputDialog(this.parent, "Enter text line", "Send text", JOptionPane.PLAIN_MESSAGE);
    if (text != null) {
      sendControlEvent(KodiService.Control.SEND_TEXT, text);
    }
  }//GEN-LAST:event_buttonSendTextActionPerformed

  private void buttonUpActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonUpActionPerformed
    sendControlEvent(KodiService.Control.UP);
  }//GEN-LAST:event_buttonUpActionPerformed

  private void buttonLeftActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLeftActionPerformed
    sendControlEvent(KodiService.Control.LEFT);
  }//GEN-LAST:event_buttonLeftActionPerformed

  private void buttonSelectActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectActionPerformed
    sendControlEvent(KodiService.Control.SELECT);
  }//GEN-LAST:event_buttonSelectActionPerformed

  private void buttonRightActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRightActionPerformed
    sendControlEvent(KodiService.Control.RIGHT);
  }//GEN-LAST:event_buttonRightActionPerformed

  private void buttonBackActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonBackActionPerformed
    sendControlEvent(KodiService.Control.BACK);
  }//GEN-LAST:event_buttonBackActionPerformed

  private void buttonDownActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonDownActionPerformed
    sendControlEvent(KodiService.Control.DOWN);
  }//GEN-LAST:event_buttonDownActionPerformed

  private void buttonHomeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonHomeActionPerformed
    sendControlEvent(KodiService.Control.HOME);
  }//GEN-LAST:event_buttonHomeActionPerformed

  private void buttonShowContextMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonShowContextMenuActionPerformed
    sendControlEvent(KodiService.Control.HOME);
  }//GEN-LAST:event_buttonShowContextMenuActionPerformed

  private void sliderVolumeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sliderVolumeMouseClicked
    this.updateStatusCounter.incrementAndGet();
    final JSlider sourceSlider = (JSlider) evt.getSource();
    final BasicSliderUI bsu = (BasicSliderUI) sourceSlider.getUI();
    final int value = bsu.valueForXPosition(evt.getX());
    sourceSlider.setValue(value);
  }//GEN-LAST:event_sliderVolumeMouseClicked

  private void sliderVolumeMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sliderVolumeMousePressed
    this.updateStatusCounter.incrementAndGet();
  }//GEN-LAST:event_sliderVolumeMousePressed

  private void buttonChangeRatioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonChangeRatioActionPerformed
    this.sendInputExecuteAction(ExecuteAction.ASPECTRATIO);
  }//GEN-LAST:event_buttonChangeRatioActionPerformed

  private void buttonZoomInActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomInActionPerformed
    this.sendInputExecuteAction(ExecuteAction.ZOOMIN);
  }//GEN-LAST:event_buttonZoomInActionPerformed

  private void buttonZoomOutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonZoomOutActionPerformed
    this.sendInputExecuteAction(ExecuteAction.ZOOMOUT);
  }//GEN-LAST:event_buttonZoomOutActionPerformed


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton buttonBack;
  private javax.swing.JButton buttonChangeRatio;
  private javax.swing.JButton buttonDown;
  private javax.swing.JButton buttonHome;
  private javax.swing.JButton buttonLeft;
  private javax.swing.JToggleButton buttonMute;
  private javax.swing.JButton buttonReboot;
  private javax.swing.JButton buttonRight;
  private javax.swing.JButton buttonSelect;
  private javax.swing.JButton buttonSendText;
  private javax.swing.JButton buttonShowContextMenu;
  private javax.swing.JButton buttonShutdown;
  private javax.swing.JButton buttonUp;
  private javax.swing.JButton buttonZoomIn;
  private javax.swing.JButton buttonZoomOut;
  private javax.swing.Box.Filler filler1;
  private javax.swing.Box.Filler filler2;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JLabel labelApplicationName;
  private javax.swing.JPanel panelCommonControlButtons;
  private javax.swing.JPanel panelControlButtons;
  private javax.swing.JSlider sliderVolume;
  // End of variables declaration//GEN-END:variables
}
