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
import com.igormaznitsa.ravikoodi.kodijsonapi.PlayerItem;
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class TimerScheduler {

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    private static final Logger LOGGER = LoggerFactory.getLogger(TimerScheduler.class);

    private final ScheduledExecutorService executor;
    private final ApplicationPreferences preferences;
    private final List<ScheduledTimer> timers = new ArrayList<>();
    private final KodiComm kodiComm;
    private final GuiMessager guiMessager;

    @Autowired
    public TimerScheduler(
        @NonNull final GuiMessager guiMessager,
        @NonNull final KodiComm kodiComm,
        @NonNull final ScheduledExecutorService executor,
        @NonNull final ApplicationPreferences preferences
    ) {
        this.guiMessager = guiMessager;
        this.executor = executor;
        this.preferences = preferences;
        this.kodiComm = kodiComm;
    }

    public void reloadTimers() {
        synchronized (this.timers) {
            LOGGER.info("Cancelling {} current timers", this.timers.size());
            timers.forEach(x -> x.cancel());
            timers.clear();
            final AtomicLong counter = new AtomicLong(1L);

            final List<ApplicationPreferences.Timer> prefereceTimers = preferences.getTimers();

            this.timers.addAll(prefereceTimers.stream()
                .filter(x -> x.isEnabled() && x.getFrom() != null && x.getResourcePath() != null)
                .map(
                    x -> new ScheduledTimer(
                        this.guiMessager,
                        this.kodiComm,
                        String.format("Timer#%d[%s]", counter.getAndIncrement(), x.getName()),
                        x,
                        this.executor)
                )
                .collect(Collectors.toList()));

            LOGGER.info("Detected {} active timers", this.timers.size());

            this.timers.forEach(timer -> {
                try {
                    timer.init();
                } catch (Exception ex) {
                    LOGGER.error("Can't init timer {}: {}", timer, ex.getMessage());
                }
            });

            final long numberOfTimersWithResource = prefereceTimers.stream().filter(x -> x.isEnabled() && x.getResourcePath()!=null).count();
            if (numberOfTimersWithResource>0) {
                this.guiMessager.showInfoMessage("Timers", String.format("Activated %d timers of %d", this.timers.size(), numberOfTimersWithResource));
            }
        }
    }

    private static final class ScheduledTimer {

        private final GuiMessager guiMessager;
        private final String id;
        private final File resource;
        private final KodiComm kodiComm;
        private final ScheduledExecutorService executorService;
        private final ApplicationPreferences.Timer timer;
        private final AtomicReference<ScheduledFuture<?>> scheduledFutureRef = new AtomicReference<>();
        private final AtomicReference<UUID> lastUuid = new AtomicReference<>();
        public ScheduledTimer(
            @NonNull final GuiMessager guiMessager,
            @NonNull final KodiComm kodiComm,
            @NonNull final String id,
            @NonNull final ApplicationPreferences.Timer timer,
            @NonNull final ScheduledExecutorService executorService
        ) {
            this.guiMessager = guiMessager;
            this.kodiComm = kodiComm;
            this.id = id;
            this.timer = timer;
            this.executorService = executorService;
            this.resource = timer.getResourcePath();
        }

        private ScheduledFuture<?> scheduleStart() {
            return this.scheduleForTime(Objects.requireNonNull(this.timer.getFrom()), time -> {
                LOGGER.info("Scheduling start of timer {} for {}", this.id, time.format(DATETIME_FORMATTER));
            }, this::onStart);
        }

        void cancel() {
            final ScheduledFuture<?> scheduledAction = this.scheduledFutureRef.getAndSet(null);
            if (scheduledAction != null) {
                scheduledAction.cancel(false);
            }
        }

        private ScheduledFuture<?> scheduleEnd() {
            final LocalTime endTime = this.timer.getTo();
            if (endTime == null) {
                return null;
            }

            return this.scheduleForTime(endTime, time -> {
                LOGGER.info("Scheduling end of timer {} for {}", this.id, time.format(DATETIME_FORMATTER));
            }, this::onEnd);
        }

        private ScheduledFuture<?> scheduleForTime(
            @NonNull final LocalTime time,
            @NonNull final Consumer<LocalDateTime> consumerOfCalculatedTime,
            @NonNull final Runnable call
        ) {
            final LocalDate now = LocalDate.now();

            LocalDateTime scheduledTime = time.atDate(now);
            if (scheduledTime.isBefore(LocalDateTime.now())) {
                scheduledTime = time.atDate(now.plusDays(1));
            }

            consumerOfCalculatedTime.accept(scheduledTime);
            return this.executorService.schedule(
                call,
                LocalDateTime.now().until(scheduledTime, ChronoUnit.SECONDS),
                TimeUnit.SECONDS
            );
        }

        private void onStart() {
            LOGGER.info("Starting {} timer", this.id);

            if (!this.resource.isFile()) {
                LOGGER.error("Can't find resource file for timer {}: {}", this.timer, this.resource);
                this.guiMessager.showWarningMessage("Error", "Can't find resource file: "+this.resource);
                return;
            }

            try {
                this.kodiComm.openFileThroughRegistry(this.resource.toPath(), null).ifPresent(uuid -> {
                    this.lastUuid.set(uuid);
                    if (this.timer.getTo() == null) {
                        this.scheduledFutureRef.set(scheduleStart());
                    } else {
                        this.scheduledFutureRef.set(scheduleEnd());
                    }
                });
            } catch (Throwable ex) {
                this.guiMessager.showErrorMessage("Error", "Can't start timer " + this.timer + ": " + ex.getMessage());
            }
        }

        private void onEnd() {
            LOGGER.info("Ending {} timer", this.id);
            try {
                final UUID uuid = this.lastUuid.getAndSet(null);
                if (uuid != null) {
                    final List<ActivePlayerInfo> players = this.kodiComm.findActivePlayers();
                    final Optional<ActivePlayerInfo> foundPlayer = players.stream()
                        .filter(player -> {
                            try {
                                final PlayerItem item = this.kodiComm.getPlayerItem(player);
                                return item.getItem().getFile().contains(uuid.toString());
                            } catch (Throwable th) {
                                return false;
                            }
                        }).findFirst();

                    if (!foundPlayer.isPresent()) {
                        LOGGER.warn("Can't find active player for timer {}", this);
                    } else {
                        foundPlayer.ifPresent(info -> {
                            LOGGER.info("Detected started player {} for timer {}", info.getPlayerid(), this);
                            try {
                                this.kodiComm.doPlayerStop(info);
                            } catch (Throwable ex) {
                                this.guiMessager.showErrorMessage("Error", "Error during timer stop " + ex.getMessage() + ": " + ex.getMessage());
                            }
                        });
                    }
                }
            } catch (Throwable ex) {
                LOGGER.error("Error during timer {} end processing", this, ex);
            }
            this.scheduledFutureRef.set(scheduleStart());
        }


        public void init() {
            if (!this.resource.isFile()) {
                throw new IllegalArgumentException("Can't find timer resource file: " + this.resource.getAbsolutePath());
            }

            if (!this.scheduledFutureRef.compareAndSet(null, this.scheduleStart())) {
                throw new IllegalStateException("Already scheduled action detected: " + this.id);
            }
        }

        @Override
        public String toString() {
            return this.id;
        }
    }

}
