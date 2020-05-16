package com.igormaznitsa.ravikoodi;

import com.igormaznitsa.ravikoodi.ApplicationPreferences.GrabberType;
import com.igormaznitsa.ravikoodi.screencast.JavaSoundAdapter;
import com.igormaznitsa.ravikoodi.ApplicationPreferences.Quality;
import com.igormaznitsa.ravikoodi.ApplicationPreferences.SpeedProfile;
import com.igormaznitsa.ravikoodi.kodijsonapi.ApplicationProperties;
import com.igormaznitsa.ravikoodi.kodijsonapi.KodiService;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JOptionPane;
import javax.swing.JSpinner;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;

@SuppressWarnings("all")
public class OptionsPanel extends javax.swing.JPanel {

  private static final Logger LOGGER = LoggerFactory.getLogger(OptionsPanel.class);

  public static final class Data {

    private String host;
    private int port;
    private boolean serverSsl;
    private String kodiAddress;
    private String kodiName;
    private String kodiPassword;
    private String ffmpegPath;
    private boolean grabCursor;
    private int snapsPerSecond;
    private int kodiPort;
    private String soundInput;
    private int bandwidth;
    private Quality quality;
    private GrabberType grabberType;
    private SpeedProfile speedProfile;
    private boolean kodiSsl;
    private float soundOffset;

    public Data(@NonNull final ApplicationPreferences preferences) {
      this.host = preferences.getServerHost();
      this.port = preferences.getServerPort();
      this.kodiAddress = preferences.getKodiAddress();
      this.kodiName = preferences.getKodiName();
      this.kodiPassword = preferences.getKodiPassword();
      this.kodiPort = preferences.getKodiPort();
      this.serverSsl = preferences.isServerSsl();
      this.kodiSsl = preferences.isKodiSsl();
      this.ffmpegPath = preferences.getFfmpegPath();
      this.snapsPerSecond = preferences.getSnapsPerSecond();
      this.grabCursor = preferences.isGrabCursor();
      this.soundInput = preferences.getSoundInput();
      this.quality = preferences.getQuality();
      this.bandwidth = preferences.getBandwidth();
      this.soundOffset = preferences.getSoundOffset();
      this.speedProfile = preferences.getSpeedProfile();
      this.grabberType = preferences.getGrabberType();
    }

    public void save(@NonNull final ApplicationPreferences preferences) {
      preferences.setServerInterface(this.host);
      preferences.setServerPort(this.port);
      preferences.setServerSsl(this.serverSsl);
      
      preferences.setGrabCursor(this.grabCursor);
      preferences.setFfmpegPath(this.ffmpegPath);
      preferences.setSnapsPerSecond(this.snapsPerSecond);
      preferences.setSoundnput(this.soundInput);
      preferences.setQuality(this.quality);
      preferences.setSpeedProfile(this.speedProfile);
      preferences.setBandwidth(this.bandwidth);
      preferences.setSoundOffset(this.soundOffset);
      preferences.setGrabberType(this.grabberType);
      
      preferences.setKodiAddress(this.kodiAddress);
      preferences.setKodiName(this.kodiName);
      preferences.setKodiPassword(this.kodiPassword);
      preferences.setKodiSsl(this.kodiSsl);
      
      preferences.setKodiPort(this.kodiPort);

      preferences.flush();
    }

    @NonNull
    public GrabberType getGrabberType() {
      return this.grabberType;
    }
    
    public void setGrabberType(@NonNull final GrabberType grabberType) {
      this.grabberType = grabberType;
    }
    
    public int getBandwidth() {
      return this.bandwidth;
    }
    
    public void setBandwidth(final int value) {
      this.bandwidth = value;
    }
    
    @NonNull
    public SpeedProfile getSpeedProfile() {
      return this.speedProfile;
    }
    
    public void setSpeedProfile(@NonNull final SpeedProfile value) {
      this.speedProfile = value;
    }
    
