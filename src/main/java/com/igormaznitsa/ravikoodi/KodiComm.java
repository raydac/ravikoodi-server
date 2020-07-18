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
import com.igormaznitsa.ravikoodi.kodijsonapi.PlayerSeekResult;
import com.igormaznitsa.ravikoodi.kodijsonapi.Subtitle;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class KodiComm {

    private static final Logger LOGGER = LoggerFactory.getLogger(KodiComm.class);

    private final ApplicationPreferences preferences;

    @Autowired
    public KodiComm(final ApplicationPreferences preferences) {
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
            )));
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

    public void doPlayerStartPause(@NonNull final ActivePlayerInfo playerInfo) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            service.get().doPlayerStartPause(playerInfo);
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
    public PlayerSeekResult doPlayerSeekPercentage(@NonNull final ActivePlayerInfo playerInfo, final double percentage) throws Throwable {
        final Optional<KodiService> service = this.makeKodiService();
        if (service.isPresent()) {
            return service.get().doPlayerSeekPercentage(playerInfo, percentage);
        } else {
            throw new IllegalStateException("Can't get kodi service");
        }
    }

}
