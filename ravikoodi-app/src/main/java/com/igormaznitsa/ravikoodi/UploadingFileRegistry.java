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

  public static final long INITIAL_VALID_DELAY_MILLISECONDS = 15000L;
  public static final long VALID_TIME_MILLISECONDS = 5000L;
  public static final long CHECK_DELAY_MILLISECONDS = 3000L;

  @Autowired
  private MimeTypes mimeTypes;

  @Autowired
  private ScheduledExecutorService executor;

  private final AtomicBoolean paused = new AtomicBoolean();

  private final AtomicReference<Map<String,UploadFileRecord>> removedFileRecordStore = new AtomicReference<>();
  
  private final Map<String, UploadFileRecord> records = new ConcurrentHashMap<>();

  public void setRemovedRecordsStore(final Map<String, UploadFileRecord> store) {
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
      final List<String> listOfTimeOut = this.records.entrySet()
              .stream()
              .filter(x -> x.getValue().getUploadsCounter() == 0 && x.getValue().getValidUntil() < time)
              .map(x -> x.getKey())
              .collect(Collectors.toList());

      final Map<String, UploadFileRecord> removedRecordsStore = this.removedFileRecordStore.get();
      listOfTimeOut.forEach(x -> {
        LOGGER.info("Collected file registry record '{}'", x);
        if (removedRecordsStore!=null) {
          final UploadFileRecord removedRecord = this.records.remove(x);
          if (removedRecord != null) {
            removedRecordsStore.put(removedRecord.getUid(), removedRecord);
          }
        }
      });
    }
  }

  @Nullable
  public UploadFileRecord restoreRecord(@NonNull final UploadFileRecord record) {
    LOGGER.info("Restoring record: {}", record);
    UploadFileRecord result = null;
    if (this.records.putIfAbsent(record.getUid(), record) == null) {
      LOGGER.info("Record {} has been restored", record.getUid());
      record.refreshValidTime();
      result = record;
    }
    return result;
  }
  
  public UploadFileRecord registerFile(@NonNull final String uid, @NonNull final Path file, @Nullable final byte[] data) {
    final UploadFileRecord newRecord = new UploadFileRecord(uid, file, this.mimeTypes.findMimeTypeForFile(file), data);
    this.records.put(uid, newRecord);
    return newRecord;
  }

  public boolean isFileAtPlay(@NonNull final Path path) {
    return this.records.values().stream().anyMatch((record) -> (record.getFile().equals(path)));
  }

  public void unregisterFile(final String uid, final boolean totally) {
      LOGGER.info("Unregistering file {}, totally={}", uid, totally);
      this.records.remove(uid);
      if (totally) {
          final Map<String, UploadFileRecord> fileRecordStore = this.removedFileRecordStore.get();
          if (fileRecordStore!=null){
            fileRecordStore.remove(uid);
          }
      }
  }

  public void clear() {
    this.records.clear();
  }

  @Nullable
  public UploadFileRecord find(@NonNull final String uid) {
    return this.records.get(uid);
  }

  public boolean hasActiveUploads() {
    return !this.records.isEmpty() && this.records.values().stream().anyMatch(x -> x.getUploadsCounter() > 0);
  }
}