    @NonNull
    public Quality getQuality() {
      return this.quality;
    }
    
    public void setQuality(@NonNull final Quality value) {
      this.quality = value;
    }
        
    @NonNull
    public String getSoundInput() {
      return this.soundInput;
    }
    
    public void setSoundInput(@NonNull final String value) {
      this.soundInput = value;
    }
    
    public float getSoundOffset() {
      return this.soundOffset;
    }
    
    public void setSoundOffset(final float value) {
      this.soundOffset = value;
    }
    
    @NonNull
    public String getKodiAddress() {
      return this.kodiAddress;
    }

    public void setKodiAddress(@NonNull final String address) {
      this.kodiAddress = address.trim();
    }

    @NonNull
    public String getKodiName() {
      return this.kodiName;
    }

    public void setKodiName(@NonNull final String name) {
      this.kodiName = name;
    }

    @NonNull
    public String getKodiPassword() {
      return this.kodiPassword;
    }

    public void setKodiPassword(@NonNull final String password) {
      this.kodiPassword = password;
    }

    public int getKodiPort() {
      return this.kodiPort;
    }

    public void setKodiPort(final int port) {
      this.kodiPort = port;
    }

    @NonNull
    public String getFfmpegPath() {
      return this.ffmpegPath;
    }

    @NonNull
    public void setFfmpegPath(@NonNull final String path) {
      this.ffmpegPath = path;
    }
    
    public int getSnapsPerSecond() {
      return this.snapsPerSecond;
    }

    public void setSnapsPerSecond(final int value) {
      this.snapsPerSecond = value;
    }

    public void setServerHost(@NonNull final String host) {
      this.host = host;
    }

    public void setGrabCursor(final boolean value) {
      this.grabCursor = value;
    }

    public boolean isGrabCursor() {
      return this.grabCursor;
    }
    
    public void setServerPort(final int port) {
      this.port = port;
    }

    public void setServerSsl(final boolean useSsl) {
      this.serverSsl = useSsl;
    }
    
    public boolean isServerSsl() {
      return this.serverSsl;
    }
    
    public void setKodiSsl(final boolean useSsl) {
      this.kodiSsl = useSsl;
    }
    
    public boolean isKodiSsl() {
      return this.kodiSsl;
    }
    
    @NonNull
    public String getServerHost() {
      return this.host;
    }

    public int getServerPort() {
      return this.port;
    }
  }

  private final Data currentData;

  private static final class DocumentWrapper implements DocumentListener {

    private final Consumer<String> consumer;
    
    private DocumentWrapper(@NonNull final Document document, final Consumer<String> consumer) {
      this.consumer = consumer;
    }

    public static void of(@NonNull final Document document, final Consumer<String> consumer) {
      document.addDocumentListener(new DocumentWrapper(document, consumer));
    }
    
    @NonNull
    private String getText(@NonNull final Document doc) {
      try {
        return doc.getText(0, doc.getLength());
      } catch (BadLocationException ex) {
        return "";
      }
    }

    @Override
    public void insertUpdate(@NonNull final DocumentEvent e) {
      this.consumer.accept(getText(e.getDocument()));
    }

    @Override
    public void removeUpdate(@NonNull final DocumentEvent e) {
      this.consumer.accept(getText(e.getDocument()));
    }

    @Override
    public void changedUpdate(@NonNull final DocumentEvent e) {
      this.consumer.accept(getText(e.getDocument()));
    }

  }

