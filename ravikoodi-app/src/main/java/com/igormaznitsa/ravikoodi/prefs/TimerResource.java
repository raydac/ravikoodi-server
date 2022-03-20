/*
 * Copyright 2022 Igor Maznitsa.
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
package com.igormaznitsa.ravikoodi.prefs;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class TimerResource implements Comparable<TimerResource> {
    
    @NonNull
    public static TimerResource fromBase64(@NonNull final String base64encoded) throws IOException {
        final Properties properties = new Properties();
        properties.load(new StringReader(new String(Base64.getDecoder().decode(base64encoded), StandardCharsets.UTF_8)));
        if (!properties.containsKey("name")) {
            throw new IllegalArgumentException("Can't find 'name' field");
        }
        final TimerResource result = new TimerResource(properties.getProperty("name"));
        result.setEnabled(Boolean.parseBoolean(properties.getProperty("enabled")));
        result.setReplay(Boolean.parseBoolean(properties.getProperty("replay", "false")));
        if (properties.containsKey("resource")) {
            result.setResourcePath(new File(properties.getProperty("resource")));
        }
        if (properties.containsKey("from")) {
            result.setFrom(LocalTime.parse(properties.getProperty("from")));
        }
        if (properties.containsKey("to")) {
            result.setTo(LocalTime.parse(properties.getProperty("to")));
        }
        return result;
    }
    private String name;
    private boolean enabled;
    private boolean replay;
    private LocalTime from;
    private LocalTime to;
    private File resourcePath;

    public TimerResource(@NonNull final String id) {
        this.name = Objects.requireNonNull(id);
        this.enabled = false;
        this.replay = false;
        this.from = null;
        this.to = null;
        this.resourcePath = null;
    }

    public boolean isReplay() {
        return this.replay;
    }

    public void setReplay(final boolean value) {
        this.replay = value;
    }

    @NonNull
    public String getName() {
        return this.name;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean value) {
        this.enabled = value;
    }

    @Nullable
    public LocalTime getFrom() {
        return this.from;
    }

    public void setFrom(@Nullable final LocalTime time) {
        this.from = time;
    }

    @Nullable
    public LocalTime getTo() {
        return this.to;
    }

    public void setTo(@Nullable final LocalTime to) {
        this.to = to;
    }

    @Nullable
    public File getResourcePath() {
        return this.resourcePath;
    }

    public void setResourcePath(@Nullable final File path) {
        this.resourcePath = path;
    }

    @NonNull
    public String toBase64() {
        final Properties properties = new Properties();
        properties.setProperty("name", this.name);
        properties.setProperty("enabled", Boolean.toString(this.enabled));
        properties.setProperty("replay", Boolean.toString(this.replay));
        if (this.from != null) {
            properties.setProperty("from", this.from.toString());
        }
        if (this.to != null) {
            properties.setProperty("to", this.to.toString());
        }
        if (this.resourcePath != null) {
            properties.setProperty("resource", this.resourcePath.getAbsolutePath());
        }
        try {
            final StringWriter writer = new StringWriter();
            properties.store(writer, null);
            return Base64.getEncoder().encodeToString(writer.toString().getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new Error("Unexpected IOException in internal operation", ex);
        }
    }

    public void setName(@NonNull String value) {
        this.name = Objects.requireNonNull(value);
    }

    @Override
    public int compareTo(@NonNull final TimerResource that) {
        if (this.from == null) {
            return -1;
        }
        if (that.from == null) {
            return -1;
        }
        return this.from.isBefore(that.from) ? -1 : 1;
    }
    
}
