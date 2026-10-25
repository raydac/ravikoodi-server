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
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

  public long getPayloadSize() throws IOException {
    if (this.predefinedData != null) {
      return this.predefinedData.length;
    }
    return Files.size(this.file);
  }

    public InputStream getAsInputStream() throws IOException {
      return this.openFrom(0L);
    }

  public InputStream openFrom(final long offset) throws IOException {
    if (offset < 0L) {
      throw new IllegalArgumentException("offset must not be negative");
    }
    if (this.predefinedData != null) {
      return this.openPredefined(offset);
    }
    return this.openFile(offset);
  }

  private InputStream openPredefined(final long offset) throws EOFException {
    if (offset > this.predefinedData.length) {
      throw new EOFException("offset past payload");
    }
    final ByteArrayInputStream in = new ByteArrayInputStream(this.predefinedData);
    if (offset > 0L && in.skip(offset) != offset) {
      throw new EOFException("offset past payload");
    }
    return in;
  }

  private InputStream openFile(final long offset) throws IOException {
    final FileChannel channel = FileChannel.open(this.file, StandardOpenOption.READ);
    boolean transferred = false;
    try {
      if (offset > channel.size()) {
        throw new EOFException("offset past payload");
      }
      channel.position(offset);
      final int bufferSize = (int) Math.min(262144L, Math.max(1L, channel.size() - offset));
      final InputStream stream =
          new BufferedInputStream(Channels.newInputStream(channel), bufferSize);
      transferred = true;
      return stream;
    } finally {
      if (!transferred) {
        channel.close();
      }
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