  public OptionsPanel(@NonNull final Data data, @NonNull final JavaSoundAdapter soundAdapter) {
    this.currentData = data;
    initComponents();
    this.comboSoundLine.setPrototypeDisplayValue("############################");
    final List<String> ne = new ArrayList<>();

    try {
      final Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
      while (e.hasMoreElements()) {
        e.nextElement().getInterfaceAddresses().stream().map(a -> a.getAddress().getHostAddress()).collect(Collectors.toCollection(() -> ne));
      }

      if (!ne.contains(data.getServerHost())) {
        ne.add(data.getServerHost());
      }

      Collections.sort(ne);
    } catch (SocketException e) {
      LOGGER.error("Can't get interfaces", e);
    }

    final List<String> inputLines = new ArrayList<>();
    inputLines.add("(none)");
    for(JavaSoundAdapter.SoundPort port : soundAdapter.getAvailableSoundPorts()) {
      inputLines.add(port.getName());
    }
    
    this.spinnerKodiPort.setEditor(new JSpinner.NumberEditor(this.spinnerKodiPort, "#"));
    this.spinnerServerPort.setEditor(new JSpinner.NumberEditor(this.spinnerServerPort, "#"));
    this.spinnerSnapsPerSecond.setEditor(new JSpinner.NumberEditor(this.spinnerSnapsPerSecond, "##"));
    this.spinnerBandwidth.setEditor(new JSpinner.NumberEditor(this.spinnerBandwidth,"##"));
    this.spinnerSoundOffset.setEditor(new JSpinner.NumberEditor(this.spinnerSoundOffset, "##.##"));
    
    this.comboSoundLine.setModel(new DefaultComboBoxModel<>(inputLines.toArray(new String[inputLines.size()])));
    this.comboSoundLine.setSelectedItem(data.getSoundInput());
    
    this.comboGrabberType.setModel(new DefaultComboBoxModel<>(Stream.of(GrabberType.values()).map(x -> x.name()).toArray(String[]::new)));
    this.comboQuality.setModel(new DefaultComboBoxModel<>(Stream.of(Quality.values()).map(x -> x.getViewName()).toArray(String[]::new)));
    this.comboSpeedProfile.setModel(new DefaultComboBoxModel<>(Stream.of(SpeedProfile.values()).map(x -> x.getViewName()).toArray(String[]::new)));
    
    this.comboInterface.setModel(new DefaultComboBoxModel<>(ne.toArray(new String[ne.size()])));
    this.comboInterface.setSelectedItem(data.getServerHost());
    this.spinnerServerPort.setValue(data.getServerPort());
    this.spinnerSoundOffset.setValue(data.getSoundOffset());
    this.textFieldKodiAddress.setText(data.getKodiAddress());
    this.spinnerKodiPort.setValue(data.getKodiPort());
    this.textFieldKodiName.setText(data.getKodiName());
    this.textFieldKodiPassword.setText(data.getKodiPassword());

    this.textFieldFfmpeg.setText(data.getFfmpegPath());
    this.checkGrabCursor.setSelected(data.isGrabCursor());
    this.comboGrabberType.setSelectedItem(data.getGrabberType().name());
    this.comboQuality.setSelectedItem(data.getQuality().getViewName());
    this.comboSpeedProfile.setSelectedItem(data.getSpeedProfile().getViewName());
    this.spinnerSnapsPerSecond.setValue(data.getSnapsPerSecond());
    this.spinnerBandwidth.setValue(data.getBandwidth());
    
    this.checkServerSsl.setSelected(data.isServerSsl());
    this.checkKodiSsl.setSelected(data.isKodiSsl());
    
    DocumentWrapper.of(this.textFieldFfmpeg.getDocument(), x -> data.setFfmpegPath(x));
    DocumentWrapper.of(this.textFieldKodiAddress.getDocument(), x -> data.setKodiAddress(x));
    DocumentWrapper.of(this.textFieldKodiName.getDocument(), x -> data.setKodiName(x));
    DocumentWrapper.of(this.textFieldKodiPassword.getDocument(), x -> data.setKodiPassword(x));
  }

