package com.igormaznitsa.ravikoodi;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class UploadingFileRegistry {

  private static final Logger LOGGER = LoggerFactory.getLogger(UploadingFileRegistry.class);

  private static final long INITIAL_VALID_DELAY_MILLISECONDS = 15000L;
  private static final long VALID_TIME_MILLISECONDS = 5000L;
  private static final long CHECK_DELAY_MILLISECONDS = 3000L;

  @Autowired
  private MimeTypes mimeTypes;

  @Autowired
  private ScheduledExecutorService executor;

  private final AtomicBoolean paused = new AtomicBoolean();

  private final AtomicReference<Map<UUID,FileRecord>> removedFileRecordStore = new AtomicReference<>();
  
  public static final class FileRecord {

    private final UUID uuid;
    private final Path file;
    private final String mimeType;
    private final AtomicInteger uploadsCounter = new AtomicInteger();

    private final AtomicLong validUntil = new AtomicLong();
    private final byte[] predefinedData;

    FileRecord(@NonNull final UUID uuid, @NonNull final Path file, @NonNull final String mimeType, @Nullable final byte[] predefinedData) {
      this.validUntil.set(System.currentTimeMillis() + INITIAL_VALID_DELAY_MILLISECONDS);
      this.uuid = uuid;
      this.file = file;
      this.mimeType = mimeType;
      this.predefinedData = predefinedData;
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
      this.validUntil.set(System.currentTimeMillis() + VALID_TIME_MILLISECONDS);
    }

    public int incUploadsCounter() {
      this.validUntil.set(System.currentTimeMillis() + VALID_TIME_MILLISECONDS);
      return this.uploadsCounter.incrementAndGet();
    }

    public int decUploadsCounter() {
      this.validUntil.set(System.currentTimeMillis() + VALID_TIME_MILLISECONDS);
      return this.uploadsCounter.decrementAndGet();
    }

    @NonNull
    public String getMimeType() {
      return this.mimeType;
    }

    @NonNull
    public UUID getUUID() {
      return this.uuid;
    }

    @NonNull
    public Path getFile() {
      return this.file;
    }
  
    @NonNull
    @Override
    public String toString() {
      return String.format("FileRecord(%s,file=%s)", this.uuid, this.file.getFileName().toString());
    }
  }

  private final Map<UUID, FileRecord> records = new ConcurrentHashMap<>();

  public void setRemovedRecordsStore(final Map<UUID, FileRecord> store) {
    this.removedFileRecordStore.set(store);
  }
  
  @PostConstruct
  public void postConstruct() {
    this.executor.scheduleWithFixedDelay(() -> this.collectTimeoutFiles(), CHECK_DELAY_MILLISECONDS, CHECK_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS);
  }

  public void pause() {
    if (this.paused.compareAndSet(false, true)) {
      LOGGER.info("Component work paused");
    }
  }

  public void resume() {
    if (this.paused.compareAndSet(true, false)) {
      LOGGER.info("Component work resumed");
    }
  }

  private void collectTimeoutFiles() {
    if (this.paused.get()) {
      this.records.entrySet().forEach(x -> x.getValue().refreshValidTime());
    } else {
      final long time = System.currentTimeMillis();
      final List<UUID> listOfTimeOut = this.records.entrySet()
              .stream()
              .filter(x -> x.getValue().getUploadsCounter() == 0 && x.getValue().validUntil.get() < time)
              .map(x -> x.getKey())
              .collect(Collectors.toList());

      final Map<UUID, FileRecord> removedRecordsStore = this.removedFileRecordStore.get();
      listOfTimeOut.forEach(x -> {
        LOGGER.info("Collected file registry record '{}'", x);
        if (removedRecordsStore!=null) {
          final FileRecord removedRecord = this.records.remove(x);
          if (removedRecord != null) {
            removedRecordsStore.put(removedRecord.uuid, removedRecord);
          }
        }
      });
    }
  }

  @Nullable
  public FileRecord restoreRecord(@NonNull final FileRecord record) {
    LOGGER.info("Restoring record: {}", record);
    FileRecord result = null;
    if (this.records.putIfAbsent(record.getUUID(), record) == null) {
      LOGGER.info("Record {} has been restored", record.getUUID());
      record.refreshValidTime();
      result = record;
    }
    return result;
  }
  
  public FileRecord registerFile(@NonNull final UUID uuid, @NonNull final Path file, @Nullable final byte[] data) {
    final FileRecord newRecord = new FileRecord(uuid, file, this.mimeTypes.findMimeTypeForFile(file), data);
    this.records.put(uuid, newRecord);
    return newRecord;
  }

  public boolean isFileAtPlay(@NonNull final Path path) {
    return this.records.values().stream().anyMatch((record) -> (record.file.equals(path)));
  }

  public void unregisterFile(final UUID uuid, final boolean totally) {
      LOGGER.info("Unregistering file {}, totally={}", uuid, totally);
      this.records.remove(uuid);
      if (totally) {
          final Map<UUID, FileRecord> fileRecordStore = this.removedFileRecordStore.get();
          if (fileRecordStore!=null){
            fileRecordStore.remove(uuid);
          }
      }
  }

  public void clear() {
    this.records.clear();
  }

  @Nullable
  public FileRecord find(@NonNull final UUID uuid) {
    return this.records.get(uuid);
  }

  public boolean hasActiveUploads() {
    return !this.records.isEmpty() && this.records.values().stream().anyMatch(x -> x.getUploadsCounter() > 0);
  }
}
