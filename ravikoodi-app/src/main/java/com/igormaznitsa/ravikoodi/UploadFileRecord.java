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

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public final class UploadFileRecord {
    
    private final String id;
    private final Path file;
    private final String mimeType;
    private final AtomicInteger uploadsCounter = new AtomicInteger();
    private final AtomicLong validUntil = new AtomicLong();
    private final byte[] predefinedData;

    UploadFileRecord(@NonNull final String id, @NonNull final Path file, @NonNull final String mimeType, @Nullable final byte[] predefinedData) {
        this.validUntil.set(System.currentTimeMillis() + UploadingFileRegistry.INITIAL_VALID_DELAY_MILLISECONDS);
        this.id = id;
        this.file = file;
        this.mimeType = mimeType;
        this.predefinedData = predefinedData;
    }

    public long getValidUntil(){
        return this.validUntil.get();
    }
    
    public void setValidUntil(final long time){
        this.validUntil.set(time);
    }
    
    public InputStream getAsInputStream() throws IOException {
        if (this.predefinedData == null) {
            final long length = Files.size(this.file);
            return new BufferedInputStream(new FileInputStream(this.file.toFile()), (int) Math.min(262144L, length));
        } else {
            return new ByteArrayInputStream(this.predefinedData);
        }
    }

    public Optional<byte[]> getPredefinedData() {
        return Optional.ofNullable(this.predefinedData);
    }

    public int getUploadsCounter() {
        return this.uploadsCounter.get();
    }

    public void refreshValidTime() {
        this.validUntil.set(System.currentTimeMillis() + UploadingFileRegistry.VALID_TIME_MILLISECONDS);
    }

    public int incUploadsCounter() {
        this.validUntil.set(System.currentTimeMillis() + UploadingFileRegistry.VALID_TIME_MILLISECONDS);
        return this.uploadsCounter.incrementAndGet();
    }

    public int decUploadsCounter() {
        this.validUntil.set(System.currentTimeMillis() + UploadingFileRegistry.VALID_TIME_MILLISECONDS);
        return this.uploadsCounter.decrementAndGet();
    }

    @NonNull
    public String getMimeType() {
        return this.mimeType;
    }

    @NonNull
    public String getId() {
        return this.id;
    }

    @NonNull
    public Path getFile() {
        return this.file;
    }

    @NonNull
    @Override
    public String toString() {
        return String.format("FileRecord(%s,file=%s)", this.id, this.file.getFileName().toString());
    }
    
}