  public Data getData() {
    return this.currentData;
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

    panelServerOptions = new javax.swing.JPanel();
    jLabel1 = new javax.swing.JLabel();
    jLabel2 = new javax.swing.JLabel();
    spinnerServerPort = new javax.swing.JSpinner();
    comboInterface = new javax.swing.JComboBox<>();
    filler2 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
    jLabel3 = new javax.swing.JLabel();
    checkServerSsl = new javax.swing.JCheckBox();
    panelKodiOptions = new javax.swing.JPanel();
    labelKodiAddress = new javax.swing.JLabel();
    labelKodiPort = new javax.swing.JLabel();
    labelKodiName = new javax.swing.JLabel();
    labelKodiPassword = new javax.swing.JLabel();
    textFieldKodiAddress = new javax.swing.JTextField();
    textFieldKodiName = new javax.swing.JTextField();
    textFieldKodiPassword = new javax.swing.JTextField();
    spinnerKodiPort = new javax.swing.JSpinner();
    buttonTestKodiConnection = new javax.swing.JButton();
    jLabel4 = new javax.swing.JLabel();
    checkKodiSsl = new javax.swing.JCheckBox();
    panelScreenCast = new javax.swing.JPanel();
    jLabel5 = new javax.swing.JLabel();
    textFieldFfmpeg = new javax.swing.JTextField();
    jLabel6 = new javax.swing.JLabel();
    spinnerSnapsPerSecond = new javax.swing.JSpinner();
    jLabel7 = new javax.swing.JLabel();
    checkGrabCursor = new javax.swing.JCheckBox();
    jLabel8 = new javax.swing.JLabel();
    comboSoundLine = new javax.swing.JComboBox<>();
    jLabel9 = new javax.swing.JLabel();
    spinnerBandwidth = new javax.swing.JSpinner();
    jLabel10 = new javax.swing.JLabel();
    comboQuality = new javax.swing.JComboBox<>();
    jLabel11 = new javax.swing.JLabel();
    spinnerSoundOffset = new javax.swing.JSpinner();
    jLabel12 = new javax.swing.JLabel();
    comboSpeedProfile = new javax.swing.JComboBox<>();
    jLabel13 = new javax.swing.JLabel();
    comboGrabberType = new javax.swing.JComboBox<>();

    setLayout(new java.awt.GridBagLayout());

    panelServerOptions.setBorder(javax.swing.BorderFactory.createTitledBorder("Local server"));
    panelServerOptions.setLayout(new java.awt.GridBagLayout());

    jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel1.setText("Host:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelServerOptions.add(jLabel1, gridBagConstraints);

    jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel2.setText("Port:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelServerOptions.add(jLabel2, gridBagConstraints);

    spinnerServerPort.setModel(new javax.swing.SpinnerNumberModel(0, 0, 65535, 1));
    spinnerServerPort.setToolTipText("Local port, 0 means auto choice");
    spinnerServerPort.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        spinnerServerPortStateChanged(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelServerOptions.add(spinnerServerPort, gridBagConstraints);

    comboInterface.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    comboInterface.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        comboInterfaceActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1000.0;
    panelServerOptions.add(comboInterface, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.weightx = 1000.0;
    panelServerOptions.add(filler2, gridBagConstraints);

    jLabel3.setText("Use SSL:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelServerOptions.add(jLabel3, gridBagConstraints);

    checkServerSsl.setToolTipText("Use HTTPS connectins");
    checkServerSsl.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        checkServerSslActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    panelServerOptions.add(checkServerSsl, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
    add(panelServerOptions, gridBagConstraints);

    panelKodiOptions.setBorder(javax.swing.BorderFactory.createTitledBorder("KODI HTTP control"));
    panelKodiOptions.setLayout(new java.awt.GridBagLayout());

    labelKodiAddress.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelKodiAddress.setText("Address:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelKodiOptions.add(labelKodiAddress, gridBagConstraints);

    labelKodiPort.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelKodiPort.setText("Port:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelKodiOptions.add(labelKodiPort, gridBagConstraints);

    labelKodiName.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelKodiName.setText("Name:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelKodiOptions.add(labelKodiName, gridBagConstraints);

    labelKodiPassword.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelKodiPassword.setText("Password:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelKodiOptions.add(labelKodiPassword, gridBagConstraints);

    textFieldKodiAddress.setToolTipText("Network address of KODI HTTP service");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelKodiOptions.add(textFieldKodiAddress, gridBagConstraints);

    textFieldKodiName.setToolTipText("Authorization name for connection, must be the same as in KODI HTTP service");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelKodiOptions.add(textFieldKodiName, gridBagConstraints);

    textFieldKodiPassword.setToolTipText("Authorization password, must be the same as defined for KODI HTTP service");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelKodiOptions.add(textFieldKodiPassword, gridBagConstraints);

    spinnerKodiPort.setModel(new javax.swing.SpinnerNumberModel(0, 0, 65535, 1));
    spinnerKodiPort.setToolTipText("Port of KODI HTTP service");
    spinnerKodiPort.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        spinnerKodiPortStateChanged(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelKodiOptions.add(spinnerKodiPort, gridBagConstraints);

    buttonTestKodiConnection.setText("Test");
    buttonTestKodiConnection.setToolTipText("Do test request to KODI with provided parameters");
    buttonTestKodiConnection.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonTestKodiConnectionActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelKodiOptions.add(buttonTestKodiConnection, gridBagConstraints);

    jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel4.setText("Use SSL:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelKodiOptions.add(jLabel4, gridBagConstraints);

    checkKodiSsl.setToolTipText("Make requests to KODI through HTTPS");
    checkKodiSsl.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        checkKodiSslActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    panelKodiOptions.add(checkKodiSsl, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    add(panelKodiOptions, gridBagConstraints);

    panelScreenCast.setBorder(javax.swing.BorderFactory.createTitledBorder("Screencast"));
    panelScreenCast.setLayout(new java.awt.GridBagLayout());

    jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel5.setText("FFmpeg command:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    panelScreenCast.add(jLabel5, gridBagConstraints);

    textFieldFfmpeg.setColumns(32);
    textFieldFfmpeg.setToolTipText("FFmpeg execution path");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    gridBagConstraints.weightx = 1.0;
    panelScreenCast.add(textFieldFfmpeg, gridBagConstraints);

    jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel6.setText("Frame rate:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelScreenCast.add(jLabel6, gridBagConstraints);

    spinnerSnapsPerSecond.setModel(new javax.swing.SpinnerNumberModel(1, 1, 50, 1));
    spinnerSnapsPerSecond.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        spinnerSnapsPerSecondStateChanged(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    panelScreenCast.add(spinnerSnapsPerSecond, gridBagConstraints);

    jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel7.setText("Grab cursor:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    panelScreenCast.add(jLabel7, gridBagConstraints);

    checkGrabCursor.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        checkGrabCursorActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    panelScreenCast.add(checkGrabCursor, gridBagConstraints);

    jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel8.setText("Sound source:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    panelScreenCast.add(jLabel8, gridBagConstraints);

    comboSoundLine.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "none" }));
    comboSoundLine.setToolTipText("Line to grab sound data");
    comboSoundLine.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        comboSoundLineActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    panelScreenCast.add(comboSoundLine, gridBagConstraints);

    jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel9.setText("Bandwidth (Mbits):");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    panelScreenCast.add(jLabel9, gridBagConstraints);

    spinnerBandwidth.setModel(new javax.swing.SpinnerNumberModel(1, 1, 64, 1));
    spinnerBandwidth.setToolTipText("Preferred bandwidth for video (Megabits)");
    spinnerBandwidth.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        spinnerBandwidthStateChanged(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    panelScreenCast.add(spinnerBandwidth, gridBagConstraints);

    jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel10.setText("Quality:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelScreenCast.add(jLabel10, gridBagConstraints);

    comboQuality.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    comboQuality.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        comboQualityActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    panelScreenCast.add(comboQuality, gridBagConstraints);

    jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel11.setText("Sound offset (sec):");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelScreenCast.add(jLabel11, gridBagConstraints);

    spinnerSoundOffset.setModel(new javax.swing.SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(-59.0f), Float.valueOf(59.0f), Float.valueOf(0.01f)));
    spinnerSoundOffset.setToolTipText("Time-offset of sound");
    spinnerSoundOffset.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        spinnerSoundOffsetStateChanged(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    panelScreenCast.add(spinnerSoundOffset, gridBagConstraints);

    jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel12.setText("Codec profile:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelScreenCast.add(jLabel12, gridBagConstraints);

    comboSpeedProfile.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    comboSpeedProfile.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        comboSpeedProfileActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    panelScreenCast.add(comboSpeedProfile, gridBagConstraints);

    jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    jLabel13.setText("Grabber type:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    panelScreenCast.add(jLabel13, gridBagConstraints);

    comboGrabberType.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
    comboGrabberType.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        comboGrabberTypeActionPerformed(evt);
      }
    });
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    panelScreenCast.add(comboGrabberType, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    add(panelScreenCast, gridBagConstraints);
  }// </editor-fold>//GEN-END:initComponents

  
  
  private void comboInterfaceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboInterfaceActionPerformed
    this.currentData.setServerHost(this.comboInterface.getSelectedItem().toString());
  }//GEN-LAST:event_comboInterfaceActionPerformed

  private void spinnerServerPortStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerServerPortStateChanged
    this.currentData.setServerPort((Integer) this.spinnerServerPort.getValue());
  }//GEN-LAST:event_spinnerServerPortStateChanged

  private void spinnerKodiPortStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerKodiPortStateChanged
    this.currentData.setKodiPort((Integer) this.spinnerKodiPort.getValue());
  }//GEN-LAST:event_spinnerKodiPortStateChanged

  private void buttonTestKodiConnectionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonTestKodiConnectionActionPerformed
    try {
      final KodiService testKodiService = new KodiService(
              new KodiAddress(
                      this.textFieldKodiAddress.getText(),
                      (Integer) this.spinnerKodiPort.getValue(),
                      this.textFieldKodiName.getText(),
                      this.textFieldKodiPassword.getText(),
                      this.checkKodiSsl.isSelected()
              )
      );
      final ApplicationProperties properties = testKodiService.getAllApplicationProperties();
      JOptionPane.showMessageDialog(this, "Detected kodi: " + properties.getVersion(), "Connection test", JOptionPane.INFORMATION_MESSAGE);
    } catch (MalformedURLException ex) {
      JOptionPane.showMessageDialog(this, "Malformed URL, check KODI address", "Malformed URL", JOptionPane.ERROR_MESSAGE);
    } catch (Throwable ex) {
      JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
    }
  }//GEN-LAST:event_buttonTestKodiConnectionActionPerformed

  private void checkServerSslActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkServerSslActionPerformed
    this.currentData.setServerSsl(this.checkServerSsl.isSelected());
  }//GEN-LAST:event_checkServerSslActionPerformed

  private void checkKodiSslActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkKodiSslActionPerformed
    this.currentData.setKodiSsl(this.checkKodiSsl.isSelected());
  }//GEN-LAST:event_checkKodiSslActionPerformed

  private void checkGrabCursorActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_checkGrabCursorActionPerformed
    this.currentData.setGrabCursor(this.checkGrabCursor.isSelected());
  }//GEN-LAST:event_checkGrabCursorActionPerformed

  private void spinnerSnapsPerSecondStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerSnapsPerSecondStateChanged
    this.currentData.setSnapsPerSecond((Integer) this.spinnerSnapsPerSecond.getValue());
  }//GEN-LAST:event_spinnerSnapsPerSecondStateChanged

  private void comboSoundLineActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboSoundLineActionPerformed
    this.currentData.setSoundInput(this.comboSoundLine.getSelectedIndex() == 0 ? "" : this.comboSoundLine.getSelectedItem().toString());
    if (this.comboSoundLine.getSelectedIndex() == 0) {
      this.comboSoundLine.setToolTipText("Found sound source to be grabbed during screencast");
    } else {
      this.comboSoundLine.setToolTipText(this.comboSoundLine.getItemAt(this.comboSoundLine.getSelectedIndex()));
    }
  }//GEN-LAST:event_comboSoundLineActionPerformed

  private void comboQualityActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboQualityActionPerformed
    this.currentData.setQuality(Quality.findForViewName(this.comboQuality.getSelectedItem().toString()));
  }//GEN-LAST:event_comboQualityActionPerformed

  private void spinnerBandwidthStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerBandwidthStateChanged
    this.currentData.setBandwidth((Integer)this.spinnerBandwidth.getValue());
  }//GEN-LAST:event_spinnerBandwidthStateChanged

  private void spinnerSoundOffsetStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_spinnerSoundOffsetStateChanged
    this.currentData.setSoundOffset((Float)this.spinnerSoundOffset.getValue());
  }//GEN-LAST:event_spinnerSoundOffsetStateChanged

  private void comboSpeedProfileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboSpeedProfileActionPerformed
    this.currentData.setSpeedProfile(SpeedProfile.findForViewName(this.comboSpeedProfile.getSelectedItem().toString()));
  }//GEN-LAST:event_comboSpeedProfileActionPerformed

  private void comboGrabberTypeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboGrabberTypeActionPerformed
    this.currentData.setGrabberType(GrabberType.findForName(this.comboGrabberType.getSelectedItem().toString()));
  }//GEN-LAST:event_comboGrabberTypeActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton buttonTestKodiConnection;
  private javax.swing.JCheckBox checkGrabCursor;
  private javax.swing.JCheckBox checkKodiSsl;
  private javax.swing.JCheckBox checkServerSsl;
  private javax.swing.JComboBox<String> comboGrabberType;
  private javax.swing.JComboBox<String> comboInterface;
  private javax.swing.JComboBox<String> comboQuality;
  private javax.swing.JComboBox<String> comboSoundLine;
  private javax.swing.JComboBox<String> comboSpeedProfile;
  private javax.swing.Box.Filler filler2;
  private javax.swing.JLabel jLabel1;
  private javax.swing.JLabel jLabel10;
  private javax.swing.JLabel jLabel11;
  private javax.swing.JLabel jLabel12;
  private javax.swing.JLabel jLabel13;
  private javax.swing.JLabel jLabel2;
  private javax.swing.JLabel jLabel3;
  private javax.swing.JLabel jLabel4;
  private javax.swing.JLabel jLabel5;
  private javax.swing.JLabel jLabel6;
  private javax.swing.JLabel jLabel7;
  private javax.swing.JLabel jLabel8;
  private javax.swing.JLabel jLabel9;
  private javax.swing.JLabel labelKodiAddress;
  private javax.swing.JLabel labelKodiName;
  private javax.swing.JLabel labelKodiPassword;
  private javax.swing.JLabel labelKodiPort;
  private javax.swing.JPanel panelKodiOptions;
  private javax.swing.JPanel panelScreenCast;
  private javax.swing.JPanel panelServerOptions;
  private javax.swing.JSpinner spinnerBandwidth;
  private javax.swing.JSpinner spinnerKodiPort;
  private javax.swing.JSpinner spinnerServerPort;
  private javax.swing.JSpinner spinnerSnapsPerSecond;
  private javax.swing.JSpinner spinnerSoundOffset;
  private javax.swing.JTextField textFieldFfmpeg;
  private javax.swing.JTextField textFieldKodiAddress;
  private javax.swing.JTextField textFieldKodiName;
  private javax.swing.JTextField textFieldKodiPassword;
  // End of variables declaration//GEN-END:variables
}
