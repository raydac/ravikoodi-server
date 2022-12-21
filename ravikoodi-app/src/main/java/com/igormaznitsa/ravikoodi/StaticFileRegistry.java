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
package com.igormaznitsa.ravikoodi;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class StaticFileRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(StaticFileRegistry.class);

    @Autowired
    private MimeTypes mimeTypes;

    @Autowired
    private ApplicationPreferences applicationPreferences;

    private final Map<String, UploadFileRecord> records = new ConcurrentHashMap<>();

    public StaticFileRegistry() {

    }

    @PostConstruct
    public void onPostConstruct() {
        this.refresh();
    }
    
    public Optional<UploadFileRecord> findFile(@NonNull final String uid) {
        return Optional.ofNullable(this.records.get(uid));
    }

    public UploadFileRecord registerFile(@NonNull final String uid, @NonNull final Path file, @Nullable final byte[] data) {
        LOGGER.info("Registering file {} as static resource: {}", file, uid);
        final UploadFileRecord newRecord = new UploadFileRecord(uid, file, this.mimeTypes.findMimeTypeForFile(file), data);
        this.records.put(uid, newRecord);
        return newRecord;
    }

    public void unregisterFile(final String uid, final boolean totally) {
        LOGGER.info("Unregistering file {}, totally={}", uid, totally);
        this.records.remove(uid);
    }

    public void clear() {
        this.records.clear();
    }

    public synchronized void refresh() {
        LOGGER.info("Refreshing static resources");
        this.records.clear();
        this.applicationPreferences.getStaticResources()
                .stream()
                .filter(r -> r.isEnabled() && r.getResourcePath() != null)
                .forEach(r -> {
            try {
                final Path path = r.getResourcePath().toPath();
                final UploadFileRecord record = new UploadFileRecord(r.getId(), path, this.mimeTypes.findMimeTypeForFile(path), null);
                this.records.put(r.getId(), record);
                LOGGER.info("Registered static resource {}: {}", r.getId(), record);
            } catch (Exception ex) {
                LOGGER.error("Error during registration static resource: {}", r, ex);
            }
        });
    }

}
