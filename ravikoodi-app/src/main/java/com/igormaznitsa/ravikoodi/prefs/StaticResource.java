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
import java.util.Base64;
import java.util.Objects;
import java.util.Properties;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class StaticResource implements Comparable<StaticResource> {
    
    @NonNull
    public static StaticResource fromBase64(@NonNull final String base64encoded) throws IOException {
        final Properties properties = new Properties();
        properties.load(new StringReader(new String(Base64.getDecoder().decode(base64encoded), StandardCharsets.UTF_8)));
        if (!properties.containsKey("id")) {
            throw new IllegalArgumentException("Can't find 'id' field");
        }
        final StaticResource result = new StaticResource(properties.getProperty("id"));
        result.setEnabled(Boolean.parseBoolean(properties.getProperty("enabled")));
        if (properties.containsKey("resource")) {
            result.setResourcePath(new File(properties.getProperty("resource")));
        }
        return result;
    }
    private String id;
    private boolean enabled;
    private File resourcePath;

    public StaticResource(@NonNull final String id) {
        this.id = Objects.requireNonNull(id);
        this.enabled = false;
        this.resourcePath = null;
    }

    @NonNull
    public String getId() {
        return this.id;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(final boolean value) {
        this.enabled = value;
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
        properties.setProperty("id", this.id);
        properties.setProperty("enabled", Boolean.toString(this.enabled));
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

    public void setId(@NonNull String id) {
        this.id = Objects.requireNonNull(id);
    }

    @Override
    public int compareTo(@NonNull final StaticResource that) {
        return this.id.compareTo(that.id);
    }
    
}
