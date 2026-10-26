package com.igormaznitsa.ravikoodi;

import com.igormaznitsa.ravikoodi.ApplicationPreferences.GrabberType;
import com.igormaznitsa.ravikoodi.ApplicationPreferences.Quality;
import com.igormaznitsa.ravikoodi.ApplicationPreferences.SpeedProfile;
import com.igormaznitsa.ravikoodi.kodijsonapi.ApplicationProperties;
import com.igormaznitsa.ravikoodi.kodijsonapi.KodiService;
import com.igormaznitsa.ravikoodi.screencast.JavaSoundAdapter;
import com.igormaznitsa.ravikoodi.ytloader.YtMode;
import com.igormaznitsa.ravikoodi.ytloader.YtQuality;
import com.igormaznitsa.ravikoodi.ytloader.YtVideoType;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.MalformedURLException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("all")
public class OptionsPanel extends javax.swing.JPanel {

    private static final Logger LOGGER = LoggerFactory.getLogger(OptionsPanel.class);
    private final Data currentData;
    private JButton buttonTestKodiConnection;
    private JCheckBox checkGrabCursor;
    private JCheckBox checkKodiSsl;
    private JCheckBox checkServerSsl;
    private JComboBox<YtMode> comboBoxYoutubeMode;
    private JComboBox<YtQuality> comboBoxYoutubePreferredQuality;
    private JComboBox<YtVideoType> comboBoxYoutubeRequiredFormat;
    private JComboBox<String> comboGrabberType;
    private JComboBox<String> comboInterface;
    private JComboBox<String> comboQuality;
    private JComboBox<String> comboSoundLine;
    private JComboBox<String> comboSpeedProfile;
    private Box.Filler filler1;
    private Box.Filler filler2;
    private Box.Filler filler3;
    private Box.Filler filler4;
    private Box.Filler filler5;
    private JLabel jLabel1;
    private JLabel jLabel10;
    private JLabel jLabel11;
    private JLabel jLabel12;
    private JLabel jLabel13;
    private JLabel jLabel14;
    private JLabel jLabel15;
    private JLabel jLabel16;
    private JLabel jLabel17;
    private JLabel jLabel18;
    private JLabel jLabel19;
    private JLabel jLabel2;
    private JLabel jLabel3;
    private JLabel jLabel4;
    private JLabel jLabel5;
    private JLabel jLabel6;
    private JLabel jLabel7;
    private JLabel jLabel8;
    private JLabel jLabel9;
    private JLabel labelKodiAddress;
    private JLabel labelKodiName;
    private JLabel labelKodiPassword;
    private JLabel labelKodiPort;
    private JLabel labelKodiRpcTimeout;
    private JLabel labelScaleUi;
    private JPanel panelGeneral;
    private JPanel panelKodiOptions;
    private JPanel panelScreenCast;
    private JPanel panelServerOptions;
    private JSpinner spinnerBandwidth;
    private JSpinner spinnerCrf;
    private JSpinner spinnerGrabThreads;
    private JSpinner spinnerKodiPort;
    private JSpinner spinnerRpcTimeout;
    private JSpinner spinnerScaleUi;
    private JSpinner spinnerServerPort;
    private JSpinner spinnerSnapsPerSecond;
    private JSpinner spinnerSoundOffset;
    private JPanel tabGeneral;
    private JTabbedPane tabPanel;
    private JPanel tabScreencast;
    private JPanel tabYoutube;
    private JTextField textFieldFfmpeg;
    private JTextField textFieldKodiAddress;
    private JTextField textFieldKodiName;
    private JTextField textFieldKodiPassword;

