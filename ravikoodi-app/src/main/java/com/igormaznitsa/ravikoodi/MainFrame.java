package com.igormaznitsa.ravikoodi;

import com.github.kiulian.downloader.downloader.YoutubeCallback;
import com.github.kiulian.downloader.downloader.client.ClientType;
import com.github.kiulian.downloader.downloader.request.RequestVideoInfo;
import com.github.kiulian.downloader.model.videos.VideoInfo;
import com.github.kiulian.downloader.model.videos.formats.AudioFormat;
import com.github.kiulian.downloader.model.videos.formats.VideoFormat;
import static com.igormaznitsa.ravikoodi.ContentTreeItem.CONTENT_ITEM_COMPARATOR;
import com.igormaznitsa.ravikoodi.MimeTypes.ContentType;
import static com.igormaznitsa.ravikoodi.Utils.isBlank;
import com.igormaznitsa.ravikoodi.kodijsonapi.ActivePlayerInfo;
import com.igormaznitsa.ravikoodi.kodijsonapi.KodiService;
import com.igormaznitsa.ravikoodi.prefs.StaticResource;
import com.igormaznitsa.ravikoodi.prefs.TimerResource;
import com.igormaznitsa.ravikoodi.screencast.FfmpegWrapper;
import com.igormaznitsa.ravikoodi.screencast.JavaSoundAdapter;
import com.igormaznitsa.ravikoodi.screencast.ScreenGrabber;
import com.igormaznitsa.ravikoodi.timers.TimerScheduler;
import com.igormaznitsa.ravikoodi.timers.TimersTable;
import com.igormaznitsa.ravikoodi.ytloader.YtLinkExtractor;
import com.igormaznitsa.ravikoodi.ytloader.YtQuality;
import com.igormaznitsa.ravikoodi.ytloader.YtVideoType;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Taskbar;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.FlavorEvent;
import java.awt.datatransfer.FlavorListener;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import static javax.swing.tree.TreeSelectionModel.SINGLE_TREE_SELECTION;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

