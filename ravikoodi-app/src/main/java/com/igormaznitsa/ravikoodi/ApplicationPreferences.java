package com.igormaznitsa.ravikoodi;

import com.igormaznitsa.ravikoodi.prefs.StaticResource;
import com.igormaznitsa.ravikoodi.prefs.TimerResource;
import static com.igormaznitsa.ravikoodi.Utils.isBlank;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.UIManager;
import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class ApplicationPreferences {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationPreferences.class);

    public enum SpeedProfile {
        VERYSLOW("Very slow", "veryslow"),
        SLOWER("Slower", "slower"),
        SLOW("Slow", "slow"),
        MEDIUM("Medium", "medium"),
        FAST("Fast", "fast"),
        FASTER("Faster", "faster"),
        VERYFAST("Very-fast", "veryfast"),
        SUPERFAST("Super-fast", "superfast"),
        ULTRAFAST("Ultra-fast", "ultrafast");

        private final String viewName;
        private final String ffmpegName;

        private SpeedProfile(final String viewName, final String ffmpegName) {
            this.viewName = viewName;
            this.ffmpegName = ffmpegName;
        }

        @NonNull
        public String getFfmpegName() {
            return this.ffmpegName;
        }

        @NonNull
        public String getViewName() {
            return this.viewName;
        }

        @NonNull
        public static SpeedProfile findForViewName(@NonNull final String name) {
            return Stream.of(SpeedProfile.values())
                .filter(x -> x.viewName.equalsIgnoreCase(name))
                .findFirst()
                .orElse(FASTER);
        }
    }


    public enum GrabberType {
        AUTO,
        ROBOT,
        FFMPEG;

        @NonNull
        public static GrabberType findForName(@Nullable final String name) {
            return Stream.of(GrabberType.values())
                .filter(x -> x.name().equalsIgnoreCase(name))
                .findFirst()
                .orElse(AUTO);
        }
    }

    public enum Quality {
        MODE144P("144p", "-2:144", 144),
        MODE240P("240p", "-2:240", 240),
        MODE360P("360p", "-2:360", 360),
        MODE480P("480p", "-2:480", 480),
        MODE720P("720p", "-2:720", 720),
        MODE1080P("1080p", "-2:1080", 1080);

        private final String viewName;
        private final String ffmpegScale;
        private final int height;

        private Quality(final String viewName, final String ffmpegScale, final int height) {
            this.viewName = viewName;
            this.ffmpegScale = ffmpegScale;
            this.height = height;
        }

        public int getHeight() {
            return this.height;
        }

        @NonNull
        public String getViewName() {
            return this.viewName;
        }

        @NonNull
        public String getFfmpegScale() {
            return this.ffmpegScale;
        }

        @NonNull
        public static Quality findForViewName(@NonNull final String name) {
            return Stream.of(Quality.values())
                .filter(x -> x.getViewName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(MODE720P);
        }
    }

    public enum Option {
        SCREENCAST_THREADS("screencast.threads"),
        SCREENCAST_SOUND_INPUT("screencast.sound.input"),
        SCREENCAST_GRAB_CURSOR("screencast.grab.cursor"),
        SCREENCAST_FFMPEG_PATH("screencast.ffmpeg.path"),
        SCREENCAST_SNAPSPERSECOND("screencast.snaps.per.second"),
        SCREENCAST_QUALITY("screencast.quality"),
        SCREENCAST_BANDWIDTH("screencast.bandwidth"),
        SCREENCAST_SOUNDOFFSET("screencast.sndoffset"),
        SCREENCAST_CRF("screencast.crf"),
        SCREENCAST_SPEED_PROFILE("screencast.speed.profile"),
        SCREENCAST_GRABBER_TYPE("screencast.grabber.type"),
        SERVER_PORT("server.port"),
        SERVER_INTERFACE("server.interface"),
        SERVER_SSL("server.ssl"),
        FILE_ROOT("file.root"),
        LANDF("lookandfeel.class"),
        TIMERS("timers.list"),
        STATIC_RESOURCES("static.resources.list"),
        KODI_ADDRESS("kodi.address"),
        KODI_PORT("kodi.port"),
        KODI_NAME("kodi.name"),
        KODI_SSL("kodi.ssl"),
        KODI_PASSWORD("kodi.password"),
        GENERAL_SCALE_UI("ui.scale");

        private final String propertyName;

        private Option(final String propertyName) {
            this.propertyName = propertyName;
        }

        @NonNull
        public String getPropertyName() {
            return this.propertyName;
        }
    }

    private final Preferences preferences;

    public ApplicationPreferences() {
        this.preferences = findPreferences();
    }

    public static Preferences findPreferences() {
        return Preferences.userNodeForPackage(ApplicationPreferences.class);
    }
    
    @NonNull
    public List<TimerResource> getTimers() {
        synchronized (this.preferences) {
            final String value = this.preferences.get(Option.TIMERS.getPropertyName(), null);
            if (!isBlank(value)) {
                final TimerResource ERROR = new TimerResource("<>");
                return Arrays.stream(value.split("\\r?\\n"))
                    .filter(m -> !isBlank(m))
                    .map(m -> {
                        try {
                            return TimerResource.fromBase64(m.trim());
                        } catch (IOException ex) {
                            return ERROR;
                        }
                    })
                    .filter(x -> x != ERROR)
                    .collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        }
    }

    @NonNull
    public List<StaticResource> getStaticResources() {
        synchronized (this.preferences) {
            final String value = this.preferences.get(Option.STATIC_RESOURCES.getPropertyName(), null);
            if (!isBlank(value)) {
                final StaticResource ERROR = new StaticResource("<>");
                return Arrays.stream(value.split("\\r?\\n"))
                    .filter(m -> !isBlank(m))
                    .map(m -> {
                        try {
                            return StaticResource.fromBase64(m.trim());
                        } catch (IOException ex) {
                            return ERROR;
                        }
                    })
                    .filter(x -> x != ERROR)
                    .collect(Collectors.toList());
            } else {
                return Collections.emptyList();
            }
        }
    }

    public void setStaticResources(@Nullable final List<StaticResource> resources) {
        if (resources == null || resources.isEmpty()) {
            synchronized (this.preferences) {
                this.preferences.remove(Option.STATIC_RESOURCES.getPropertyName());
            }
        } else {
            final StringBuilder buffer = new StringBuilder();
            for (final StaticResource t : resources) {
                if (buffer.length() > 0) {
                    buffer.append('\n');
                }
                buffer.append(t.toBase64());
            }
            synchronized (this.preferences) {
                this.preferences.put(Option.STATIC_RESOURCES.getPropertyName(), buffer.toString());
            }
        }
    }


    public void setTimers(@Nullable final List<TimerResource> timers) {
        if (timers == null || timers.isEmpty()) {
            synchronized (this.preferences) {
                this.preferences.remove(Option.TIMERS.getPropertyName());
            }
        } else {
            final StringBuilder buffer = new StringBuilder();
            for (final TimerResource t : timers) {
                if (buffer.length() > 0) {
                    buffer.append('\n');
                }
                buffer.append(t.toBase64());
            }
            synchronized (this.preferences) {
                this.preferences.put(Option.TIMERS.getPropertyName(), buffer.toString());
            }
        }
    }

    @NonNull
    public String getLookAndFeelClassName() {
        synchronized (this.preferences) {
            return this.preferences.get(Option.LANDF.getPropertyName(), UIManager.getSystemLookAndFeelClassName());
        }
    }

    public void setLookAndFeelClassName(@Nullable final String lafClassName) {
        synchronized (this.preferences) {
            this.preferences.put(Option.LANDF.getPropertyName(), lafClassName == null ? UIManager.getSystemLookAndFeelClassName() : lafClassName);
        }
    }

    @NonNull
    public GrabberType getGrabberType() {
        synchronized (this.preferences) {
            return GrabberType.findForName(this.preferences.get(Option.SCREENCAST_GRABBER_TYPE.getPropertyName(), GrabberType.AUTO.name()));
        }
    }

    public void setGrabberType(@NonNull final GrabberType grabberType) {
        synchronized (this.preferences) {
            this.preferences.put(Option.SCREENCAST_GRABBER_TYPE.getPropertyName(), grabberType.name());
        }
    }

    @NonNull
    public String getSoundInput() {
        synchronized (this.preferences) {
            return this.preferences.get(Option.SCREENCAST_SOUND_INPUT.getPropertyName(), "");
        }
    }

    @NonNull
    public void setSoundnput(final String value) {
        synchronized (this.preferences) {
            this.preferences.put(Option.SCREENCAST_SOUND_INPUT.getPropertyName(), value == null || value.trim().length() == 0 ? "" : value);
        }
    }

    public int getBandwidth() {
        synchronized (this.preferences) {
            return Math.max(1, this.preferences.getInt(Option.SCREENCAST_BANDWIDTH.getPropertyName(), 5));
        }
    }

    public void setBandwidth(final int bandwidth) {
        synchronized (this.preferences) {
            this.preferences.putInt(Option.SCREENCAST_BANDWIDTH.getPropertyName(), Math.max(1, bandwidth));
        }
    }

    public int getThreads() {
        synchronized (this.preferences) {
            return Math.max(0, this.preferences.getInt(Option.SCREENCAST_THREADS.getPropertyName(), 0));
        }
    }

    public void setThreads(final int threads) {
        synchronized (this.preferences) {
            this.preferences.putInt(Option.SCREENCAST_THREADS.getPropertyName(), Math.max(0, threads));
        }
    }

    public int getCrf() {
        synchronized (this.preferences) {
            return Math.max(-1, this.preferences.getInt(Option.SCREENCAST_CRF.getPropertyName(), -1));
        }
    }

    public void setCrf(final int value) {
        synchronized (this.preferences) {
            this.preferences.putInt(Option.SCREENCAST_CRF.getPropertyName(), Math.max(-1, value));
        }
    }

    public float getSoundOffset() {
        synchronized (this.preferences) {
            return this.preferences.getFloat(Option.SCREENCAST_SOUNDOFFSET.getPropertyName(), 0.0f);
        }
    }

    public void setSoundOffset(final float value) {
        synchronized (this.preferences) {
            this.preferences.putFloat(Option.SCREENCAST_SOUNDOFFSET.getPropertyName(), value);
        }
    }

    @NonNull
    public SpeedProfile getSpeedProfile() {
        synchronized (this.preferences) {
            return SpeedProfile.findForViewName(this.preferences.get(Option.SCREENCAST_SPEED_PROFILE.getPropertyName(), SpeedProfile.FASTER.getViewName()));
        }
    }

    public void setSpeedProfile(@NonNull final SpeedProfile value) {
        synchronized (this.preferences) {
            this.preferences.put(Option.SCREENCAST_SPEED_PROFILE.getPropertyName(), value.getViewName());
        }
    }

    @NonNull
    public Quality getQuality() {
        synchronized (this.preferences) {
            return Quality.findForViewName(this.preferences.get(Option.SCREENCAST_QUALITY.getPropertyName(), Quality.MODE360P.getViewName()));
        }
    }

    public void setQuality(@NonNull final Quality value) {
        synchronized (this.preferences) {
            this.preferences.put(Option.SCREENCAST_QUALITY.getPropertyName(), value.getViewName());
        }
    }

    @NonNull
    public String getKodiAddress() {
        synchronized (this.preferences) {
            return this.preferences.get(Option.KODI_ADDRESS.getPropertyName(), InetAddress.getLoopbackAddress().getHostAddress());
        }
    }

    @NonNull
    public void setKodiAddress(@Nullable final String address) {
        synchronized (this.preferences) {
            this.preferences.put(Option.KODI_ADDRESS.getPropertyName(), address == null || isBlank(address) ? InetAddress.getLoopbackAddress().getHostAddress() : address.trim());
        }
    }

    public int getKodiPort() {
        synchronized (this.preferences) {
            return this.preferences.getInt(Option.KODI_PORT.getPropertyName(), 80);
        }
    }

    public void setKodiPort(final int port) {
        synchronized (this.preferences) {
            this.preferences.putInt(Option.KODI_PORT.getPropertyName(), port < 0 || port > 65535 ? 80 : port);
        }
    }

    public static int getScaleUi(final Preferences preferences) {
        synchronized (preferences) {
            return preferences.getInt(Option.GENERAL_SCALE_UI.getPropertyName(), 1);
        }
    }
    
    public int getScaleUi() {
        return getScaleUi(this.preferences);
    }

    public void setScaleUi(final int scale) {
        synchronized (this.preferences) {
            this.preferences.putInt(Option.GENERAL_SCALE_UI.getPropertyName(), scale < 1 || scale > 5 ? 1 : scale);
        }
    }

    @NonNull
    public String getKodiName() {
        synchronized (this.preferences) {
            return this.preferences.get(Option.KODI_NAME.getPropertyName(), "");
        }
    }

    @NonNull
    public String getKodiPassword() {
        synchronized (this.preferences) {
            return this.preferences.get(Option.KODI_PASSWORD.getPropertyName(), "");
        }
    }

    public void setKodiSsl(final boolean enableSsl) {
        synchronized (this.preferences) {
            this.preferences.putBoolean(Option.KODI_SSL.getPropertyName(), enableSsl);
        }
    }

    public boolean isKodiSsl() {
        synchronized (this.preferences) {
            return this.preferences.getBoolean(Option.KODI_SSL.getPropertyName(), false);
        }
    }

    public void setKodiName(@Nullable final String name) {
        synchronized (this.preferences) {
            this.preferences.put(Option.KODI_NAME.getPropertyName(), name == null ? "" : name);
        }
    }

    public void setKodiPassword(@Nullable final String password) {
        synchronized (this.preferences) {
            this.preferences.put(Option.KODI_PASSWORD.getPropertyName(), password == null ? "" : password);
        }
    }

    @NonNull
    public String getFileRoot() {
        synchronized (this.preferences) {
            return this.preferences.get(Option.FILE_ROOT.getPropertyName(), "");
        }
    }

    public void setFileRoot(@Nullable final String fileRoot) {
        synchronized (this.preferences) {
            this.preferences.put(Option.FILE_ROOT.getPropertyName(), fileRoot == null ? "" : fileRoot);
        }
    }

    public String getServerHost() {
        synchronized (this.preferences) {
            try {
                return this.preferences.get(Option.SERVER_INTERFACE.getPropertyName(), InetAddress.getLocalHost().getHostAddress());
            } catch (UnknownHostException ex) {
                return "localhost";
            }
        }
    }

    public String getFfmpegPath() {
        synchronized (this.preferences) {
            return this.preferences.get(Option.SCREENCAST_FFMPEG_PATH.getPropertyName(), SystemUtils.IS_OS_WINDOWS ? "ffmpeg.exe" : "ffmpeg");
        }
    }

    public boolean isGrabCursor() {
        synchronized (this.preferences) {
            return this.preferences.getBoolean(Option.SCREENCAST_GRAB_CURSOR.getPropertyName(), false);
        }
    }

    public int getSnapsPerSecond() {
        synchronized (this.preferences) {
            return this.preferences.getInt(Option.SCREENCAST_SNAPSPERSECOND.getPropertyName(), 10);
        }
    }

    public boolean isServerSsl() {
        synchronized (this.preferences) {
            return this.preferences.getBoolean(Option.SERVER_SSL.getPropertyName(), false);
        }
    }

    public void setServerSsl(final boolean useSsl) {
        synchronized (this.preferences) {
            this.preferences.putBoolean(Option.SERVER_SSL.getPropertyName(), useSsl);
        }
    }

    public void flush() {
        synchronized (this.preferences) {
            try {
                this.preferences.flush();
            } catch (BackingStoreException ex) {
                LOGGER.error("Can't flush preferences", ex);
            }
        }
    }

    public void setServerInterface(final String netInterface) {
        synchronized (this.preferences) {
            this.preferences.put(Option.SERVER_INTERFACE.getPropertyName(), netInterface);
        }
    }

    public void setGrabCursor(final boolean value) {
        synchronized (this.preferences) {
            this.preferences.putBoolean(Option.SCREENCAST_GRAB_CURSOR.getPropertyName(), value);
        }
    }

    public void setSnapsPerSecond(final int value) {
        synchronized (this.preferences) {
            this.preferences.putInt(Option.SCREENCAST_SNAPSPERSECOND.getPropertyName(), Math.min(Math.max(1, value), 50));
        }
    }

    public void setFfmpegPath(final String value) {
        synchronized (this.preferences) {
            this.preferences.put(Option.SCREENCAST_FFMPEG_PATH.getPropertyName(), value == null || value.trim().length() == 0
                ? SystemUtils.IS_OS_WINDOWS ? "ffmpeg.exe" : "ffmpeg" : value);
        }
    }

    public int getServerPort() {
        synchronized (this.preferences) {
            return this.preferences.getInt(Option.SERVER_PORT.getPropertyName(), 0);
        }
    }

    public void setServerPort(final int port) {
        synchronized (this.preferences) {
            this.preferences.putInt(Option.SERVER_PORT.getPropertyName(), Math.max(0, port));
        }
    }

}
