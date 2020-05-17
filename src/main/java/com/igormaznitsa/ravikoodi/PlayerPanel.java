package com.igormaznitsa.ravikoodi;

import com.igormaznitsa.ravikoodi.kodijsonapi.ActivePlayerInfo;
import com.igormaznitsa.ravikoodi.kodijsonapi.AudioStream;
import com.igormaznitsa.ravikoodi.kodijsonapi.KodiService;
import com.igormaznitsa.ravikoodi.kodijsonapi.PlayerItem;
import com.igormaznitsa.ravikoodi.kodijsonapi.PlayerProperties;
import com.igormaznitsa.ravikoodi.kodijsonapi.PlayerSeekResult;
import com.igormaznitsa.ravikoodi.kodijsonapi.Subtitle;
import com.igormaznitsa.ravikoodi.kodijsonapi.Time;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.ToolTipManager;
import javax.swing.plaf.basic.BasicProgressBarUI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PlayerPanel extends javax.swing.JPanel {

  private static final Logger LOGGER = LoggerFactory.getLogger(PlayerPanel.class);

  private static final AtomicLong playerInternalIdCounter = new AtomicLong(1000L);

  public static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

  private final KodiService kodiService;
  private final ActivePlayerInfo playerInfo;
  private final ScheduledExecutorService executors;

  private final AtomicBoolean fullPlayerDataRefresh = new AtomicBoolean(true);

  private final AtomicLong speed = new AtomicLong();

  private final AtomicReference<Subtitle> lastDetectedSubtitle = new AtomicReference<>();
  private final AtomicReference<AudioStream> lastDetectedAudiostream = new AtomicReference<>();
  private final AtomicBoolean lastSubtitleEnabled = new AtomicBoolean();
  private final AtomicReference<PlayerProperties> lastProperties = new AtomicReference<>();

  private final Icon ICO_MOVIE = new ImageIcon(Utils.loadImage("32_movies.png"));
  private final Icon ICO_MUSIC = new ImageIcon(Utils.loadImage("32_music.png"));
  private final Icon ICO_PICTURE = new ImageIcon(Utils.loadImage("32_picture.png"));

  private final Icon ICO_BIG_PLAY = new ImageIcon(Utils.loadImage("32_control_play_blue.png"));
  private final Icon ICO_BIG_PAUSE = new ImageIcon(Utils.loadImage("32_control_pause_blue.png"));

  private final AtomicLong updateStatusCounter = new AtomicLong();
  private final MainFrame parent;

  private volatile boolean enableListeners = true;

  private volatile boolean ignoreRefresh = false;

  private static final Font DIGIFONT;

  private final long internalId = playerInternalIdCounter.getAndIncrement();

  private final AtomicBoolean paused = new AtomicBoolean();
  
  static {
    Font font;

    try (InputStream fontStream = PlayerPanel.class.getClassLoader().getResourceAsStream("fonts/digi.ttf")) {
      font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
    } catch (Exception ex) {
      LOGGER.error("Can't load DIGIFONT from resources", ex);
      font = Font.getFont(Font.MONOSPACED);
    }

    DIGIFONT = font;
  }

  public PlayerPanel(final MainFrame parent, final ActivePlayerInfo playerInfo, final ScheduledExecutorService executors, final KodiService kodiService) {
    super();
    initComponents();

    this.labelTitle.setFont(this.labelTitle.getFont().deriveFont(Font.BOLD, this.labelTitle.getFont().getSize2D() * 1.5f));

    Font font = DIGIFONT.deriveFont(Font.PLAIN, this.progressTime.getFont().getSize2D() * 1.5f);

    this.progressTime.setFont(font.deriveFont(Font.BOLD));
    this.labelStartTime.setFont(font);
    this.labelEndTime.setFont(font);

    this.progressTime.revalidate();
    this.labelStartTime.revalidate();
    this.labelEndTime.revalidate();

    this.doLayout();

    this.parent = parent;
    this.executors = executors;
    this.kodiService = kodiService;
    this.playerInfo = playerInfo;

    final String playerType = playerInfo.getType();

    if ("video".equalsIgnoreCase(playerType)) {
      this.labelTitle.setIcon(ICO_MOVIE);
      this.labelTitle.setToolTipText("Video");
    } else if ("picture".equalsIgnoreCase(playerType)) {
      this.labelTitle.setIcon(ICO_PICTURE);
      this.labelTitle.setToolTipText("Picture");
    } else if ("audio".equalsIgnoreCase(playerType)) {
      this.labelTitle.setIcon(ICO_MUSIC);
      this.labelTitle.setToolTipText("Audio");
    } else {
      LOGGER.warn("Detected unknown player type '{}'", playerType);
      this.labelTitle.setIcon(null);
      this.labelTitle.setToolTipText(playerType);
    }

    final PlayerPanel thePanel = this;

    this.progressTime.addMouseListener(new MouseAdapter() {
      final int defaultDismissTimeout = ToolTipManager.sharedInstance().getDismissDelay();
      final int dismissDelayMinutes = (int) TimeUnit.MINUTES.toMillis(10); // 10 minutes

      @Override
      public void mouseEntered(MouseEvent me) {
        ToolTipManager.sharedInstance().setDismissDelay(dismissDelayMinutes);
        showProgressAtPoint(me.getPoint(), true);
      }

      @Override
      public void mouseExited(MouseEvent me) {
        ToolTipManager.sharedInstance().setDismissDelay(defaultDismissTimeout);
        showProgressAtPoint(me.getPoint(), false);
      }
    });
  }

  public boolean isPaused() {
    return this.paused.get();
  }
  
  public long getPlayerId() {
    return this.playerInfo.getPlayerid();
  }

  public void dispose() {
  }

  private void focusSelectedSubtitle(final Subtitle subtitle, final boolean enabled) {
    this.comboSubtitle.setSelectedItem(subtitle);
    this.checkboxSubtitleEabled.setSelected(enabled);
  }

  private void focusSelectedAudiostream(final AudioStream audioStream) {
    this.comboAudio.setSelectedItem(audioStream);
  }

  public void refresh() {
    if (this.ignoreRefresh) {
      return;
    }

    final long counterValue = this.updateStatusCounter.get();
    if (this.executors.isShutdown()) {
      return;
    }
    this.executors.submit(() -> {
      try {
        final PlayerProperties properties;
        final String title;

        if (this.fullPlayerDataRefresh.compareAndSet(true, false)) {
          properties = this.kodiService.getPlayerProperties(this.playerInfo,
                  "percentage",
                  "time",
                  "totaltime",
                  "speed",
                  "audiostreams",
                  "currentaudiostream",
                  "subtitles",
                  "currentsubtitle",
                  "subtitleenabled");

          final PlayerItem playerItem = this.kodiService.getPlayerItem(this.playerInfo);
          title = playerItem.getItem().getLabel();

          SwingUtilities.invokeLater(() -> {
            this.comboAudio.setModel(new DefaultComboBoxModel<>(properties.getAudiostreams()));
            this.comboSubtitle.setModel(new DefaultComboBoxModel<>(properties.getSubtitles()));

            this.comboSubtitle.setEnabled(this.comboSubtitle.getModel().getSize() > 1);
            this.checkboxSubtitleEabled.setEnabled(this.comboSubtitle.getModel().getSize() > 0);
            this.comboAudio.setEnabled(this.comboAudio.getModel().getSize() > 1);

            this.lastDetectedAudiostream.set(properties.getCurrentaudiostream());
            this.lastDetectedSubtitle.set(properties.getCurrentsubtitle());

            focusSelectedAudiostream(properties.getCurrentaudiostream());
            focusSelectedSubtitle(properties.getCurrentsubtitle(), properties.isSubtitleenabled());

            this.labelStartTime.setText("00:00:00.000");
            this.labelEndTime.setText(properties.getTotaltime().toString());
          });
        } else {

          if (this.updateStatusCounter.get() != counterValue) {
            return;
          }

          title = null;
          properties = this.kodiService.getPlayerProperties(this.playerInfo,
                  "percentage",
                  "time",
                  "totaltime",
                  "speed",
                  "subtitleenabled"
          );
        }

        this.lastProperties.set(properties);

        final double percentage = properties.getPercentage();
        final Time time = properties.getTime();
        final Time totalTime = properties.getTotaltime();

        final long speed = properties.getSpeed();
        this.paused.set(speed == 0L);
        this.lastSubtitleEnabled.set(properties.isSubtitleenabled());

        SwingUtilities.invokeLater(() -> {
          if (this.updateStatusCounter.getAndIncrement() == counterValue) {
            this.enableListeners = false;
            try {
              this.speed.set(speed);
              if (title != null) {
                this.labelTitle.setText(title);
              }
              if (totalTime.isZero()) {
                this.progressTime.setIndeterminate(true);
                this.progressTime.setStringPainted(false);
                this.labelEndTime.setText("..:..:......");
                this.labelStartTime.setText("..:..:......");
              } else {
                this.progressTime.setIndeterminate(false);
                this.progressTime.setStringPainted(true);

                this.labelStartTime.setText("00:00:00.000");
                this.labelEndTime.setText(properties.getTotaltime().toString());
              }
              this.progressTime.setValue((int) Math.round(percentage * 100d));
              this.progressTime.setString(title);

              this.progressTime.setString(time.toString());
              this.checkboxSubtitleEabled.setSelected(properties.isSubtitleenabled());
              this.buttonPausePlay.setIcon(speed == 0 ? ICO_BIG_PLAY : ICO_BIG_PAUSE);
              this.labelSpeed.setText("<html><b>Speed: " + speed + "</b></html>");
            } finally {
              this.enableListeners = true;
            }
          }
        });
      } catch (Throwable thr) {
        LOGGER.error("Error during player update", thr);
      }
    });
  }

  public void notifyFullDataRefresh() {
    this.fullPlayerDataRefresh.set(true);
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

    filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
    buttonClose = new javax.swing.JButton();
    panelOptions = new javax.swing.JPanel();
    jLabel1 = new javax.swing.JLabel();
    comboAudio = new javax.swing.JComboBox<>();
    jLabel2 = new javax.swing.JLabel();
    comboSubtitle = new javax.swing.JComboBox<>();
    filler3 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
    panelControlButton = new javax.swing.JPanel();
    buttonSpeedDecrease = new javax.swing.JButton();
    buttonPausePlay = new javax.swing.JButton();
    buttonIncreaseSpeed = new javax.swing.JButton();
    labelSpeed = new javax.swing.JLabel();
    checkboxSubtitleEabled = new javax.swing.JCheckBox();
    labelTitle = new javax.swing.JLabel();
    panelProgress = new javax.swing.JPanel();
    labelStartTime = new javax.swing.JLabel();
    labelEndTime = new javax.swing.JLabel();
    progressTime = new javax.swing.JProgressBar(){
      public Point getToolTipLocation(MouseEvent evt){
        return new Point(evt.getPoint().x, this.getHeight() - 2);
      }
    };
    ruler1 = new com.igormaznitsa.ravikoodi.Ruler();
    ruler2 = new com.igormaznitsa.ravikoodi.Ruler();

    setBorder(javax.swing.BorderFactory.createCompoundBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(0, 0, 0)), javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8)));
    setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1000.0;
    add(filler1, gridBagConstraints);

    buttonClose.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/32_control_eject_blue.png"))); // NOI18N
    buttonClose.setToolTipText("Shutdown the player");
    buttonClose.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonCloseActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHEAST;
    add(buttonClose, gridBagConstraints);

    panelOptions.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 1, 8, 1));
    panelOptions.setLayout(new java.awt.GridBagLayout());

    jLabel1.setText("Audio:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    panelOptions.add(jLabel1, gridBagConstraints);

    comboAudio.setEnabled(false);
    comboAudio.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        comboAudioActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    panelOptions.add(comboAudio, gridBagConstraints);

    jLabel2.setText("Subtitle:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 3;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 16, 0, 0);
    panelOptions.add(jLabel2, gridBagConstraints);

    comboSubtitle.setEnabled(false);
    comboSubtitle.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        comboSubtitleActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    panelOptions.add(comboSubtitle, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.weightx = 1000.0;
    panelOptions.add(filler3, gridBagConstraints);

    buttonSpeedDecrease.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/32_control_rewind_blue.png"))); // NOI18N
    buttonSpeedDecrease.setToolTipText("Decrease speed");
    buttonSpeedDecrease.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonSpeedDecreaseActionPerformed(evt);
      }
    });
    panelControlButton.add(buttonSpeedDecrease);

    buttonPausePlay.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/32_control_play_blue.png"))); // NOI18N
    buttonPausePlay.setToolTipText("Play/Pause");
    buttonPausePlay.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonPausePlayActionPerformed(evt);
      }
    });
    panelControlButton.add(buttonPausePlay);

    buttonIncreaseSpeed.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/32_control_fastforward_blue.png"))); // NOI18N
    buttonIncreaseSpeed.setToolTipText("Increase speed");
    buttonIncreaseSpeed.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonIncreaseSpeedActionPerformed(evt);
      }
    });
    panelControlButton.add(buttonIncreaseSpeed);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1000.0;
    panelOptions.add(panelControlButton, gridBagConstraints);

    labelSpeed.setText("Speed: ---");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.insets = new java.awt.Insets(0, 16, 0, 8);
    panelOptions.add(labelSpeed, gridBagConstraints);

    checkboxSubtitleEabled.setText("Subtitle enabled");
    checkboxSubtitleEabled.setEnabled(false);
    checkboxSubtitleEabled.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        checkboxSubtitleEabledActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    panelOptions.add(checkboxSubtitleEabled, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    add(panelOptions, gridBagConstraints);

    labelTitle.setText("......");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    add(labelTitle, gridBagConstraints);

    panelProgress.setLayout(new java.awt.GridBagLayout());

    labelStartTime.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelStartTime.setText("..:..:..");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE;
    gridBagConstraints.insets = new java.awt.Insets(0, 0, 0, 8);
    panelProgress.add(labelStartTime, gridBagConstraints);

    labelEndTime.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
    labelEndTime.setText("..:..:..");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE;
    gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
    panelProgress.add(labelEndTime, gridBagConstraints);

    progressTime.setMaximum(10000);
    progressTime.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    progressTime.setFocusable(false);
    progressTime.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
      public void mouseMoved(java.awt.event.MouseEvent evt) {
        progressTimeMouseMoved(evt);
      }
    });
    progressTime.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        progressTimeMouseClicked(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.BASELINE;
    gridBagConstraints.weightx = 1000.0;
    panelProgress.add(progressTime, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.ipady = 8;
    gridBagConstraints.insets = new java.awt.Insets(2, 0, 4, 0);
    panelProgress.add(ruler1, gridBagConstraints);

    ruler2.setDown(false);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    gridBagConstraints.ipady = 8;
    gridBagConstraints.insets = new java.awt.Insets(4, 0, 2, 0);
    panelProgress.add(ruler2, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    add(panelProgress, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

  private void buttonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCloseActionPerformed
    if (JOptionPane.showConfirmDialog(this.parent, "Do you really want to stop the player?", "Stop player", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
      executors.submit(() -> {
        try {
          this.kodiService.doPlayerStop(this.playerInfo);
        } catch (Throwable thr) {
          LOGGER.error("Error during player stop", thr);
        }
      });
    }
  }//GEN-LAST:event_buttonCloseActionPerformed

  private void buttonSpeedDecreaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSpeedDecreaseActionPerformed
    if (this.enableListeners) {
      executors.submit(() -> {
        try {
          final long next = Utils.calculateNextKodiSpeedValue(this.speed.get(), false);
          LOGGER.info("Decreasing player {} speed to {}", this, next);
          this.kodiService.setPlayerSpeed(this.playerInfo, next);
        } catch (Throwable thr) {
          LOGGER.error("Error during player speed decrease", thr);
        }
      });
    }
  }//GEN-LAST:event_buttonSpeedDecreaseActionPerformed

  private void buttonIncreaseSpeedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonIncreaseSpeedActionPerformed
    if (this.enableListeners) {
      executors.submit(() -> {
        try {
          final long next = Utils.calculateNextKodiSpeedValue(this.speed.get(), true);
          LOGGER.info("Increasing player {} speed to {}", this, next);
          this.kodiService.setPlayerSpeed(this.playerInfo, next);
        } catch (Throwable thr) {
          LOGGER.error("Error during player speed increase", thr);
        }
      });
    }
  }//GEN-LAST:event_buttonIncreaseSpeedActionPerformed

  private void buttonPausePlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPausePlayActionPerformed
    if (this.enableListeners) {
      executors.submit(() -> {
        try {
          this.kodiService.doPlayerStartPause(this.playerInfo);
        } catch (Throwable thr) {
          LOGGER.error("Error during player pause/play", thr);
        }
      });
    }
  }//GEN-LAST:event_buttonPausePlayActionPerformed

  private void comboAudioActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboAudioActionPerformed
    if (this.enableListeners) {
      final AudioStream newAudioStream = (AudioStream) this.comboAudio.getSelectedItem();
      if (newAudioStream != null && !newAudioStream.equals(this.lastDetectedAudiostream.get())) {
        executors.submit(() -> {
          try {
            LOGGER.info("Trying to change audiostream of '{}' to '{}'", this.playerInfo, newAudioStream);
            this.kodiService.setPlayerAudiostream(this.playerInfo, newAudioStream);
          } catch (Throwable thr) {
            LOGGER.error("Error during set player audiostream", thr);
          }
        });
      }
    }
  }//GEN-LAST:event_comboAudioActionPerformed

  private void comboSubtitleActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboSubtitleActionPerformed
    if (this.enableListeners) {
      final Subtitle newSubtitle = (Subtitle) this.comboSubtitle.getSelectedItem();
      if (newSubtitle != null && !newSubtitle.equals(this.lastDetectedSubtitle.get())) {
        sendSubtitle(newSubtitle, this.checkboxSubtitleEabled.isSelected());
      }
    }
  }//GEN-LAST:event_comboSubtitleActionPerformed

  private void sendSubtitle(final Subtitle subtitle, final boolean enable) {
    executors.submit(() -> {
      try {
        LOGGER.info("Trying to change subtite of '{}' to '{}', enable={}", this.playerInfo, subtitle, enable);
        this.kodiService.setPlayerSubtitle(this.playerInfo, subtitle, enable);
      } catch (Throwable thr) {
        LOGGER.error("Error during set player audiostream", thr);
      }
    });
  }

  private void checkboxSubtitleEabledActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkboxSubtitleEabledActionPerformed
    if (this.enableListeners) {
      this.updateStatusCounter.incrementAndGet();
      sendSubtitle((Subtitle) this.comboSubtitle.getModel().getSelectedItem(), this.checkboxSubtitleEabled.isSelected());
    }
  }//GEN-LAST:event_checkboxSubtitleEabledActionPerformed

  private void progressTimeMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_progressTimeMouseMoved
    showProgressAtPoint(evt.getPoint(), true);
  }//GEN-LAST:event_progressTimeMouseMoved

  private void progressTimeMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_progressTimeMouseClicked
    if (this.enableListeners && !this.progressTime.isIndeterminate()) {
      final double position = getProgressPercentAtPoint(evt.getPoint());
      this.updateStatusCounter.incrementAndGet();
      try {
        LOGGER.info("Seek player '{}' position : {}%", this.playerInfo, position);
        final PlayerSeekResult result = this.kodiService.doPlayerSeekPercentage(playerInfo, position * 100.0d);
        this.progressTime.setValue((int) Math.round(result.getPercentage() * 100));
      } catch (Throwable ex) {
        LOGGER.error("Error in player seek: {}", ex.getMessage());
      }
    }
  }//GEN-LAST:event_progressTimeMouseClicked

  private double getProgressPercentAtPoint(final Point point) {
    BasicProgressBarUI barui = (BasicProgressBarUI) this.progressTime.getUI();
    return point.getX() / this.progressTime.getWidth();
  }

  private void showProgressAtPoint(final Point mousePoint, final boolean show) {
    final PlayerProperties playerProperties = this.lastProperties.get();

    if (show && playerProperties != null && !this.progressTime.isIndeterminate()) {
      if (!playerProperties.getTotaltime().isZero()) {
        final LocalTime totalTime = playerProperties.getTotaltime().asLocalTime();
        this.progressTime.setToolTipText(LocalTime.ofSecondOfDay(Math.round((totalTime.toSecondOfDay() * getProgressPercentAtPoint(mousePoint)))).format(TIME_FORMATTER));
      } else {
        this.progressTime.setToolTipText(null);
      }
    } else {
      this.progressTime.setToolTipText(null);
    }
  }

  @Override
  public String toString() {
    return String.format("playerPanel#%d(%d)", this.internalId, this.getPlayerId());
  }


  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton buttonClose;
  private javax.swing.JButton buttonIncreaseSpeed;
  private javax.swing.JButton buttonPausePlay;
  private javax.swing.JButton buttonSpeedDecrease;
  private javax.swing.JCheckBox checkboxSubtitleEabled;
  private javax.swing.JComboBox<com.igormaznitsa.ravikoodi.kodijsonapi.AudioStream> comboAudio;
  private javax.swing.JComboBox<com.igormaznitsa.ravikoodi.kodijsonapi.Subtitle> comboSubtitle;
  private javax.swing.Box.Filler filler1;
  private javax.swing.Box.Filler filler3;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel labelEndTime;
  private javax.swing.JLabel labelSpeed;
  private javax.swing.JLabel labelStartTime;
  private javax.swing.JLabel labelTitle;
  private javax.swing.JPanel panelControlButton;
  private javax.swing.JPanel panelOptions;
  private javax.swing.JPanel panelProgress;
  private javax.swing.JProgressBar progressTime;
  private com.igormaznitsa.ravikoodi.Ruler ruler1;
  private com.igormaznitsa.ravikoodi.Ruler ruler2;
  // End of variables declaration//GEN-END:variables

}