    public OptionsPanel(@NonNull final Data data, @NonNull final JavaSoundAdapter soundAdapter) {
        this.currentData = data;
        initComponents();
        this.comboSoundLine.setPrototypeDisplayValue("############################");
        final List<String> ne = new ArrayList<>();

        try {
            final Enumeration<NetworkInterface> e = NetworkInterface.getNetworkInterfaces();
            while (e.hasMoreElements()) {
                e.nextElement().getInterfaceAddresses().stream()
                    .map(a -> a.getAddress().getHostAddress())
                    .collect(Collectors.toCollection(() -> ne));
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
        soundAdapter.getAvailableSoundPorts().forEach((port) -> {
            inputLines.add(port.getName());
        });

        this.spinnerKodiPort.setEditor(new JSpinner.NumberEditor(this.spinnerKodiPort, "#"));
        this.spinnerGrabThreads.setEditor(new JSpinner.NumberEditor(this.spinnerGrabThreads, "##"));
        this.spinnerServerPort.setEditor(new JSpinner.NumberEditor(this.spinnerServerPort, "#"));
        this.spinnerSnapsPerSecond.setEditor(
            new JSpinner.NumberEditor(this.spinnerSnapsPerSecond, "##"));
        this.spinnerBandwidth.setEditor(new JSpinner.NumberEditor(this.spinnerBandwidth, "##"));
        this.spinnerSoundOffset.setEditor(
            new JSpinner.NumberEditor(this.spinnerSoundOffset, "##.##"));

        this.comboSoundLine.setModel(
            new DefaultComboBoxModel<>(inputLines.toArray(new String[inputLines.size()])));
        this.comboSoundLine.setSelectedItem(data.getSoundInput());

        this.comboGrabberType.setModel(new DefaultComboBoxModel<>(
            Stream.of(GrabberType.values()).map(x -> x.name()).toArray(String[]::new)));
        this.comboQuality.setModel(new DefaultComboBoxModel<>(
            Stream.of(Quality.values()).map(x -> x.getViewName()).toArray(String[]::new)));
        this.comboSpeedProfile.setModel(new DefaultComboBoxModel<>(
            Stream.of(SpeedProfile.values()).map(x -> x.getViewName()).toArray(String[]::new)));

        this.comboInterface.setModel(new DefaultComboBoxModel<>(ne.toArray(new String[ne.size()])));
        this.comboInterface.setSelectedItem(data.getServerHost());
        this.spinnerServerPort.setValue(data.getServerPort());
        this.spinnerSoundOffset.setValue(data.getSoundOffset());
        this.textFieldKodiAddress.setText(data.getKodiAddress());
        this.spinnerKodiPort.setValue(data.getKodiPort());
        this.textFieldKodiName.setText(data.getKodiName());
        this.textFieldKodiPassword.setText(data.getKodiPassword());

        this.spinnerScaleUi.setValue(data.getScaleUi());

        this.textFieldFfmpeg.setText(data.getFfmpegPath());
        this.checkGrabCursor.setSelected(data.isGrabCursor());
        this.comboGrabberType.setSelectedItem(data.getGrabberType().name());
        this.comboQuality.setSelectedItem(data.getQuality().getViewName());
        this.comboSpeedProfile.setSelectedItem(data.getSpeedProfile().getViewName());
        this.spinnerSnapsPerSecond.setValue(data.getSnapsPerSecond());
        this.spinnerCrf.setValue(data.getCrf());
        this.spinnerBandwidth.setValue(data.getBandwidth());
        this.spinnerGrabThreads.setValue(data.getThreads());

        this.checkServerSsl.setSelected(data.isServerSsl());
        this.checkKodiSsl.setSelected(data.isKodiSsl());

        this.comboBoxYoutubeMode.setSelectedItem(data.getYoutubeOpenUrlMode());
        this.comboBoxYoutubePreferredQuality.setSelectedItem(data.getYoutubePreferredQuality());
        this.comboBoxYoutubeRequiredFormat.setSelectedItem(data.getYoutubeRequiredFormat());

        this.spinnerRpcTimeout.setValue(data.jsonRequestTimeout);

        DocumentWrapper.of(this.textFieldFfmpeg.getDocument(), x -> data.setFfmpegPath(x));
        DocumentWrapper.of(this.textFieldKodiAddress.getDocument(), x -> data.setKodiAddress(x));
        DocumentWrapper.of(this.textFieldKodiName.getDocument(), x -> data.setKodiName(x));
        DocumentWrapper.of(this.textFieldKodiPassword.getDocument(), x -> data.setKodiPassword(x));

        this.onUpdatedYoutubeMode();
    }

    private void onUpdatedYoutubeMode() {
        final YtMode mode = (YtMode) this.comboBoxYoutubeMode.getSelectedItem();
        this.comboBoxYoutubePreferredQuality.setEnabled(mode == YtMode.DIRECT_URL);
        this.comboBoxYoutubeRequiredFormat.setEnabled(mode == YtMode.DIRECT_URL);
    }

    public Data getData() {
        return this.currentData;
    }

    @SuppressWarnings("unchecked")
    private void initComponents() {
        GridBagConstraints gridBagConstraints;

        tabPanel = new JTabbedPane();
        tabGeneral = new JPanel();
        panelGeneral = new JPanel();
        labelScaleUi = new JLabel();
        spinnerScaleUi = new JSpinner();
        filler1 =
            new Box.Filler(new Dimension(0, 0), new Dimension(0, 0),
                new Dimension(32767, 0));
        jLabel15 = new JLabel();
        panelServerOptions = new JPanel();
        jLabel1 = new JLabel();
        jLabel2 = new JLabel();
        spinnerServerPort = new JSpinner();
        comboInterface = new JComboBox<>();
        filler2 =
            new Box.Filler(new Dimension(0, 0), new Dimension(0, 0),
                new Dimension(32767, 0));
        jLabel3 = new JLabel();
        checkServerSsl = new JCheckBox();
        panelKodiOptions = new JPanel();
        labelKodiAddress = new JLabel();
        labelKodiPort = new JLabel();
        labelKodiName = new JLabel();
        labelKodiPassword = new JLabel();
        textFieldKodiAddress = new JTextField();
        textFieldKodiName = new JTextField();
        textFieldKodiPassword = new JTextField();
        spinnerKodiPort = new JSpinner();
        buttonTestKodiConnection = new JButton();
        jLabel4 = new JLabel();
        checkKodiSsl = new JCheckBox();
        labelKodiRpcTimeout = new JLabel();
        spinnerRpcTimeout = new JSpinner();
        filler3 =
            new Box.Filler(new Dimension(0, 0), new Dimension(0, 0),
                new Dimension(0, 32767));
        tabScreencast = new JPanel();
        panelScreenCast = new JPanel();
        jLabel5 = new JLabel();
        textFieldFfmpeg = new JTextField();
        jLabel6 = new JLabel();
        spinnerSnapsPerSecond = new JSpinner();
        jLabel7 = new JLabel();
        checkGrabCursor = new JCheckBox();
        jLabel8 = new JLabel();
        comboSoundLine = new JComboBox<>();
        jLabel9 = new JLabel();
        spinnerBandwidth = new JSpinner();
        jLabel10 = new JLabel();
        comboQuality = new JComboBox<>();
        jLabel11 = new JLabel();
        spinnerSoundOffset = new JSpinner();
        jLabel12 = new JLabel();
        comboSpeedProfile = new JComboBox<>();
        jLabel13 = new JLabel();
        comboGrabberType = new JComboBox<>();
        jLabel14 = new JLabel();
        spinnerGrabThreads = new JSpinner();
        jLabel16 = new JLabel();
        spinnerCrf = new JSpinner();
        tabYoutube = new JPanel();
        jLabel17 = new JLabel();
        jLabel18 = new JLabel();
        jLabel19 = new JLabel();
        filler4 =
            new Box.Filler(new Dimension(0, 0), new Dimension(0, 0),
                new Dimension(0, 32767));
        comboBoxYoutubePreferredQuality = new JComboBox<>();
        comboBoxYoutubeRequiredFormat = new JComboBox<>();
        filler5 =
            new Box.Filler(new Dimension(0, 0), new Dimension(0, 0),
                new Dimension(32767, 0));
        comboBoxYoutubeMode = new JComboBox<>();

        setLayout(new BorderLayout());

        tabGeneral.setLayout(new GridBagLayout());

        panelGeneral.setBorder(javax.swing.BorderFactory.createTitledBorder("UI"));
        panelGeneral.setLayout(new GridBagLayout());

        labelScaleUi.setText("Scale UI: ");
        labelScaleUi.setToolTipText("Zoom all interface elements");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelGeneral.add(labelScaleUi, gridBagConstraints);

        spinnerScaleUi.setModel(new SpinnerNumberModel(1, 0, 5, 1));
        spinnerScaleUi.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                spinnerScaleUiStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelGeneral.add(spinnerScaleUi, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1111.0;
        panelGeneral.add(filler1, gridBagConstraints);

        jLabel15.setForeground(new Color(255, 0, 0));
        jLabel15.setText(" Requires restart ");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        panelGeneral.add(jLabel15, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        tabGeneral.add(panelGeneral, gridBagConstraints);

        panelServerOptions.setBorder(javax.swing.BorderFactory.createTitledBorder("Local server"));
        panelServerOptions.setLayout(new GridBagLayout());

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("Host:");
        jLabel1.setToolTipText("Network interface to listen incoming connections");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelServerOptions.add(jLabel1, gridBagConstraints);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Port:");
        jLabel2.setToolTipText("Port to listen incoming connections");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelServerOptions.add(jLabel2, gridBagConstraints);

        spinnerServerPort.setModel(new SpinnerNumberModel(0, 0, 65535, 1));
        spinnerServerPort.setToolTipText("Local port, 0 means auto choice");
        spinnerServerPort.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                spinnerServerPortStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelServerOptions.add(spinnerServerPort, gridBagConstraints);

        comboInterface.setModel(new DefaultComboBoxModel<>(
            new String[] {"Item 1", "Item 2", "Item 3", "Item 4"}));
        comboInterface.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                comboInterfaceActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.weightx = 1000.0;
        panelServerOptions.add(comboInterface, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.weightx = 1000.0;
        panelServerOptions.add(filler2, gridBagConstraints);

        jLabel3.setText("Use SSL:");
        jLabel3.setToolTipText("If turned on then internal server will be working in SSL mode");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelServerOptions.add(jLabel3, gridBagConstraints);

        checkServerSsl.setToolTipText("Use HTTPS connectins");
        checkServerSsl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                checkServerSslActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelServerOptions.add(checkServerSsl, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        tabGeneral.add(panelServerOptions, gridBagConstraints);

        panelKodiOptions.setBorder(
            javax.swing.BorderFactory.createTitledBorder("KODI HTTP control"));
        panelKodiOptions.setLayout(new GridBagLayout());

        labelKodiAddress.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labelKodiAddress.setText("Address:");
        labelKodiAddress.setToolTipText("Network address of KODI device");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelKodiOptions.add(labelKodiAddress, gridBagConstraints);

        labelKodiPort.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labelKodiPort.setText("Port:");
        labelKodiPort.setToolTipText("Network port of KODI device");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelKodiOptions.add(labelKodiPort, gridBagConstraints);

        labelKodiName.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        labelKodiName.setText("Name:");
        labelKodiName.setToolTipText("Login for KODI device");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelKodiOptions.add(labelKodiName, gridBagConstraints);

        labelKodiPassword.setHorizontalAlignment(SwingConstants.RIGHT);
        labelKodiPassword.setText("Password:");
        labelKodiPassword.setToolTipText("Password for KODI device");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelKodiOptions.add(labelKodiPassword, gridBagConstraints);

        textFieldKodiAddress.setToolTipText("Network address of KODI HTTP service");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelKodiOptions.add(textFieldKodiAddress, gridBagConstraints);

        textFieldKodiName.setToolTipText(
            "Authorization name for connection, must be the same as in KODI HTTP service");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelKodiOptions.add(textFieldKodiName, gridBagConstraints);

        textFieldKodiPassword.setToolTipText(
            "Authorization password, must be the same as defined for KODI HTTP service");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelKodiOptions.add(textFieldKodiPassword, gridBagConstraints);

        spinnerKodiPort.setModel(new SpinnerNumberModel(0, 0, 65535, 1));
        spinnerKodiPort.setToolTipText("Port of KODI HTTP service");
        spinnerKodiPort.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                spinnerKodiPortStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelKodiOptions.add(spinnerKodiPort, gridBagConstraints);

        buttonTestKodiConnection.setText("Test");
        buttonTestKodiConnection.setToolTipText("Do test request to KODI with provided parameters");
        buttonTestKodiConnection.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                buttonTestKodiConnectionActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelKodiOptions.add(buttonTestKodiConnection, gridBagConstraints);

        jLabel4.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabel4.setText("Use SSL:");
        jLabel4.setToolTipText("If turned on then all requests to KODI device will be through SSL");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelKodiOptions.add(jLabel4, gridBagConstraints);

        checkKodiSsl.setToolTipText("Make requests to KODI through HTTPS");
        checkKodiSsl.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                checkKodiSslActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelKodiOptions.add(checkKodiSsl, gridBagConstraints);

        labelKodiRpcTimeout.setHorizontalAlignment(SwingConstants.RIGHT);
        labelKodiRpcTimeout.setText("RPC Timeout (ms):");
        labelKodiRpcTimeout.setToolTipText("Timeout for network requests");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelKodiOptions.add(labelKodiRpcTimeout, gridBagConstraints);

        spinnerRpcTimeout.setModel(
            new SpinnerNumberModel(Long.valueOf(100L), Long.valueOf(1L),
                Long.valueOf(60000L), Long.valueOf(1L)));
        spinnerRpcTimeout.setToolTipText("Timeout allowed for RPC Json call to KODI player");
        spinnerRpcTimeout.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                spinnerRpcTimeoutStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelKodiOptions.add(spinnerRpcTimeout, gridBagConstraints);

        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        tabGeneral.add(panelKodiOptions, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.weighty = 1000.0;
        tabGeneral.add(filler3, gridBagConstraints);

        tabPanel.addTab("General", tabGeneral);

        tabScreencast.setLayout(new BorderLayout());

        panelScreenCast.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelScreenCast.setLayout(new GridBagLayout());

        jLabel5.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabel5.setText("FFmpeg command:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panelScreenCast.add(jLabel5, gridBagConstraints);

        textFieldFfmpeg.setColumns(32);
        textFieldFfmpeg.setToolTipText("FFmpeg execution path");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1.0;
        panelScreenCast.add(textFieldFfmpeg, gridBagConstraints);

        jLabel6.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabel6.setText("Frame rate:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelScreenCast.add(jLabel6, gridBagConstraints);

        spinnerSnapsPerSecond.setModel(new SpinnerNumberModel(1, 1, 50, 1));
        spinnerSnapsPerSecond.addChangeListener(new ChangeListener() {
            public void stateChanged(ChangeEvent evt) {
                spinnerSnapsPerSecondStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelScreenCast.add(spinnerSnapsPerSecond, gridBagConstraints);

        jLabel7.setHorizontalAlignment(SwingConstants.RIGHT);
        jLabel7.setText("Grab cursor:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelScreenCast.add(jLabel7, gridBagConstraints);

        checkGrabCursor.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                checkGrabCursorActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 9;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelScreenCast.add(checkGrabCursor, gridBagConstraints);

        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel8.setText("Sound source:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelScreenCast.add(jLabel8, gridBagConstraints);

        comboSoundLine.setModel(new DefaultComboBoxModel<>(new String[] {"none"}));
        comboSoundLine.setToolTipText("Line to grab sound data");
        comboSoundLine.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                comboSoundLineActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelScreenCast.add(comboSoundLine, gridBagConstraints);

        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel9.setText("Bandwidth (Mbits):");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.EAST;
        panelScreenCast.add(jLabel9, gridBagConstraints);

        spinnerBandwidth.setModel(new SpinnerNumberModel(1, 1, 64, 1));
        spinnerBandwidth.setToolTipText("Preferred bandwidth for video (Megabits)");
        spinnerBandwidth.addChangeListener(new ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerBandwidthStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelScreenCast.add(spinnerBandwidth, gridBagConstraints);

        jLabel10.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel10.setText("Quality:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelScreenCast.add(jLabel10, gridBagConstraints);

        comboQuality.setModel(new DefaultComboBoxModel<>(
            new String[] {"Item 1", "Item 2", "Item 3", "Item 4"}));
        comboQuality.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                comboQualityActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelScreenCast.add(comboQuality, gridBagConstraints);

        jLabel11.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel11.setText("Sound offset (sec):");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelScreenCast.add(jLabel11, gridBagConstraints);

        spinnerSoundOffset.setModel(
            new SpinnerNumberModel(Float.valueOf(0.0f), Float.valueOf(-59.0f),
                Float.valueOf(59.0f), Float.valueOf(0.01f)));
        spinnerSoundOffset.setToolTipText("Time-offset of sound");
        spinnerSoundOffset.addChangeListener(new ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerSoundOffsetStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 8;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelScreenCast.add(spinnerSoundOffset, gridBagConstraints);

        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel12.setText("Codec profile:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelScreenCast.add(jLabel12, gridBagConstraints);

        comboSpeedProfile.setModel(new DefaultComboBoxModel<>(
            new String[] {"Item 1", "Item 2", "Item 3", "Item 4"}));
        comboSpeedProfile.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                comboSpeedProfileActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelScreenCast.add(comboSpeedProfile, gridBagConstraints);

        jLabel13.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel13.setText("Grabber type:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelScreenCast.add(jLabel13, gridBagConstraints);

        comboGrabberType.setModel(new DefaultComboBoxModel<>(
            new String[] {"Item 1", "Item 2", "Item 3", "Item 4"}));
        comboGrabberType.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                comboGrabberTypeActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelScreenCast.add(comboGrabberType, gridBagConstraints);

        jLabel14.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel14.setText("Threads:");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelScreenCast.add(jLabel14, gridBagConstraints);

        spinnerGrabThreads.setModel(new SpinnerNumberModel(0, 0, 99, 1));
        spinnerGrabThreads.addChangeListener(new ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerGrabThreadsStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        panelScreenCast.add(spinnerGrabThreads, gridBagConstraints);

        jLabel16.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel16.setText("CRF:");
        jLabel16.setToolTipText(
            "Constant Rate Factor, if -1 - turn off, 0 - lossless, 51 - worst quality");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        panelScreenCast.add(jLabel16, gridBagConstraints);

        spinnerCrf.setModel(new SpinnerNumberModel(-1, -1, 51, 1));
        spinnerCrf.setToolTipText("");
        spinnerCrf.addChangeListener(new ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                spinnerCrfStateChanged(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 10;
        gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
        panelScreenCast.add(spinnerCrf, gridBagConstraints);

        tabScreencast.add(panelScreenCast, BorderLayout.NORTH);

        tabPanel.addTab("Screencast", tabScreencast);

        tabYoutube.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 8, 8, 8));
        tabYoutube.setLayout(new GridBagLayout());

        jLabel17.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel17.setText("Youtube URL mode:");
        jLabel17.setToolTipText("Mode to process Youtube URL");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        tabYoutube.add(jLabel17, gridBagConstraints);

        jLabel18.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel18.setText("Preferred video quality:");
        jLabel18.setToolTipText("Preferred quality of opened Youtube video through direct link");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        tabYoutube.add(jLabel18, gridBagConstraints);

        jLabel19.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel19.setText("Required video format:");
        jLabel19.setToolTipText("Required video format to be opened through direct link");
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        tabYoutube.add(jLabel19, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.weighty = 1000.0;
        tabYoutube.add(filler4, gridBagConstraints);

        comboBoxYoutubePreferredQuality.setModel(new DefaultComboBoxModel<>(
            Arrays.stream(YtQuality.values()).filter(x -> x != YtQuality.UNKNOWN)
                .toArray(x -> new YtQuality[x]))
        );
        comboBoxYoutubePreferredQuality.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                comboBoxYoutubePreferredQualityActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        tabYoutube.add(comboBoxYoutubePreferredQuality, gridBagConstraints);

        comboBoxYoutubeRequiredFormat.setModel(new DefaultComboBoxModel<>(
            Arrays.stream(YtVideoType.values()).filter(x -> x != YtVideoType.UNKNOWN)
                .toArray(x -> new YtVideoType[x]))
        );
        comboBoxYoutubeRequiredFormat.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                comboBoxYoutubeRequiredFormatActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        tabYoutube.add(comboBoxYoutubeRequiredFormat, gridBagConstraints);
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.weightx = 1000.0;
        tabYoutube.add(filler5, gridBagConstraints);

        comboBoxYoutubeMode.setModel(new DefaultComboBoxModel<>(YtMode.values()));
        comboBoxYoutubeMode.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                comboBoxYoutubeModeActionPerformed(evt);
            }
        });
        gridBagConstraints = new GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = GridBagConstraints.WEST;
        tabYoutube.add(comboBoxYoutubeMode, gridBagConstraints);

        tabPanel.addTab("Youtube", tabYoutube);

        add(tabPanel, BorderLayout.CENTER);
    }

    private void comboInterfaceActionPerformed(ActionEvent evt) {
        this.currentData.setServerHost(this.comboInterface.getSelectedItem().toString());
    }

    private void spinnerServerPortStateChanged(javax.swing.event.ChangeEvent evt) {
        this.currentData.setServerPort((Integer) this.spinnerServerPort.getValue());
    }

    private void spinnerKodiPortStateChanged(javax.swing.event.ChangeEvent evt) {
        this.currentData.setKodiPort((Integer) this.spinnerKodiPort.getValue());
    }

    private void buttonTestKodiConnectionActionPerformed(ActionEvent evt) {
        try {
            final KodiService testKodiService = new KodiService(
                new KodiAddress(
                    this.textFieldKodiAddress.getText(),
                    (Integer) this.spinnerKodiPort.getValue(),
                    this.textFieldKodiName.getText(),
                    this.textFieldKodiPassword.getText(),
                    this.checkKodiSsl.isSelected()
                ), Duration.ofMillis(((Number) this.spinnerRpcTimeout.getValue()).longValue())
            );
            final ApplicationProperties properties = testKodiService.getAllApplicationProperties();
            JOptionPane.showMessageDialog(this, "Detected kodi: " + properties.getVersion(),
                "Connection test", JOptionPane.INFORMATION_MESSAGE);
        } catch (MalformedURLException ex) {
            JOptionPane.showMessageDialog(this, "Malformed URL, check KODI address",
                "Malformed URL",
                JOptionPane.ERROR_MESSAGE);
        } catch (Throwable ex) {
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }

    private void checkServerSslActionPerformed(ActionEvent evt) {
        this.currentData.setServerSsl(this.checkServerSsl.isSelected());
    }

    private void checkKodiSslActionPerformed(ActionEvent evt) {
        this.currentData.setKodiSsl(this.checkKodiSsl.isSelected());
    }

    private void checkGrabCursorActionPerformed(ActionEvent evt) {
        this.currentData.setGrabCursor(this.checkGrabCursor.isSelected());
    }

    private void spinnerSnapsPerSecondStateChanged(javax.swing.event.ChangeEvent evt) {
        this.currentData.setSnapsPerSecond((Integer) this.spinnerSnapsPerSecond.getValue());
    }

    private void comboSoundLineActionPerformed(ActionEvent evt) {
        this.currentData.setSoundInput(this.comboSoundLine.getSelectedIndex() == 0 ? "" :
            this.comboSoundLine.getSelectedItem().toString());
        if (this.comboSoundLine.getSelectedIndex() == 0) {
            this.comboSoundLine.setToolTipText(
                "Found sound source to be grabbed during screencast");
        } else {
            this.comboSoundLine.setToolTipText(
                this.comboSoundLine.getItemAt(this.comboSoundLine.getSelectedIndex()));
        }
    }

    private void comboQualityActionPerformed(ActionEvent evt) {
        this.currentData.setQuality(
            Quality.findForViewName(this.comboQuality.getSelectedItem().toString()));
    }

    private void spinnerBandwidthStateChanged(javax.swing.event.ChangeEvent evt) {
        this.currentData.setBandwidth((Integer) this.spinnerBandwidth.getValue());
    }

    private void spinnerSoundOffsetStateChanged(javax.swing.event.ChangeEvent evt) {
        this.currentData.setSoundOffset((Float) this.spinnerSoundOffset.getValue());
    }

    private void comboSpeedProfileActionPerformed(ActionEvent evt) {
        this.currentData.setSpeedProfile(
            SpeedProfile.findForViewName(this.comboSpeedProfile.getSelectedItem().toString()));
    }

    private void comboGrabberTypeActionPerformed(ActionEvent evt) {
        this.currentData.setGrabberType(
            GrabberType.findForName(this.comboGrabberType.getSelectedItem().toString()));
    }

    private void spinnerGrabThreadsStateChanged(javax.swing.event.ChangeEvent evt) {
        this.currentData.setThreads((Integer) this.spinnerGrabThreads.getValue());
    }

    private void spinnerCrfStateChanged(javax.swing.event.ChangeEvent evt) {
        this.currentData.setCrf((Integer) this.spinnerCrf.getValue());
    }

    private void spinnerScaleUiStateChanged(ChangeEvent evt) {
        this.currentData.setScaleUi((Integer) this.spinnerScaleUi.getValue());
    }

    private void spinnerRpcTimeoutStateChanged(ChangeEvent evt) {
        this.currentData.setJsonRequestTimeout(
            ((Number) this.spinnerRpcTimeout.getValue()).longValue());
    }

    private void comboBoxYoutubePreferredQualityActionPerformed(ActionEvent evt) {
        this.currentData.setYoutubePreferredQuality(
            (YtQuality) this.comboBoxYoutubePreferredQuality.getSelectedItem());
    }

    private void comboBoxYoutubeRequiredFormatActionPerformed(ActionEvent evt) {
        this.currentData.setYoutubeRequiredFormat(
            (YtVideoType) this.comboBoxYoutubeRequiredFormat.getSelectedItem());
    }

    private void comboBoxYoutubeModeActionPerformed(ActionEvent evt) {
        this.currentData.setYoutubeOpenUrlMode((YtMode) this.comboBoxYoutubeMode.getSelectedItem());
        this.onUpdatedYoutubeMode();
    }

    public static final class Data {

        private String host;
        private int port;
        private int threads;
        private boolean serverSsl;
        private String kodiAddress;
        private String kodiName;
        private String kodiPassword;
        private String ffmpegPath;
        private boolean grabCursor;
        private int snapsPerSecond;
        private int kodiPort;
        private int scaleUi;
        private String soundInput;
        private int bandwidth;
        private int crf;
        private Quality quality;
        private GrabberType grabberType;
        private SpeedProfile speedProfile;
        private boolean kodiSsl;
        private float soundOffset;
        private long jsonRequestTimeout;
        private YtMode youtubeOpenUrlMode;
        private YtQuality youtubePreferredQuality;
        private YtVideoType youtubeRequiredFormat;

        public Data(@NonNull final ApplicationPreferences preferences) {
            this.youtubeOpenUrlMode = preferences.getYoutubeOpenUrlMode();
            this.youtubePreferredQuality = preferences.getYoutubePreferredQuality();
            this.youtubeRequiredFormat = preferences.getYoutubeRequiredFormat();
            this.threads = preferences.getThreads();
            this.scaleUi = preferences.getScaleUi();
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
            this.jsonRequestTimeout = preferences.getJsonRequestTimeout().toMillis();
            this.crf = preferences.getCrf();
        }

        public void save(@NonNull final ApplicationPreferences preferences) {
            preferences.setYoutubeOpenUrlMode(this.youtubeOpenUrlMode);
            preferences.setYoutubePreferredQuality(this.youtubePreferredQuality);
            preferences.setYoutubeRequiredFormat(this.youtubeRequiredFormat);

            preferences.setScaleUi(this.scaleUi);

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
            preferences.setThreads(this.threads);
            preferences.setCrf(this.crf);

            preferences.setKodiAddress(this.kodiAddress);
            preferences.setKodiName(this.kodiName);
            preferences.setKodiPassword(this.kodiPassword);
            preferences.setKodiSsl(this.kodiSsl);

            preferences.setKodiPort(this.kodiPort);

            preferences.setJsonRequestTimeout(Duration.ofMillis(this.jsonRequestTimeout));

            preferences.flush();
        }

        public YtMode getYoutubeOpenUrlMode() {
            return this.youtubeOpenUrlMode;
        }

        public void setYoutubeOpenUrlMode(final YtMode mode) {
            this.youtubeOpenUrlMode = mode;
        }

        @NonNull
        public YtQuality getYoutubePreferredQuality() {
            return this.youtubePreferredQuality;
        }

        public void setYoutubePreferredQuality(@NonNull final YtQuality youtubePreferredQuality) {
            this.youtubePreferredQuality = youtubePreferredQuality;
        }

        @NonNull
        public YtVideoType getYoutubeRequiredFormat() {
            return this.youtubeRequiredFormat;
        }

        public void setYoutubeRequiredFormat(@NonNull final YtVideoType youtubeRequiredFormat) {
            this.youtubeRequiredFormat = youtubeRequiredFormat;
        }

        public int getCrf() {
            return this.crf;
        }

        public void setCrf(final int value) {
            this.crf = value;
        }

        public int getThreads() {
            return this.threads;
        }

        public void setThreads(final int threads) {
            this.threads = threads;
        }

        public long getJsonRequestTimeout() {
            return this.jsonRequestTimeout;
        }

        public void setJsonRequestTimeout(final long value) {
            this.jsonRequestTimeout = Math.max(0L, value);
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

        public int getScaleUi() {
            return this.scaleUi;
        }

        public void setScaleUi(final int value) {
            this.scaleUi = value;
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

        public void setFfmpegPath(@NonNull final String path) {
            this.ffmpegPath = path;
        }

        public int getSnapsPerSecond() {
            return this.snapsPerSecond;
        }

        public void setSnapsPerSecond(final int value) {
            this.snapsPerSecond = value;
        }

        public boolean isGrabCursor() {
            return this.grabCursor;
        }

        public void setGrabCursor(final boolean value) {
            this.grabCursor = value;
        }

        public boolean isServerSsl() {
            return this.serverSsl;
        }

        public void setServerSsl(final boolean useSsl) {
            this.serverSsl = useSsl;
        }

        public boolean isKodiSsl() {
            return this.kodiSsl;
        }

        public void setKodiSsl(final boolean useSsl) {
            this.kodiSsl = useSsl;
        }

        @NonNull
        public String getServerHost() {
            return this.host;
        }

        public void setServerHost(@NonNull final String host) {
            this.host = host;
        }

        public int getServerPort() {
            return this.port;
        }

        public void setServerPort(final int port) {
            this.port = port;
        }
    }

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
}