@org.springframework.stereotype.Component
public class MainFrame extends javax.swing.JFrame implements TreeModel, FlavorListener, InternalServer.InternalServerListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainFrame.class);

    private static final int STR_CUT_LEN = 48;

    private Path currentRootFolder;

    private final List<TreeModelListener> treeListeners = new CopyOnWriteArrayList<>();

    @Autowired
    private DonationController donationController;
    @Autowired
    private ScheduledExecutorService executorService;
    @Autowired
    private ApplicationPreferences preferences;
    @Autowired
    private UploadingFileRegistry fileRegstry;
    @Autowired
    private InternalServer server;
    @Autowired
    private MimeTypes mimeTypes;
    @Autowired
    private ApplicationContext context;
    @Autowired
    private BuildProperties buildProperties;
    @Autowired
    private JavaSoundAdapter soundAdapter;
    @Autowired
    private TimerScheduler timerScheduler;
    @Autowired
    private StaticFileRegistry staticFileRegistry;
    @Autowired
    private KodiComm kodiComm;
    @Autowired
    private YtLinkExtractor youTubeLinkExtractor;

    private File lastSelectedFileFolder = null;
    private final AtomicReference<OpeningFileInfoPanel> openingFileInfoPanel = new AtomicReference<>();

    private final AtomicReference<ApplicationStatusPanel> applicationStatusPanel = new AtomicReference<>();

    private final AtomicReference<ScreenGrabber> currentScreenGrabber = new AtomicReference<>();

    private final AtomicLong timeWhenEndScreencastFlowEnable = new AtomicLong();

    private WeakReference<FfmpegWrapper> lastFFmpegWrapper;

    private static class FileTreeRenderer extends DefaultTreeCellRenderer {

        private static final Icon ICON_UNKNOWN = new ImageIcon(Utils.loadImage("tree/mask.png"));
        private static final Icon ICON_AUDIO = new ImageIcon(Utils.loadImage("tree/music.png"));
        private static final Icon ICON_VIDEO = new ImageIcon(Utils.loadImage("tree/movies.png"));
        private static final Icon ICON_PICTURE = new ImageIcon(Utils.loadImage("tree/picture.png"));
        private static final Icon ICON_FOLDER = new ImageIcon(Utils.loadImage("tree/folder.png"));
        private static final Icon ICON_FOLDER_OPEN = new ImageIcon(Utils.loadImage("tree/folder_blue.png"));

        public FileTreeRenderer() {
            super();
        }

        @Override
        public Component getTreeCellRendererComponent(
                final JTree tree,
                final Object value,
                final boolean selected,
                final boolean expanded,
                final boolean leaf,
                final int row,
                final boolean hasFocus
        ) {
            DefaultTreeCellRenderer result = (DefaultTreeCellRenderer) super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            if (leaf) {
                if (value instanceof ContentFile) {
                    switch (((ContentFile) value).getContentType()) {
                        case AUDIO:
                            result.setIcon(ICON_AUDIO);
                            break;
                        case VIDEO:
                            result.setIcon(ICON_VIDEO);
                            break;
                        case PICTURE:
                            result.setIcon(ICON_PICTURE);
                            break;
                        case UNKNOWN:
                            result.setIcon(ICON_UNKNOWN);
                            break;
                    }
                } else {
                    result.setIcon(null);
                }
            } else {
                result.setIcon(expanded ? ICON_FOLDER_OPEN : ICON_FOLDER);
            }
            return result;
        }
    }

    @Override
    public void flavorsChanged(final FlavorEvent e) {
        this.updateToolButtons();
    }

    private final List<ContentTreeItem> videoFiles = new ArrayList<>();

    private void updateToolButtons() {
        final TreePath treePath = this.treeVideoFiles.getSelectionPath();
        final boolean contentFileFocused = treePath != null && treePath.getLastPathComponent() instanceof ContentFile;
        if (this.toggleButtonScreencast.isSelected()) {
            buttonPlaySelected.setEnabled(false);
            buttonImageFromClipboard.setEnabled(false);
        } else {
            buttonPlaySelected.setEnabled(contentFileFocused);
            try {
                this.buttonImageFromClipboard.setEnabled(Toolkit.getDefaultToolkit().getSystemClipboard().isDataFlavorAvailable(DataFlavor.imageFlavor));
            } catch (Exception ex) {
                this.buttonImageFromClipboard.setEnabled(false);
            }
        }
        buttonOpenSelectedFileInSystem.setEnabled(contentFileFocused);
        this.repaint();
    }

    @PostConstruct
    public void postInit() {
        final Throwable lastServerError = this.server.getLastStartServerError();
        if (lastServerError != null) {
            LOGGER.error("Detected error during server start", lastServerError);
            SwingUtilities.invokeLater(() -> {
                final String[] options = new String[]{"Options", "Continue", "Exit"};

                final int result = JOptionPane.showOptionDialog(
                        this,
                        "Can't start embedded server: " + lastServerError.getMessage(),
                        "Error",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.ERROR_MESSAGE,
                        null,
                        options,
                        options[0]
                );

                switch (result) {
                    case JOptionPane.NO_OPTION: {
                        // Do nothing, just continue
                    }
                    break;
                    case JOptionPane.YES_OPTION: {
                        final OptionsPanel.Data container = new OptionsPanel.Data(this.preferences);
                        if (JOptionPane.showConfirmDialog(this, Utils.makeOwningDialogResizable(new JScrollPane(new OptionsPanel(container, this.soundAdapter))), "Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
                            container.save(this.preferences);
                            this.server.restartServer();
                        } else {
                            LOGGER.warn("Preferences not changed, closing");
                            SpringApplication.exit(this.context, () -> 11);
                        }
                    }
                    break;
                    default: {
                        SpringApplication.exit(this.context, () -> 1);
                    }
                    break;
                }
            });
        } else {
            this.server.addListener(this);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (this.lastFFmpegWrapper != null) {
                    final FfmpegWrapper lastWrapper = this.lastFFmpegWrapper.get();
                    if (lastWrapper != null) {
                        try {
                            lastWrapper.stopStartedExternalProcess();
                        } catch (Throwable ex) {
                            // ignoring
                        }
                    }
                }
            }));
        }
    }

    private JPanel makeMainPanel() {
        final Image logo = Utils.loadImage("back-logo.png");
        return new JPanel() {
            @Override
            protected void paintChildren(final Graphics g) {
                final Rectangle rect = this.getBounds();
                final int imageWidth = logo.getWidth(null);
                final int imageHeight = logo.getHeight(null);
                if (rect.width >= imageWidth && rect.height >= imageHeight) {
                    g.drawImage(logo, (rect.width - imageWidth) / 2, (rect.height - imageHeight) / 2, null);
                }
                super.paintChildren(g);
            }
        };
    }

    private void stopScreenCast() {
        Utils.closeQuietly(this.currentScreenGrabber.getAndSet(null));
        SwingUtilities.invokeLater(() -> {
            if (this.toggleButtonScreencast.isSelected()) {
                this.toggleButtonScreencast.setEnabled(true);
                this.toggleButtonScreencast.setSelected(false);
                this.toggleButtonScreencast.revalidate();
                this.repaint();
            }
            updateToolButtons();
        });
    }

    @Override
    public void onScreencastStarted(final InternalServer source) {
    }

    @Override
    public void onScreencastEnded(final InternalServer source) {
        if (this.timeWhenEndScreencastFlowEnable.get() < System.currentTimeMillis()) {
            this.stopScreenCast();
        }
    }

    public MainFrame() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            initComponents();
            this.setTitle("Ravikoodi content server");

            final Image icon = Utils.loadImage("ravikoodi-logo-256.png");
            this.setIconImage(icon);
            this.setIconImages(List.of(icon));
            if (Taskbar.isTaskbarSupported()) {
                try {
                    Taskbar.getTaskbar().setIconImage(icon);
                } catch (final Exception ex) {
                    LOGGER.error("Can't set taskbar icon for error", ex);
                }
            }

            this.treeVideoFiles.setCellRenderer(new FileTreeRenderer());

            Toolkit.getDefaultToolkit().getSystemClipboard().addFlavorListener(this);
            this.flavorsChanged(new FlavorEvent(Toolkit.getDefaultToolkit().getSystemClipboard()));
            this.treeVideoFiles.getSelectionModel().setSelectionMode(SINGLE_TREE_SELECTION);
            this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

            this.treeVideoFiles.getSelectionModel().addTreeSelectionListener((@NonNull final TreeSelectionEvent e) -> {
                this.updateToolButtons();
            });

            this.treeVideoFiles.setModel(this);
            this.addWindowListener(new WindowAdapter() {
                @Override
                public void windowActivated(WindowEvent e) {
                    panelMain.invalidate();
                    panelMain.repaint();
                }

                @Override
                public void windowDeiconified(WindowEvent e) {
                    panelMain.invalidate();
                    panelMain.repaint();
                }

                @Override
                public void windowClosing(WindowEvent e) {
                    LOGGER.info("Closing main window");
                    if (!fileRegstry.hasActiveUploads() || JOptionPane.showConfirmDialog(MainFrame.this, "There are uploading files, do you rally want close application?", "Confirmation", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                        LOGGER.info("Activate exit");
                        stopScreenCast();
                        SwingUtilities.invokeLater(()
                                -> SpringApplication.exit(context, () -> 0));
                    }
                }
            });

            final DropTarget dropTarget = new DropTarget(this.panelMain, new DropTargetAdapter() {
                @Override
                public void drop(final DropTargetDropEvent dtde) {
                    onMainPanelDropEvent(dtde);
                }
            });
            this.panelMain.setDropTarget(dropTarget);
        });
    }

    private void onMainPanelDropEvent(@NonNull final DropTargetDropEvent event) {
        final Transferable transferable = event.getTransferable();

        try {
            if (transferable.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
                event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                final List<File> files = (List<File>) transferable.getTransferData(DataFlavor.javaFileListFlavor);
                if (files.isEmpty()) {
                    LOGGER.warn("File list is empty, check for string");
                    if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                        final String stringValue = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                        LOGGER.info("Extracted string from transferable: {}", stringValue);
                        final Pattern filePattern = Pattern.compile("\\s*([\\S]+)\\s*.*");
                        final Matcher matcher = filePattern.matcher(stringValue);
                        if (matcher.find()) {
                            String filePath = matcher.group(1);
                            if (filePath.toLowerCase(Locale.ENGLISH).startsWith("smb://") && filePath.contains("%")) {
                                LOGGER.info("Decoding SAMBA path: {}", filePath);
                                filePath = URLDecoder.decode(filePath, StandardCharsets.UTF_8);
                            }
                            LOGGER.info("Opening file path as URI: {}", filePath);
                            this.openUrlLink(filePath);
                        } else {
                            LOGGER.error("Can't extract file path from: {}", stringValue);
                        }
                    } else {
                        LOGGER.warn("String non among transferable flawors: {}", (Object) transferable.getTransferDataFlavors());
                    }
                } else {
                    LOGGER.info("Detected drop action for file list: {}", files);
                    final File fileToOpen = files.get(0);
                    if (fileToOpen.isFile()) {
                        final URI uri = extractUrlLinkFromFile(fileToOpen);
                        if (uri == null) {
                            LOGGER.info("Opening file: {}", fileToOpen);
                            startPlaying(new ContentFile(fileToOpen.toPath(), ContentType.findType(fileToOpen)), null);
                        } else {
                            final String link = uri.toASCIIString();
                            final String lowerCased = link.toLowerCase(Locale.ENGLISH);
                            LOGGER.info("Detected URI to be opened: {}", link);
                            if (lowerCased.contains("youtu.be") || lowerCased.contains("youtube")) {
                                LOGGER.info("Dropped URI recognized as Youtube one: {}", link);
                                this.openYoutubeLink(link);
                            } else {
                                LOGGER.info("Dropped URI recognized as regular one: {}", link);
                                this.openUrlLink(uri.toASCIIString());
                            }
                        }
                    }
                }
            } else if (transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                event.acceptDrop(DnDConstants.ACTION_COPY_OR_MOVE);
                final String droppedLink = ((String) transferable.getTransferData(DataFlavor.stringFlavor)).trim();

                LOGGER.info("Detected dropped string: {}", droppedLink);

                final String lowerCased = droppedLink.toLowerCase(Locale.ENGLISH);
                if (lowerCased.contains("youtu.be") || lowerCased.contains("youtube")) {
                    LOGGER.info("Dropped string recognized as Youtube one: {}", droppedLink);
                    this.openYoutubeLink(droppedLink);
                } else {
                    LOGGER.info("Dropped string recognized as just URL: {}", droppedLink);
                    if (droppedLink.contains("://")) {
                        this.openUrlLink(droppedLink);
                    } else {
                        this.openUrlLink("https://" + droppedLink);
                    }
                }
            } else {
                LOGGER.info("Unsupported transferable object: {}", (Object) transferable.getTransferDataFlavors());
                event.rejectDrop();
            }
        } catch (final Exception ex) {
            LOGGER.error("Error during process transferable object", ex);
        }
    }

    private void fillLookAndFeel() {
        final LookAndFeel current = UIManager.getLookAndFeel();
        final ButtonGroup lfGroup = new ButtonGroup();
        final String currentLFClassName = current.getClass().getName();
        for (final UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
            final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(info.getName());
            lfGroup.add(menuItem);
            if (currentLFClassName.equals(info.getClassName())) {
                menuItem.setSelected(true);
            }
            menuItem.addActionListener((final ActionEvent e) -> {
                try {
                    UIManager.setLookAndFeel(info.getClassName());
                    preferences.setLookAndFeelClassName(info.getClassName());
                    SwingUtilities.updateComponentTreeUI(MainFrame.this);
                    this.treeVideoFiles.repaint();
                } catch (Exception ex) {
                    LOGGER.error("Can't change LF", ex); //NOI18N
                }
            });
            this.menuLookAndFeel.add(menuItem);
        }
    }

    @PreDestroy
    public void preDestroy() {
        SwingUtilities.invokeLater(() -> this.dispose());
    }

    @PostConstruct
    public void postConstruct() {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(preferences.getLookAndFeelClassName());
            } catch (Exception ex) {
                LOGGER.warn("Can't set L&F", ex);
            }

            fillLookAndFeel();

            this.setSize(640, 500);

            SwingUtilities.updateComponentTreeUI(this);
            this.setExtendedState(this.getExtendedState() | MAXIMIZED_BOTH);

            this.setVisible(true);
            this.executorService.scheduleAtFixedRate(this::updatePlayers, 1000L, 1500L, TimeUnit.MILLISECONDS);
            this.setFileRoot(this.preferences.getFileRoot());

            this.applicationStatusPanel.set(new ApplicationStatusPanel(this, executorService, preferences));
            this.panelPlayers.add(this.applicationStatusPanel.get());
        });
        this.timerScheduler.reloadTimers();
    }

    private boolean doesContainPlayer(final ActivePlayerInfo player) {
        boolean result = false;

        for (final Component c : this.panelPlayers.getComponents()) {
            if (c instanceof PlayerPanel) {
                final PlayerPanel panel = (PlayerPanel) c;
                if (panel.getPlayerId() == player.getPlayerid()) {
                    result = true;
                    break;
                }
            }
        }

        return result;
    }

    private boolean removeNonListedPlayers(@NonNull final List<ActivePlayerInfo> players) {
        final Set<Long> playerId = players.stream().map(x -> x.getPlayerid()).collect(Collectors.toSet());

        boolean result = false;

        for (final Component c : this.panelPlayers.getComponents()) {
            if (c instanceof PlayerPanel) {
                final PlayerPanel panel = (PlayerPanel) c;
                if (!playerId.contains(panel.getPlayerId())) {
                    LOGGER.info("Removed player panel '{}'", panel);
                    this.panelPlayers.remove(c);
                    panel.dispose();
                    result = true;
                }
            }
        }
        return result;
    }

    private void updatePlayers() {
        try {
            final List<ActivePlayerInfo> players = this.kodiComm.findActivePlayers();

            SwingUtilities.invokeLater(() -> {
                boolean changed = removeNonListedPlayers(players);

                for (final ActivePlayerInfo p : players) {
                    if (!doesContainPlayer(p)) {
                        final PlayerPanel panel = new PlayerPanel(MainFrame.this, p, executorService, this.kodiComm);
                        panelPlayers.add(panel);
                        LOGGER.info("Added player panel '{}'", panel);
                        changed = true;
                    }
                }

                if (changed) {
                    panelPlayers.revalidate();
                    panelPlayers.repaint();
                }

                int pausedCounter = 0;

                for (final Component c : panelPlayers.getComponents()) {
                    if (c instanceof PlayerPanel) {
                        final PlayerPanel player = (PlayerPanel) c;
                        player.refresh();
                        if (player.isPaused()) {
                            pausedCounter++;
                        }
                    }
                }

                if (pausedCounter == 0) {
                    this.fileRegstry.resume();
                } else {
                    this.fileRegstry.pause();
                }
            });

        } catch (Throwable ex) {
            LOGGER.warn("Error during player update : {}", ex.getMessage());
        }
    }

    private void setFileRoot(@NonNull final String fileRootFolder) {
        this.videoFiles.clear();

        LOGGER.info("Selecting file root '{}'", fileRootFolder);

        try {
            if (!isBlank(fileRootFolder)) {
                final File theFilePath = new File(fileRootFolder);

                if (!theFilePath.isDirectory()) {
                    JOptionPane.showMessageDialog(this, "Can't find folder '" + fileRootFolder + "\'", "Can't fild folder", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                this.currentRootFolder = theFilePath.toPath();

                Files.list(this.currentRootFolder).filter(f -> {
                    return Files.isReadable(f)
                            && (Files.isDirectory(f) || ContentType.findType(f) != ContentType.UNKNOWN);
                }).forEach(f -> {
                    if (Files.isDirectory(f)) {
                        try {
                            this.videoFiles.add(new ContentFolder(f));
                        } catch (IOException ex) {
                            LOGGER.error("Can't read folder {}", f, ex);
                        }
                    } else {
                        this.videoFiles.add(new ContentFile(f, ContentType.findType(f)));
                    }
                });

                Collections.sort(this.videoFiles, CONTENT_ITEM_COMPARATOR);
            }
        } catch (IOException ex) {
            LOGGER.error("Can't set file root", ex);
        } finally {
            this.treeListeners.forEach((l) -> {
                l.treeStructureChanged(new TreeModelEvent(this, new TreePath(new Object[]{this})));
            });
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

        mainPanelSplit = new javax.swing.JSplitPane();
        panelFileTree = new javax.swing.JPanel();
        fileTreeScrollPane = new javax.swing.JScrollPane();
        treeVideoFiles = new javax.swing.JTree();
        jToolBar1 = new javax.swing.JToolBar();
        buttonSelectFolder = new javax.swing.JButton();
        buttonRefreshFolder = new javax.swing.JButton();
        buttonOpenSelectedFileInSystem = new javax.swing.JButton();
        buttonImageFromClipboard = new javax.swing.JButton();
        buttonPlaySelected = new javax.swing.JButton();
        toggleButtonScreencast = new javax.swing.JToggleButton();
        panelMain = this.makeMainPanel();
        panelPlayers = new javax.swing.JPanel();
        menuMain = new javax.swing.JMenuBar();
        menuFile = new javax.swing.JMenu();
        menuSelectFolder = new javax.swing.JMenuItem();
        menuFileOpenURL = new javax.swing.JMenuItem();
        menuOpenFile = new javax.swing.JMenuItem();
        menuOpenYoutubeLink = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        menuExit = new javax.swing.JMenuItem();
        menuTools = new javax.swing.JMenu();
        menuTimers = new javax.swing.JMenuItem();
        menuStaticContent = new javax.swing.JMenuItem();
        menuLookAndFeel = new javax.swing.JMenu();
        menuToolsOptions = new javax.swing.JMenuItem();
        menuMisc = new javax.swing.JMenu();
        menuMiscDecodeYoutubeUrl = new javax.swing.JMenuItem();
        menuHelp = new javax.swing.JMenu();
        menuHelpDonation = new javax.swing.JMenuItem();
        menuAbout = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        mainPanelSplit.setDividerLocation(255);

        panelFileTree.setLayout(new java.awt.BorderLayout());

        treeVideoFiles.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                treeVideoFilesMouseClicked(evt);
            }
        });
        fileTreeScrollPane.setViewportView(treeVideoFiles);

        panelFileTree.add(fileTreeScrollPane, java.awt.BorderLayout.CENTER);

        jToolBar1.setFloatable(false);
        jToolBar1.setRollover(true);

        buttonSelectFolder.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/folders_explorer.png"))); // NOI18N
        buttonSelectFolder.setToolTipText("Select root content folder");
        buttonSelectFolder.setFocusable(false);
        buttonSelectFolder.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonSelectFolder.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonSelectFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonSelectFolderActionPerformed(evt);
            }
        });
        jToolBar1.add(buttonSelectFolder);

        buttonRefreshFolder.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrow_refresh.png"))); // NOI18N
        buttonRefreshFolder.setToolTipText("Refresh folder");
        buttonRefreshFolder.setFocusable(false);
        buttonRefreshFolder.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonRefreshFolder.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonRefreshFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonRefreshFolderActionPerformed(evt);
            }
        });
        jToolBar1.add(buttonRefreshFolder);

        buttonOpenSelectedFileInSystem.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/32_vlc.png"))); // NOI18N
        buttonOpenSelectedFileInSystem.setToolTipText("Open selected file in system");
        buttonOpenSelectedFileInSystem.setEnabled(false);
        buttonOpenSelectedFileInSystem.setFocusable(false);
        buttonOpenSelectedFileInSystem.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonOpenSelectedFileInSystem.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonOpenSelectedFileInSystem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonOpenSelectedFileInSystemActionPerformed(evt);
            }
        });
        jToolBar1.add(buttonOpenSelectedFileInSystem);

        buttonImageFromClipboard.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/picture_insert.png"))); // NOI18N
        buttonImageFromClipboard.setToolTipText("Send clipboard image to KODI");
        buttonImageFromClipboard.setEnabled(false);
        buttonImageFromClipboard.setFocusable(false);
        buttonImageFromClipboard.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonImageFromClipboard.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonImageFromClipboard.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonImageFromClipboardActionPerformed(evt);
            }
        });
        jToolBar1.add(buttonImageFromClipboard);

        buttonPlaySelected.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/control_play_blue.png"))); // NOI18N
        buttonPlaySelected.setToolTipText("Play selected item on KODI");
        buttonPlaySelected.setEnabled(false);
        buttonPlaySelected.setFocusable(false);
        buttonPlaySelected.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        buttonPlaySelected.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        buttonPlaySelected.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonPlaySelectedActionPerformed(evt);
            }
        });
        jToolBar1.add(buttonPlaySelected);

        toggleButtonScreencast.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/videodisplay.png"))); // NOI18N
        toggleButtonScreencast.setToolTipText("Stream screen to KODI");
        toggleButtonScreencast.setFocusable(false);
        toggleButtonScreencast.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        toggleButtonScreencast.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        toggleButtonScreencast.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                toggleButtonScreencastActionPerformed(evt);
            }
        });
        jToolBar1.add(toggleButtonScreencast);

        panelFileTree.add(jToolBar1, java.awt.BorderLayout.NORTH);

        mainPanelSplit.setLeftComponent(panelFileTree);

        panelMain.setLayout(new java.awt.BorderLayout());

        panelPlayers.setLayout(new javax.swing.BoxLayout(panelPlayers, javax.swing.BoxLayout.Y_AXIS));
        panelMain.add(panelPlayers, java.awt.BorderLayout.PAGE_START);

        mainPanelSplit.setRightComponent(panelMain);

        getContentPane().add(mainPanelSplit, java.awt.BorderLayout.CENTER);

        menuFile.setText("File");

        menuSelectFolder.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_folders_explorer.png"))); // NOI18N
        menuSelectFolder.setText("Select folder");
        menuSelectFolder.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuSelectFolderActionPerformed(evt);
            }
        });
        menuFile.add(menuSelectFolder);

        menuFileOpenURL.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_world_link.png"))); // NOI18N
        menuFileOpenURL.setText("Open URL");
        menuFileOpenURL.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuFileOpenURLActionPerformed(evt);
            }
        });
        menuFile.add(menuFileOpenURL);

        menuOpenFile.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_file_extension_cda.png"))); // NOI18N
        menuOpenFile.setText("Open file");
        menuOpenFile.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOpenFileActionPerformed(evt);
            }
        });
        menuFile.add(menuOpenFile);

        menuOpenYoutubeLink.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_youtube.png"))); // NOI18N
        menuOpenYoutubeLink.setText("Open Youtube");
        menuOpenYoutubeLink.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuOpenYoutubeLinkActionPerformed(evt);
            }
        });
        menuFile.add(menuOpenYoutubeLink);
        menuFile.add(jSeparator1);

        menuExit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_door_in.png"))); // NOI18N
        menuExit.setText("Exit");
        menuExit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuExitActionPerformed(evt);
            }
        });
        menuFile.add(menuExit);

        menuMain.add(menuFile);

        menuTools.setText("Tools");

        menuTimers.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_time.png"))); // NOI18N
        menuTimers.setText("Timers");
        menuTimers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuTimersActionPerformed(evt);
            }
        });
        menuTools.add(menuTimers);

        menuStaticContent.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_file_publish_sharepoint.png"))); // NOI18N
        menuStaticContent.setText("Published files");
        menuStaticContent.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuStaticContentActionPerformed(evt);
            }
        });
        menuTools.add(menuStaticContent);

        menuLookAndFeel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_eye.png"))); // NOI18N
        menuLookAndFeel.setText("Look and Feel");
        menuTools.add(menuLookAndFeel);

        menuToolsOptions.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_setting_tools.png"))); // NOI18N
        menuToolsOptions.setText("Options");
        menuToolsOptions.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuToolsOptionsActionPerformed(evt);
            }
        });
        menuTools.add(menuToolsOptions);

        menuMain.add(menuTools);

        menuMisc.setText("Misc");

        menuMiscDecodeYoutubeUrl.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_youtube.png"))); // NOI18N
        menuMiscDecodeYoutubeUrl.setText("Decode Youtube URL");
        menuMiscDecodeYoutubeUrl.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuMiscDecodeYoutubeUrlActionPerformed(evt);
            }
        });
        menuMisc.add(menuMiscDecodeYoutubeUrl);

        menuMain.add(menuMisc);

        menuHelp.setText("Help");

        menuHelpDonation.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_coins.png"))); // NOI18N
        menuHelpDonation.setText("Make donation");
        menuHelpDonation.setToolTipText("Make donation to the author of the application");
        menuHelpDonation.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuHelpDonationActionPerformed(evt);
            }
        });
        menuHelp.add(menuHelpDonation);

        menuAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/16_information.png"))); // NOI18N
        menuAbout.setText("About");
        menuAbout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                menuAboutActionPerformed(evt);
            }
        });
        menuHelp.add(menuAbout);

        menuMain.add(menuHelp);

        setJMenuBar(menuMain);

        pack();
    }// </editor-fold>//GEN-END:initComponents

  private void buttonSelectFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonSelectFolderActionPerformed
      final JFileChooser chooser = new JFileChooser();
      chooser.setCurrentDirectory(new File(System.getProperty("user.home")));
      chooser.setDialogTitle("Select video folder");
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      chooser.setAcceptAllFileFilterUsed(false);
      if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
          final File folder = chooser.getSelectedFile();
          setFileRoot(folder.getAbsolutePath());
          this.preferences.setFileRoot(folder.getAbsolutePath());
          this.preferences.flush();
      }
  }//GEN-LAST:event_buttonSelectFolderActionPerformed

  private void menuToolsOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuToolsOptionsActionPerformed
      final OptionsPanel.Data container = new OptionsPanel.Data(this.preferences);
      if (JOptionPane.showConfirmDialog(this, Utils.makeOwningDialogResizable(new JScrollPane(new OptionsPanel(container, this.soundAdapter))), "Options", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
          container.save(this.preferences);
          this.server.restartServer();
      }
  }//GEN-LAST:event_menuToolsOptionsActionPerformed

  private void treeVideoFilesMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_treeVideoFilesMouseClicked
      if (!evt.isPopupTrigger() && evt.getClickCount() > 1) {
          final TreePath path = this.treeVideoFiles.getSelectionPath();
          if (path != null) {
              final Object last = path.getLastPathComponent();
              if (last instanceof ContentFile) {
                  openInSystem((ContentFile) last);
              }
          }
      }
  }//GEN-LAST:event_treeVideoFilesMouseClicked

  private void buttonPlaySelectedActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonPlaySelectedActionPerformed
      final TreePath selected = this.treeVideoFiles.getSelectionPath();
      if (selected != null && selected.getLastPathComponent() instanceof ContentFile) {
          final ContentFile videoFile = (ContentFile) selected.getLastPathComponent();
          if (this.fileRegstry.isFileAtPlay(videoFile.getFilePath())) {
              JOptionPane.showMessageDialog(this, "The File is already playing", "File is playing", JOptionPane.WARNING_MESSAGE);
          } else {
              this.startPlaying(videoFile, null);
          }
      }
  }//GEN-LAST:event_buttonPlaySelectedActionPerformed

  private void buttonOpenSelectedFileInSystemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOpenSelectedFileInSystemActionPerformed
      final TreePath path = this.treeVideoFiles.getSelectionPath();
      if (path.getLastPathComponent() instanceof ContentFile) {
          openInSystem((ContentFile) path.getLastPathComponent());
      }
  }//GEN-LAST:event_buttonOpenSelectedFileInSystemActionPerformed

  private void menuExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuExitActionPerformed
      this.dispatchEvent(new WindowEvent(this, WindowEvent.WINDOW_CLOSING));
  }//GEN-LAST:event_menuExitActionPerformed

  private void menuSelectFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSelectFolderActionPerformed
      this.buttonSelectFolder.doClick();
  }//GEN-LAST:event_menuSelectFolderActionPerformed

  private void buttonImageFromClipboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonImageFromClipboardActionPerformed
      try {
          final Image image = (Image) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.imageFlavor);
          if (image == null) {
              LOGGER.warn("NULL image from clipboard");
              JOptionPane.showMessageDialog(this, "Can't find image in clipboard!", "No clipboard image", JOptionPane.ERROR_MESSAGE);
              return;
          }

          final RenderedImage rendered;
          if (image instanceof RenderedImage) {
              rendered = (RenderedImage) image;
          } else {
              final BufferedImage bufferimage = new BufferedImage(image.getWidth(null), image.getHeight(null), BufferedImage.TYPE_INT_RGB);
              final Graphics g = bufferimage.createGraphics();
              g.drawImage(image, 0, 0, null);
              g.dispose();
              rendered = bufferimage;
          }
          LOGGER.info("Prepared clipboard image {}x{} to be sent", rendered.getWidth(), rendered.getHeight());
          final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
          ImageIO.write(rendered, "png", buffer);
          this.startPlaying(new ContentFile(Paths.get("clipboard_screenshot.png"), ContentType.PICTURE), buffer.toByteArray());
      } catch (Exception ex) {
          LOGGER.error("Can't send image from clipboard for error", ex);
          JOptionPane.showMessageDialog(this, "Error during operation : " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
  }//GEN-LAST:event_buttonImageFromClipboardActionPerformed

  private void menuOpenFileActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOpenFileActionPerformed
      final File folder;

      if (this.lastSelectedFileFolder == null) {
          folder = this.currentRootFolder == null ? null : this.currentRootFolder.toFile();
      } else {
          folder = this.lastSelectedFileFolder;
      }

      final JFileChooser fileChooser = new JFileChooser(folder);
      fileChooser.setDialogTitle("Open file on KODI");
      fileChooser.setMultiSelectionEnabled(false);
      fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      fileChooser.setAcceptAllFileFilterUsed(false);

      for (final MimeTypes.ContentType c : MimeTypes.ContentType.values()) {
          if (c != ContentType.UNKNOWN) {
              fileChooser.addChoosableFileFilter(this.mimeTypes.makeFileFilter(c));
          }
      }

      if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
          final File fileToOpen = fileChooser.getSelectedFile();
          this.lastSelectedFileFolder = fileToOpen.getParentFile();
          if (fileToOpen.isFile()) {
              LOGGER.info("Opening file {}", fileToOpen);
              startPlaying(new ContentFile(fileToOpen.toPath(), ContentType.findType(fileToOpen)), null);
          } else {
              LOGGER.warn("Can't find file {}", fileToOpen);
          }
      }
  }//GEN-LAST:event_menuOpenFileActionPerformed

  private void menuAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuAboutActionPerformed
      JOptionPane.showMessageDialog(this, new AboutPanel(this.donationController, this.buildProperties), "About", JOptionPane.PLAIN_MESSAGE);
  }//GEN-LAST:event_menuAboutActionPerformed

    private final void openUrlLink(final String url) {
        OpeningFileInfoPanel infoPanel = this.openingFileInfoPanel.get();
        if (infoPanel == null) {
            infoPanel = new OpeningFileInfoPanel();
            this.openingFileInfoPanel.set(infoPanel);
            this.setGlassPane(infoPanel);
        }
        infoPanel.setTextInfo(String.format("Opening link '%s'", Utils.cutStrLength(url, STR_CUT_LEN)));
        infoPanel.setVisible(true);

        this.executorService.submit(() -> {
            final KodiAddress kodiAddress;
            kodiAddress = new KodiAddress(
                    this.preferences.getKodiAddress(),
                    this.preferences.getKodiPort(),
                    this.preferences.getKodiName(),
                    this.preferences.getKodiPassword(),
                    this.preferences.isKodiSsl()
            );

            final AtomicReference<Throwable> error = new AtomicReference<>();
            try {
                final String result = new KodiService(kodiAddress, this.preferences.getJsonRequestTimeout()).doPlayerOpenFile(url);
                LOGGER.info("Player open link response is '{}' for '{}'", result, url);
                if (!"ok".equalsIgnoreCase(result)) {
                    throw new IllegalStateException("Can't start play link, status : " + result);
                }
                notifyAllPlayersToRefreshFullData();
            } catch (Throwable ex) {
                error.set(ex);
                LOGGER.error("Can't open link {}", url, ex);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    openingFileInfoPanel.get().setVisible(false);
                    if (error.get() != null) {
                        JOptionPane.showMessageDialog(MainFrame.this, "Can't open link by KODI, '" + Utils.cutStrLength(url, 64) + "', error: " + Utils.cutStrLength(error.get().getMessage(), 64), "Can't open URL", JOptionPane.ERROR_MESSAGE);
                    }
                });
            }
        });
    }

  private void menuFileOpenURLActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileOpenURLActionPerformed
      final Icon icon = new ImageIcon(Utils.loadImage("32_url_link.png"));
      final Object text = JOptionPane.showInputDialog(this, "Entered URL will be opened with KODI player", "Open URL", JOptionPane.PLAIN_MESSAGE, icon, null, null);
      if (text == null) {
          return;
      }

      final URI uri;
      try {
          uri = URI.create(text.toString().trim());
          if (!uri.isAbsolute()) {
              throw new IllegalArgumentException("Entered non-absolute URI: " + uri);
          }
      } catch (Exception ex) {
          LOGGER.error("Error URL {}", text, ex);
          JOptionPane.showMessageDialog(this, "Unsupported or wrong formatted URL: " + Utils.cutStrLength(text.toString(), STR_CUT_LEN), "Can't open URL", JOptionPane.ERROR_MESSAGE);
          return;
      }

      this.openUrlLink(uri.toASCIIString());
  }//GEN-LAST:event_menuFileOpenURLActionPerformed

  private void buttonRefreshFolderActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonRefreshFolderActionPerformed
      this.setFileRoot(this.currentRootFolder.toString());
  }//GEN-LAST:event_buttonRefreshFolderActionPerformed

  private void toggleButtonScreencastActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_toggleButtonScreencastActionPerformed
      if (this.toggleButtonScreencast.isSelected()) {
          this.toggleButtonScreencast.setEnabled(false);
          if (JOptionPane.showConfirmDialog(this, "Do you want begin screencast?", "Confirmation", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
              this.toggleButtonScreencast.setEnabled(true);
              this.toggleButtonScreencast.setSelected(false);
              updateToolButtons();
              return;
          }

          try {
              final ScreenGrabber newScreenGrabber = new ScreenGrabber(this.preferences);
              this.timeWhenEndScreencastFlowEnable.set(System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(30));

              final FfmpegWrapper ffmpwrapper = new FfmpegWrapper(
                      this.preferences,
                      this.soundAdapter,
                      this.server.getScreencastDataBuffer()
              );

              this.lastFFmpegWrapper = new WeakReference<>(ffmpwrapper);

              newScreenGrabber.addGrabbingListener(ffmpwrapper);

              newScreenGrabber.addGrabbingListener(new ScreenGrabber.ScreenGrabberListener() {
                  private void endWork() {
                      stopScreenCast();
                  }

                  @Override
                  public void onStarted(final ScreenGrabber source) {
                      sendScreencastAddressToKodi();
                  }

                  @Override
                  public void onDisposed(final ScreenGrabber source) {
                      endWork();
                  }

                  @Override
                  public void onError(final ScreenGrabber source, final Throwable error) {
                      timeWhenEndScreencastFlowEnable.set(0L);
                      final Runnable code = () -> {
                          JOptionPane.showMessageDialog(MainFrame.this, "Screencast error: " + Utils.cutStrLength(error.getMessage(), STR_CUT_LEN), "Error", JOptionPane.ERROR_MESSAGE);
                      };
                      if (SwingUtilities.isEventDispatchThread()) {
                          code.run();
                      } else {
                          SwingUtilities.invokeLater(code);
                      }
                      endWork();
                  }
              });

              if (this.currentScreenGrabber.compareAndSet(null, newScreenGrabber)) {
                  newScreenGrabber.start();

                  this.executorService.schedule(() -> {
                      if (this.server.isScreencastFlowActive()) {
                          LOGGER.info("Detected started screencast flow");
                      } else {
                          LOGGER.warn("Detected that there is no screencast flow, stopping grabber");
                          if (this.currentScreenGrabber.get() != null) {
                              this.stopScreenCast();
                          }
                      }
                  }, 30, TimeUnit.SECONDS);
              } else {
                  LOGGER.warn("Detected already active screen grabber!");
              }
          } catch (Exception ex) {
              LOGGER.error("Can't create screen grabber");
              JOptionPane.showMessageDialog(this, "Can't create screen grabber: " + Utils.cutStrLength(ex.getMessage(), STR_CUT_LEN), "Error", JOptionPane.ERROR_MESSAGE);
              stopScreenCast();
          }
      }
      this.updateToolButtons();
  }//GEN-LAST:event_toggleButtonScreencastActionPerformed

  private void menuHelpDonationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuHelpDonationActionPerformed
      this.donationController.openDonationUrl();
  }//GEN-LAST:event_menuHelpDonationActionPerformed

    private void menuTimersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTimersActionPerformed
        final String filePath = this.preferences.getFileRoot();
        final File root = filePath == null ? null : new File(filePath);
        final TimersTable timersTable = new TimersTable(root, this.preferences.getTimers());
        if (JOptionPane.showConfirmDialog(this, timersTable, "Timers", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            final List<TimerResource> newTimers = timersTable.getTimers();
            this.preferences.setTimers(newTimers);
            this.timerScheduler.reloadTimers();
        }
    }//GEN-LAST:event_menuTimersActionPerformed

    private void openYoutubeVideoLinkWithKodiPlugin(final String youTubeVideoId) {
        final String patternVideo = "plugin://plugin.video.youtube/play/?video_id=%s";
        LOGGER.info("Opening Youtube video for id: {}", youTubeVideoId);
        this.openUrlLink(String.format(patternVideo, youTubeVideoId.trim()));
    }

    private void openYoutubePlaylistLinkWithKodiPlugin(final String youTubeVideoId) {
        final String patternPlayList = "plugin://plugin.video.youtube/play/?playlist_id=%s";
        LOGGER.info("Opening Youtube playlist for id: {}", youTubeVideoId);
        this.openUrlLink(String.format(patternPlayList, youTubeVideoId));
    }

    private void openYoutubeThroughDirectLinkSearch(
            @NonNull final String youTubeVideoId,
            @NonNull final YtQuality preferredQuality,
            @NonNull final YtVideoType requiredFormat,
            final boolean throughPlayList) {
        LOGGER.info("Opening youtube (playlist flag is {}) through direct link search, preffered quality is {}, required format is {}: {}", throughPlayList, preferredQuality, requiredFormat, youTubeVideoId);
        if (throughPlayList) {
            this.youTubeLinkExtractor.findPlayListInfoAsync(youTubeVideoId, (playListId, playlistInfo, error) -> {
                if (error == null) {
                    if (playlistInfo.videos().isEmpty()) {
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(MainFrame.this, "Youtube playlist is empty!", "Info '" + playListId + '\'', JOptionPane.WARNING_MESSAGE);
                        });
                    } else {
                        var firstVideo = playlistInfo.videos().stream()
                                .filter(x -> x.isPlayable())
                                .findFirst()
                                .orElse(null);
                        if (firstVideo == null) {
                            LOGGER.info("Can't find any playable video in youtube playlist: {}", playListId);
                            SwingUtilities.invokeLater(() -> {
                                JOptionPane.showMessageDialog(MainFrame.this, "Youtube playlist doesn't have playable video!", "Info '" + playListId + '\'', JOptionPane.WARNING_MESSAGE);
                            });
                        } else {
                            SwingUtilities.invokeLater(() -> {
                                LOGGER.info("Opening first video in youtube playlist '{}': {} ({})", playListId, firstVideo.title(), firstVideo.videoId());
                                this.youTubeLinkExtractor.findVideoUrlAsync(firstVideo.videoId(), preferredQuality, requiredFormat, this::onResolvedYoutubeUrlLink);
                            });
                        }
                    }
                } else {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(MainFrame.this, error.getMessage(), "Error '" + youTubeLinkExtractor + '\'', JOptionPane.ERROR_MESSAGE);
                    });
                }
            });
        } else {
            this.youTubeLinkExtractor.findVideoUrlAsync(youTubeVideoId, preferredQuality, requiredFormat, this::onResolvedYoutubeUrlLink);
        }
    }

    private void onResolvedYoutubeUrlLink(@NonNull final String youTubeVideoId, @Nullable final String url, @Nullable final Throwable error) {
        if (error == null) {
            SwingUtilities.invokeLater(() -> this.openUrlLink(url));
        } else {
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(MainFrame.this, error.getMessage(), "Error '" + youTubeVideoId + '\'', JOptionPane.ERROR_MESSAGE);
            });
        }
    }

    private void openYoutubeLink(@NonNull final String youtubeLinkUrl) {
        String id = YoutubeUtils.extractYoutubeVideoId(youtubeLinkUrl).orElse(null);
        switch (this.preferences.getYoutubeOpenUrlMode()) {
            case DIRECT_URL: {
                final YtQuality preferredQuality = this.preferences.getYoutubePreferredQuality();
                final YtVideoType requiredFormat = this.preferences.getYoutubeRequiredFormat();
                if (id == null) {
                    id = YoutubeUtils.extractYoutubePlaylistId(youtubeLinkUrl).orElse(null);
                    if (id == null) {
                        this.openYoutubeThroughDirectLinkSearch(youtubeLinkUrl.trim(),
                                preferredQuality, requiredFormat, false);
                    } else {
                        this.openYoutubeThroughDirectLinkSearch(id,
                                preferredQuality, requiredFormat, true);
                    }
                } else {
                    this.openYoutubeThroughDirectLinkSearch(id,
                            preferredQuality, requiredFormat, false);
                }

            }
            break;
            case KODI_PLUGIN: {
                if (id == null) {
                    id = YoutubeUtils.extractYoutubePlaylistId(youtubeLinkUrl).orElse(null);
                    if (id == null) {
                        openYoutubeVideoLinkWithKodiPlugin(youtubeLinkUrl.trim());
                    } else {
                        openYoutubePlaylistLinkWithKodiPlugin(id);
                    }
                } else {
                    openYoutubeVideoLinkWithKodiPlugin(id);
                }
            }
            break;
            default: {
                throw new IllegalArgumentException("Unexpected Youtube mode");
            }
        }
    }

    private void menuOpenYoutubeLinkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOpenYoutubeLinkActionPerformed
        final Icon icon = new ImageIcon(Utils.loadImage("32_youtube.png"));
        final String text = (String) JOptionPane.showInputDialog(this, "Entered Youtube URL will be opened with KODI player", "Open Youtube link", JOptionPane.PLAIN_MESSAGE, icon, null, null);
        if (text == null) {
            return;
        }
        this.openYoutubeLink(text);
    }//GEN-LAST:event_menuOpenYoutubeLinkActionPerformed

    private void menuStaticContentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuStaticContentActionPerformed
        final String filePath = this.preferences.getFileRoot();
        final File root = filePath == null ? null : new File(filePath);

        final PublishedFilesTable publishedFilesTable = new PublishedFilesTable(this.server.makeUrlPrefix(InternalServer.PATH_RESOURCES), root, this.preferences.getStaticResources());
        if (JOptionPane.showConfirmDialog(this, publishedFilesTable, "Published files", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
            final List<StaticResource> newResources = publishedFilesTable.getResources();
            this.preferences.setStaticResources(newResources);
            this.staticFileRegistry.refresh();
        }
    }//GEN-LAST:event_menuStaticContentActionPerformed

    private void menuMiscDecodeYoutubeUrlActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuMiscDecodeYoutubeUrlActionPerformed
        final String youtubeLinkUrl = JOptionPane.showInputDialog(this, "Enter Youtube URL", "");
        if (youtubeLinkUrl == null || youtubeLinkUrl.isBlank()) {
            return;
        }

        final String youTubeVideoId = YoutubeUtils.extractYoutubeVideoId(youtubeLinkUrl).orElse(null);

        if (youTubeVideoId == null) {
            JOptionPane.showMessageDialog(this, "Can't extract Youtube video ID", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        final RequestVideoInfo request = new RequestVideoInfo(youTubeVideoId)
                .callback(new YoutubeCallback<VideoInfo>() {
                    @Override
                    public void onFinished(final VideoInfo videoInfo) {
                        final List<? extends VideoFormat> formatsWithSound = videoInfo.videoWithAudioFormats();
                        final List<VideoFormat> formatsNoSound = videoInfo.videoFormats();
                        final List<AudioFormat> formatsSound = videoInfo.audioFormats();

                        final StringBuilder buffer = new StringBuilder();
                        buffer.append("<html><body>");

                        buffer.append("<h2>With sound</h2><ul>");

                        formatsWithSound.forEach(x -> {
                            buffer.append(String.format("<li>%s %s %s %s <a href=\"%s\">url</a></li>",
                                    StringEscapeUtils.escapeHtml3(x.type()),
                                    StringEscapeUtils.escapeHtml3(x.mimeType()),
                                    StringEscapeUtils.escapeHtml3(x.qualityLabel()),
                                    StringEscapeUtils.escapeHtml3(x.videoQuality().name()),
                                    StringEscapeUtils.escapeHtml3(x.url())));
                        });

                        buffer.append("</ul><h2>No sound</h2><ul>");

                        formatsNoSound.forEach(x -> {
                            buffer.append(String.format("<li>%s %s %s %s <a href=\"%s\">url</a></li>",
                                    StringEscapeUtils.escapeHtml3(x.type()),
                                    StringEscapeUtils.escapeHtml3(x.mimeType()),
                                    StringEscapeUtils.escapeHtml3(x.qualityLabel()),
                                    StringEscapeUtils.escapeHtml3(x.videoQuality().name()),
                                    StringEscapeUtils.escapeHtml3(x.url())));
                        });

                        buffer.append("</ul><h2>Only sound</h2><ul>");

                        formatsSound.forEach(x -> {
                            buffer.append(String.format("<li>%s %s %s samplerate: %d bitrate:%d <a href=\"%s\">url</a></li>",
                                    StringEscapeUtils.escapeHtml3(x.type()),
                                    StringEscapeUtils.escapeHtml3(x.mimeType()),
                                    StringEscapeUtils.escapeHtml3(x.audioQuality().name()),
                                    x.audioSampleRate(),
                                    x.averageBitrate(),
                                    StringEscapeUtils.escapeHtml3(x.url())));
                        });

                        buffer.append("</ul></body></html>");

                        SwingUtilities.invokeLater(() -> {
                            final JEditorPane ed1tor = new JEditorPane("text/html", buffer.toString());
                            ed1tor.addHyperlinkListener(e -> {
                                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                                    Utils.showURLExternal(e.getURL());
                                }
                            });
                            ed1tor.setEditable(false);
                            JOptionPane.showMessageDialog(MainFrame.this, new JScrollPane(ed1tor), "Found Youtube links", JOptionPane.PLAIN_MESSAGE);
                        });
                    }

                    @Override
                    public void onError(final Throwable throwable) {
                        LOGGER.error("Error during search video formats for youtube '{}'", youTubeVideoId, throwable);
                        SwingUtilities.invokeLater(() -> {
                            JOptionPane.showMessageDialog(MainFrame.this, "Error during search video formats for youtube: " + throwable.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                        });
                    }
                })
                .clientType(ClientType.WEB_PARENT_TOOLS)
                .maxRetries(5)
                .async();
        this.youTubeLinkExtractor.getYoutubeDownloader().getVideoInfo(request);

    }//GEN-LAST:event_menuMiscDecodeYoutubeUrlActionPerformed

    public void startPlaying(@NonNull final ContentFile contentFile, @Nullable final byte[] data) {
        final Runnable run = () -> {
            OpeningFileInfoPanel infoPanel = this.openingFileInfoPanel.get();
            if (infoPanel == null) {
                infoPanel = new OpeningFileInfoPanel();
                this.openingFileInfoPanel.set(infoPanel);
                this.setGlassPane(infoPanel);
            }
            infoPanel.setTextInfo("Opening file '" + contentFile.getFileNameAsString() + "'");
            infoPanel.setVisible(true);

            this.executorService.submit(() -> {
                final String uuid = UUID.randomUUID().toString();
                final UploadFileRecord record = this.fileRegstry.registerFile(uuid, contentFile.getFilePath(), data);
                final AtomicReference<Throwable> error = new AtomicReference<>();
                try {
                    if (!this.kodiComm.openFileThroughRegistry(record.getFile(), data, Collections.singletonMap("repeat", "off")).isPresent()) {
                        throw new IllegalStateException("Can't start play");
                    }
                    notifyAllPlayersToRefreshFullData();
                } catch (Throwable ex) {
                    error.set(ex);
                    LOGGER.error("Can't start file play", ex);
                    this.fileRegstry.unregisterFile(uuid, false);
                } finally {
                    SwingUtilities.invokeLater(() -> {
                        openingFileInfoPanel.get().setVisible(false);
                        if (error.get() != null) {
                            JOptionPane.showMessageDialog(MainFrame.this, "Can't send to KODI, file '" + contentFile.getFileNameAsString() + "', error: " + error.get().getMessage(), "Can't open file", JOptionPane.ERROR_MESSAGE);
                        }
                        this.repaint();
                    });
                }
            });
        };

        if (SwingUtilities.isEventDispatchThread()) {
            run.run();
        } else {
            SwingUtilities.invokeLater(run);
        }
    }

    private void sendScreencastAddressToKodi() {
        OpeningFileInfoPanel infoPanel = this.openingFileInfoPanel.get();
        if (infoPanel == null) {
            infoPanel = new OpeningFileInfoPanel();
            this.openingFileInfoPanel.set(infoPanel);
            this.setGlassPane(infoPanel);
        }
        infoPanel.setTextInfo("Starting screencast");
        infoPanel.setVisible(true);

        this.executorService.submit(() -> {
            final KodiAddress kodiAddress;
            kodiAddress = new KodiAddress(
                    this.preferences.getKodiAddress(),
                    this.preferences.getKodiPort(),
                    this.preferences.getKodiName(),
                    this.preferences.getKodiPassword(),
                    this.preferences.isKodiSsl()
            );

            final AtomicReference<Throwable> error = new AtomicReference<>();
            try {
                final String screenCastUrl = this.server.getScreenCastUrl();
                final String result = new KodiService(kodiAddress, this.preferences.getJsonRequestTimeout()).doPlayerOpenFile(screenCastUrl);
                LOGGER.info("Player open response for '{}' is '{}'", screenCastUrl, result);
                if (!"ok".equalsIgnoreCase(result)) {
                    throw new IllegalStateException("Can't start play, status : " + result);
                }
                notifyAllPlayersToRefreshFullData();
            } catch (Throwable ex) {
                error.set(ex);
                LOGGER.error("Can't start screen casting", ex);
            } finally {
                SwingUtilities.invokeLater(() -> {
                    openingFileInfoPanel.get().setVisible(false);
                    if (error.get() != null) {
                        JOptionPane.showMessageDialog(MainFrame.this, "Can't open screen-cast on KODI, error: " + error.get().getMessage(), "Can't open file", JOptionPane.ERROR_MESSAGE);
                        stopScreenCast();
                    }
                    this.repaint();
                });
            }
        });
    }

    private void notifyAllPlayersToRefreshFullData() {
        final Runnable r = () -> {
            for (final Component c : this.panelPlayers.getComponents()) {
                if (c instanceof PlayerPanel) {
                    ((PlayerPanel) c).notifyFullDataRefresh();
                }
            }
        };
        if (SwingUtilities.isEventDispatchThread()) {
            r.run();
        } else {
            SwingUtilities.invokeLater(r);
        }
    }


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonImageFromClipboard;
    private javax.swing.JButton buttonOpenSelectedFileInSystem;
    private javax.swing.JButton buttonPlaySelected;
    private javax.swing.JButton buttonRefreshFolder;
    private javax.swing.JButton buttonSelectFolder;
    private javax.swing.JScrollPane fileTreeScrollPane;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JToolBar jToolBar1;
    private javax.swing.JSplitPane mainPanelSplit;
    private javax.swing.JMenuItem menuAbout;
    private javax.swing.JMenuItem menuExit;
    private javax.swing.JMenu menuFile;
    private javax.swing.JMenuItem menuFileOpenURL;
    private javax.swing.JMenu menuHelp;
    private javax.swing.JMenuItem menuHelpDonation;
    private javax.swing.JMenu menuLookAndFeel;
    private javax.swing.JMenuBar menuMain;
    private javax.swing.JMenu menuMisc;
    private javax.swing.JMenuItem menuMiscDecodeYoutubeUrl;
    private javax.swing.JMenuItem menuOpenFile;
    private javax.swing.JMenuItem menuOpenYoutubeLink;
    private javax.swing.JMenuItem menuSelectFolder;
    private javax.swing.JMenuItem menuStaticContent;
    private javax.swing.JMenuItem menuTimers;
    private javax.swing.JMenu menuTools;
    private javax.swing.JMenuItem menuToolsOptions;
    private javax.swing.JPanel panelFileTree;
    private javax.swing.JPanel panelMain;
    private javax.swing.JPanel panelPlayers;
    private javax.swing.JToggleButton toggleButtonScreencast;
    private javax.swing.JTree treeVideoFiles;
    // End of variables declaration//GEN-END:variables

    @Override
    @NonNull
    public Object getRoot() {
        return this;
    }

    @Override
    @NonNull
    public Object getChild(@NonNull final Object parent, final int index) {
        if (parent == this) {
            return this.videoFiles.get(index);
        } else {
            return parent instanceof ContentFolder ? ((ContentFolder) parent).getFiles().get(index) : 0;
        }
    }

    @Override
    public int getChildCount(@NonNull final Object parent) {
        if (parent == this) {
            return this.videoFiles.size();
        } else {
            return parent instanceof ContentFolder ? ((ContentFolder) parent).getFiles().size() : 0;
        }
    }

    @Override
    public boolean isLeaf(@NonNull final Object node) {
        return node instanceof ContentFile;
    }

    @Override
    public void valueForPathChanged(@NonNull final TreePath path, @NonNull final Object newValue) {
    }

    @Override
    public int getIndexOfChild(@NonNull final Object parent, @NonNull final Object child) {
        if (parent == this) {
            return this.videoFiles.indexOf(child);
        } else {
            return ((ContentFolder) parent).getFiles().indexOf(child);
        }
    }

    @Override
    public void addTreeModelListener(@NonNull final TreeModelListener l) {
        this.treeListeners.add(l);
    }

    @Override
    public void removeTreeModelListener(@NonNull final TreeModelListener l) {
        this.treeListeners.remove(l);
    }

    @Override
    public String toString() {
        return this.currentRootFolder == null ? "NOT SELECTED" : this.currentRootFolder.getFileName().toString();
    }

    public static void openInSystem(@NonNull final ContentFile file) {
        LOGGER.info("Opening '{}' in system viewer", file.getFilePathAsString());
        if (Desktop.isDesktopSupported()) {
            final Desktop desktop = Desktop.getDesktop();
            if (desktop.isSupported(Desktop.Action.OPEN)) {
                try {
                    desktop.open(file.getFilePath().toFile());
                } catch (Exception x) {
                    LOGGER.error("Can't open file in Desktop", x); //NOI18N
                }
            } else if (SystemUtils.IS_OS_LINUX) {
                final Runtime runtime = Runtime.getRuntime();
                try {
                    runtime.exec("xdg-open \"" + file.getFilePathAsString() + "\""); //NOI18N
                } catch (IOException e) {
                    LOGGER.error("Can't open file under Linux", e); //NOI18N
                }
            } else if (SystemUtils.IS_OS_MAC) {
                final Runtime runtime = Runtime.getRuntime();
                try {
                    runtime.exec("open \"" + file.getFilePathAsString() + "\""); //NOI18N
                } catch (IOException e) {
                    LOGGER.error("Can't open file on MAC", e); //NOI18N
                }
            }
        }
    }

    public static URI extractUrlLinkFromFile(final File file) {
        URI result = null;
        if (file.isFile()) {
            if (file.getName().endsWith(".url") || (SystemUtils.IS_OS_WINDOWS && file.length() < 1024)) {
                try {
                    final String uri = new UrlFile(file).getURL();
                    result = uri == null ? null : new URI(uri);
                } catch (URISyntaxException | IOException ex) {
                    result = null;
                }
            }
        }
        return result;
    }

}
