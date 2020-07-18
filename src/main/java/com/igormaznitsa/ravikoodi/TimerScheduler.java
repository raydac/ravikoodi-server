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
import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
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
    
    @Autowired
    public TimerScheduler(
        @NonNull final KodiComm kodiComm,
        @NonNull final ScheduledExecutorService executor, 
        @NonNull final ApplicationPreferences preferences) {
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
            this.timers.addAll(preferences.getTimers().stream()
                .filter(x -> x.isEnabled() && x.getFrom() != null)
                .map(x -> new ScheduledTimer(this.kodiComm, String.format("Timer#%d[%s]", counter.getAndIncrement(), x.getName()), x, this.executor))
                .collect(Collectors.toList()));

            LOGGER.info("Detected {} active timers", this.timers.size());

            this.timers.forEach(timer -> {
                try {
                    timer.init();
                } catch (Exception ex) {
                    LOGGER.error("Can't init timer {}: {}", timer, ex.getMessage());
                }
            });
        }
    }

    private static final class ScheduledTimer {

        private final String id;
        private final File resource;
        private final KodiComm kodiComm;
        private final ScheduledExecutorService executorService;
        private final ApplicationPreferences.Timer timer;
        private final AtomicReference<ScheduledFuture<?>> scheduledFutureRef = new AtomicReference<>();

        private ScheduledFuture<?> scheduleStart() {
            return this.scheduleForTime(Objects.requireNonNull(this.timer.getFrom()), time -> {
                LOGGER.info("Scheduling start of timer {} for {}", this.id, time.format(DATETIME_FORMATTER));
            }, this::onStart);
        }

        void cancel() {
            final ScheduledFuture<?> scheduledAction = this.scheduledFutureRef.getAndSet(null);
            scheduledAction.cancel(false);
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

            if (this.timer.getTo() == null) {
                this.scheduledFutureRef.set(scheduleStart());
            } else {
                this.scheduledFutureRef.set(scheduleEnd());
            }
        }

        private void onEnd() {
            LOGGER.info("Ending {} timer", this.id);
            try{
//              final ActivePlayerInfo [] players = this.kodiComm.getActivePlayersInfo();
//              if (players!=null) {
//              }
            }catch(Throwable ex){
                
            }
            this.scheduledFutureRef.set(scheduleStart());
        }

        public ScheduledTimer(
            @NonNull final KodiComm kodiComm,
            @NonNull final String id,
            @NonNull final ApplicationPreferences.Timer timer,
            @NonNull final ScheduledExecutorService executorService) {
            this.kodiComm = kodiComm;
            this.id = id;
            this.timer = timer;
            this.executorService = executorService;
            this.resource = timer.getResourcePath();
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
