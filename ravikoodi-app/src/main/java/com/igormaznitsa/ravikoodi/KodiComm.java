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

import com.igormaznitsa.ravikoodi.kodijsonapi.ActivePlayerInfo;
import com.igormaznitsa.ravikoodi.kodijsonapi.AudioStream;
import com.igormaznitsa.ravikoodi.kodijsonapi.KodiService;
import com.igormaznitsa.ravikoodi.kodijsonapi.PlayerItem;
import com.igormaznitsa.ravikoodi.kodijsonapi.PlayerProperties;
import com.igormaznitsa.ravikoodi.kodijsonapi.RepeatValue;
import com.igormaznitsa.ravikoodi.kodijsonapi.PlayerSeekResult;
import com.igormaznitsa.ravikoodi.kodijsonapi.PlayerSpeedIncDecReq;
import com.igormaznitsa.ravikoodi.kodijsonapi.Playlist;
import com.igormaznitsa.ravikoodi.kodijsonapi.PlaylistFileItem;
import com.igormaznitsa.ravikoodi.kodijsonapi.Subtitle;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class KodiComm {

    private static final Logger LOGGER = LoggerFactory.getLogger(KodiComm.class);

    private final ApplicationPreferences preferences;
    private final InternalServer internalServer;
    private final UploadingFileRegistry fileRegstry;
    private final MimeTypes mimeTypes;
    
    @Autowired
    public KodiComm(
        @NonNull final InternalServer internalServer,
        @NonNull final ApplicationPreferences preferences,
        @NonNull final UploadingFileRegistry fileRegistry,
        @NonNull final MimeTypes mimeTypes
    ) {
        this.mimeTypes = mimeTypes;
        this.fileRegstry = fileRegistry;
        this.internalServer = internalServer;
        this.preferences = preferences;
    }

    @NonNull
    public List<ActivePlayerInfo> findActivePlayers() throws Throwable {
        final Optional<KodiService> kodiService = this.makeKodiService();
        if (kodiService.isPresent()) {
            return Arrays.asList(kodiService.get().getActivePlayers());
        } else {
            return Collections.emptyList();
        }
    }

    @NonNull
    public Optional<KodiService> makeKodiService() {
        Optional<KodiService> result;
        try {
            result = Optional.of(new KodiService(new KodiAddress(
                this.preferences.getKodiAddress(),
                this.preferences.getKodiPort(),
                this.preferences.getKodiName(),
                this.preferences.getKodiPassword(),
                this.preferences.isKodiSsl()
            ), this.preferences.getJsonRequestTimeout()));
        } catch (MalformedURLException ex) {
            LOGGER.error("Can't create kodi service : {}", ex.getMessage());
            result = Optional.empty();
        }
        return result;
    }

    @NonNull
    public PlayerItem getPlayerItem(ActivePlayerInfo playerInfo) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            return service.get().getPlayerItem(playerInfo);
        } else {
            throw new IllegalStateException("Can't get kodi service");
        }
    }

    @NonNull
    public PlayerProperties getPlayerProperties(@NonNull final ActivePlayerInfo playerInfo, final String... properties) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            return service.get().getPlayerProperties(playerInfo, properties);
        } else {
            throw new IllegalStateException("Can't get kodi service");
        }
    }

    public void doPlayerStop(@NonNull final ActivePlayerInfo playerInfo) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            service.get().doPlayerStop(playerInfo);
        } else {
            throw new IllegalStateException("Can't get kodi service");
        }
    }

    public void setPlayerSpeed(@NonNull final ActivePlayerInfo playerInfo, final long next) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            service.get().setPlayerSpeed(playerInfo, next);
        } else {
            throw new IllegalStateException("Can't get kodi service");
        }
    }

    public void incPlayerSpeed(@NonNull final ActivePlayerInfo playerInfo) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            service.get().setPlayerSpeed(playerInfo, PlayerSpeedIncDecReq.Direction.INCREMENT);
        } else {
            throw new IllegalStateException("Can't get kodi service");
        }
    }

    public void decPlayerSpeed(@NonNull final ActivePlayerInfo playerInfo) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            service.get().setPlayerSpeed(playerInfo, PlayerSpeedIncDecReq.Direction.DECREMENT);
        } else {
            throw new IllegalStateException("Can't get kodi service");
        }
    }

    public void doPlayerStartPause(@NonNull final ActivePlayerInfo playerInfo) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            service.get().doPlayerStartPause(playerInfo);
        } else {
            throw new IllegalStateException("Can't get kodi service");
        }
    }

    public void doPlayerRepeat(@NonNull final ActivePlayerInfo playerInfo, @NonNull final RepeatValue repeat) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            service.get().doPlayerRepeat(playerInfo, repeat.name().toLowerCase(Locale.ENGLISH));
        } else {
            throw new IllegalStateException("Can't get kodi service");
        }
    }

    public void setPlayerAudiostream(@NonNull final ActivePlayerInfo playerInfo, @NonNull final AudioStream newAudioStream) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            service.get().setPlayerAudiostream(playerInfo, newAudioStream);
        } else {
            throw new IllegalStateException("Can't get kodi service");
        }
    }

    public void setPlayerSubtitle(@NonNull final ActivePlayerInfo playerInfo, @NonNull final Subtitle subtitle, final boolean enable) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            service.get().setPlayerSubtitle(playerInfo, subtitle, enable);
        } else {
            throw new IllegalStateException("Can't get kodi service");
        }
    }

    @NonNull
    public Optional<String> openFileThroughRegistry(@NonNull final Path path, @Nullable final byte[] data, @NonNull final Map<String,String>... options) throws Throwable {
        final String uid = UUID.randomUUID().toString();
        final UploadFileRecord record = this.fileRegstry.registerFile(uid, path, data);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final String fileUrl = this.internalServer.makeUrlFor(record);
        LOGGER.info("Opening file {} as {}, uuid={}", path, fileUrl, uid);
        return Optional.ofNullable(this.doPlayerOpenFile(fileUrl, options) ? uid : null);
    }

    @NonNull
    public Optional<Pair<Playlist,String>> openFileAsPlaylistThroughRegistry(@NonNull final Path path, @Nullable final byte[] data, @NonNull final Map<String,String>... options) throws Throwable {
        final String uid = UUID.randomUUID().toString();
        final UploadFileRecord record = this.fileRegstry.registerFile(uid, path, data);
        final AtomicReference<Throwable> error = new AtomicReference<>();
        final String fileUrl = this.internalServer.makeUrlFor(record);
        LOGGER.info("Opening file {} as {} through playlist, uuid={}", path, fileUrl, uid);

        final MimeTypes.MimeRecord foundMimeRecord = this.mimeTypes.getMimeRecord(path);

        final String playListType;
        if (foundMimeRecord == null) {
            playListType = "unknown";
        } else if (foundMimeRecord.getContentType() == MimeTypes.ContentType.AUDIO) {
            playListType = "audio";
        } else if (foundMimeRecord.getContentType() == MimeTypes.ContentType.VIDEO) {
            playListType = "video";
        } else if (foundMimeRecord.getContentType() == MimeTypes.ContentType.PICTURE) {
            playListType = "picture";
        } else {
            playListType = "mixed";
        }
        
        final KodiService service = this.makeKodiService().orElseThrow(()->new IllegalStateException("Can't get kodi service"));
        
        final Playlist [] foundLists = service.getPlaylists();
        LOGGER.info("Found play lists: {}", Arrays.toString(foundLists));

        Optional<Playlist> foundTargetList = Stream.of(foundLists).filter(x -> playListType.equalsIgnoreCase(x.getType()))
            .findFirst();
        if (!foundTargetList.isPresent()){
            foundTargetList = Stream.of(foundLists).filter(x -> playListType.equalsIgnoreCase("unknown")).findFirst();
        }
        
        if (foundTargetList.isPresent()) {
            final Playlist targetList = foundTargetList.get();
            LOGGER.info("Selected target list is {}", targetList);
            if (!isOk(service.clearPlaylist(targetList))){
                LOGGER.error("Can't clear play list");
                throw new IllegalStateException("Can't clear play list");
            }
            final PlaylistFileItem item = new PlaylistFileItem();
            item.setFile(fileUrl);
            final String resultForAddPlayerItem = service.addPlaylistItem(targetList, item);
            if (isOk(resultForAddPlayerItem)) {
                return Optional.ofNullable(isOk(service.doPlayerOpenPlaylist(targetList, Collections.singletonMap("repeat","one"))) ? Pair.of(targetList, uid) : null);
            } else {
                LOGGER.error("Can't add player list item");
                throw new IOException("Can't add player list item: " + resultForAddPlayerItem);
            }
        } else {
            throw new IOException("Can't find any player list for type: "+playListType);
        }
    }

    @NonNull
    public PlayerSeekResult doPlayerSeekPercentage(@NonNull final ActivePlayerInfo playerInfo, final double percentage) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            return service.get().doPlayerSeekPercentage(playerInfo, percentage);
        } else {
            throw new IllegalStateException("Can't get kodi service");
        }
    }

    private boolean isOk(final String result) {
        final boolean ok = "ok".equalsIgnoreCase(result);
        if (!ok) {
            LOGGER.warn("Error response: {}", result);
        }
        return ok;
    }
    
    @NonNull
    public boolean doPlayerOpenFile(@NonNull final String fileUrl, @NonNull final Map<String,String> ... options) throws Throwable {
        final KodiService service = this.makeKodiService().orElseThrow(() -> new IllegalStateException("Can't get kodi service"));
            final String result = service.doPlayerOpenFile(fileUrl, options);
            LOGGER.info("Player open response for '{}' is '{}'", fileUrl, result);
            return isOk(result);
    }

}
