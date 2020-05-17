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
package com.igormaznitsa.ravikoodi.screencast;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import javax.annotation.PostConstruct;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class JavaSoundAdapter {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaSoundAdapter.class);
  private final List<SoundPort> foundPorts = new CopyOnWriteArrayList<>();

  public static final class SoundPort {

    private final UUID uuid = UUID.randomUUID();
    private final String name;
    private final Mixer mixer;
    private final Line line;

    public SoundPort(@NonNull final Mixer mixer, @NonNull final String name, @NonNull final Line line) {
      this.mixer = mixer;
      this.name = name;
      this.line = line;
    }

    public synchronized int read(final byte[] buffer, final int start, final int len) {
      final TargetDataLine targetDataLine = (TargetDataLine) this.line;
      final int toread = Math.min(targetDataLine.available(), len);
      if (toread == 0) {
        return targetDataLine.read(buffer, start, len);
      } else if (toread > 0) {
        return targetDataLine.read(buffer, start, toread);
      } else {
        return -1;
      }
    }

    public synchronized void start(final AudioFormat format) throws LineUnavailableException {
      final TargetDataLine targetDataLine = (TargetDataLine) this.line;
      if (targetDataLine.isActive()) {
        LOGGER.info("Sound line already active: {}", this);
        return;
      }
      if (!targetDataLine.isOpen()) {
        LOGGER.info("Opening sound line: {}", targetDataLine);
        targetDataLine.open(format, Math.round(format.getFrameRate()) * format.getFrameSize() * 2);
      }
      if (!targetDataLine.isActive() && !targetDataLine.isRunning()) {
        LOGGER.info("Starting sound line: {}", this);
        targetDataLine.start();
      }
      LOGGER.info("Start sound line completed: {}", this);
    }

    public synchronized void close() {
      TargetDataLine targetDataLine = (TargetDataLine) this.line;
      if (targetDataLine.isActive() || targetDataLine.isRunning()) {
        LOGGER.info("Stopping sound line: {}", targetDataLine);
        targetDataLine.stop();
      }
      if (targetDataLine.isOpen()) {
        LOGGER.info("Closing sound line: {}", this);
        targetDataLine.close();
      }
      LOGGER.info("Close sound line completed: {}", this);
    }

    @Override
    public String toString() {
      return this.name;
    }

    @NonNull
    public Mixer getMixer() {
      return this.mixer;
    }

    @NonNull
    public Line getLine() {
      return this.line;
    }

    @NonNull
    public String getName() {
      return this.name;
    }

    @Override
    public int hashCode() {
      return this.uuid.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      return obj instanceof SoundPort ? ((SoundPort) obj).uuid.equals(this.uuid) : false;
    }

  }

  public JavaSoundAdapter() {

  }

  @NonNull
  public Optional<SoundPort> findSoundPort(@NonNull final String name) {
    return this.foundPorts.stream().filter(x -> x.getName().equals(name)).findFirst();
  }

  public List<SoundPort> getAvailableSoundPorts() {
    return Collections.unmodifiableList(this.foundPorts);
  }

  @PostConstruct
  public void post() {
    final Mixer.Info[] mixers = AudioSystem.getMixerInfo();
    LOGGER.info("Found sound mixers:");
    int counter = 1;
    for (final Mixer.Info mixerInfo : mixers) {
      LOGGER.info(String.format(" %d: %s (vendor=%s, version=%s, description=%s)", counter++, mixerInfo.getName(), mixerInfo.getVendor(), mixerInfo.getVersion(), mixerInfo.getDescription()));
      try (Mixer mixer = AudioSystem.getMixer(mixerInfo)) {
        try {
          mixer.open();
        } catch (LineUnavailableException ex) {
          LOGGER.error("Can't open mixer {}: {}", mixer, ex.getMessage());
          continue;
        }
        final Line.Info[] targetLineInfo = mixer.getTargetLineInfo();
        int lineCounter = 1;
        for (final Line.Info lineInfo : targetLineInfo) {
          try {
            final Line line = mixer.getLine(lineInfo);
            if (line instanceof TargetDataLine) {
              LOGGER.info(String.format("  %d: %s, %s ", lineCounter++, lineInfo.getLineClass().getSimpleName(), line.getLineInfo()));
              this.foundPorts.add(new SoundPort(mixer, mixerInfo.getName() + ':' + line.getLineInfo().toString(), line));
            }
          } catch (LineUnavailableException ex) {
            LOGGER.error("Line unavailable: {}", lineInfo);
          }
        }
      }
    }
  }

}
