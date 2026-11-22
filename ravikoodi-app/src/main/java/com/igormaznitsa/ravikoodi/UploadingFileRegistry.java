package com.igormaznitsa.ravikoodi;

import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class UploadingFileRegistry {

  public static final long INITIAL_VALID_DELAY_MILLISECONDS = 15000L;
  public static final long VALID_TIME_MILLISECONDS = 5000L;
  public static final long CHECK_DELAY_MILLISECONDS = 3000L;
  public static final long PARKED_KEEP_MILLISECONDS = TimeUnit.MINUTES.toMillis(30);
  public static final int MAX_PARKED_RECORDS = 32;
  private static final Logger LOGGER = LoggerFactory.getLogger(UploadingFileRegistry.class);
  private final AtomicBoolean paused = new AtomicBoolean();
  private final AtomicReference<Map<String, UploadFileRecord>> removedFileRecordStore =
      new AtomicReference<>();
  private final Map<String, UploadFileRecord> records = new ConcurrentHashMap<>();
  @Autowired
  private MimeTypes mimeTypes;
  @Autowired
  private ScheduledExecutorService executor;

  public void setRemovedRecordsStore(final Map<String, UploadFileRecord> store) {
    this.removedFileRecordStore.set(store);
  }

  @PostConstruct
  public void postConstruct() {
    this.executor.scheduleWithFixedDelay(this::collectTimeoutFiles, CHECK_DELAY_MILLISECONDS,
        CHECK_DELAY_MILLISECONDS, TimeUnit.MILLISECONDS);
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

  static void parkRecord(final Map<String, UploadFileRecord> parked,
                         final UploadFileRecord record, final long now) {
    record.setValidUntil(now + PARKED_KEEP_MILLISECONDS);
    parked.put(record.getId(), record);
    trimParkedRecords(parked);
  }

  private boolean isExpired(final UploadFileRecord record, final long now) {
    return record.getUploadsCounter() == 0 && record.getValidUntil() < now;
  }

  static void dropStaleParkedRecords(final Map<String, UploadFileRecord> parked, final long now) {
    if (parked == null || parked.isEmpty()) {
      return;
    }
    parked.entrySet().removeIf(entry -> entry.getValue().getValidUntil() < now);
    trimParkedRecords(parked);
  }

  private static void trimParkedRecords(final Map<String, UploadFileRecord> parked) {
    final int overflow = parked.size() - MAX_PARKED_RECORDS;
    if (overflow <= 0) {
      return;
    }
    parked.entrySet().stream()
        .sorted(Comparator.comparingLong(entry -> entry.getValue().getValidUntil()))
        .limit(overflow)
        .map(Map.Entry::getKey)
        .toList()
        .forEach(parked::remove);
  }

  private void collectTimeoutFiles() {
    final long now = System.currentTimeMillis();
    dropStaleParkedRecords(this.removedFileRecordStore.get(), now);
    if (this.paused.get()) {
      this.records.forEach((key, value) -> value.refreshValidTime());
      return;
    }

    final Map<String, UploadFileRecord> removedRecordsStore = this.removedFileRecordStore.get();
    final List<String> expiredIds = this.records.entrySet()
        .stream()
        .filter(entry -> this.isExpired(entry.getValue(), now))
        .map(Map.Entry::getKey)
        .toList();

    expiredIds.forEach(id -> this.records.compute(id,
        (key, record) -> this.collectIfExpired(removedRecordsStore, now, key, record)));
  }

  private UploadFileRecord collectIfExpired(final Map<String, UploadFileRecord> removedRecords,
                                            final long now, final String id,
                                            final UploadFileRecord record) {
    if (record == null || removedRecords == null || !this.isExpired(record, now)) {
      return record;
    }

    LOGGER.info("Collected file registry record '{}'", id);
    parkRecord(removedRecords, record, now);
    return null;
  }

  @Nullable
  public UploadFileRecord restoreRecord(@NonNull final UploadFileRecord record) {
    final UploadFileRecord alreadyPresent = this.records.putIfAbsent(record.getId(), record);
    if (alreadyPresent != null) {
      return alreadyPresent;
    }

    LOGGER.info("Record {} has been restored", record);
    record.refreshValidTime();
    return record;
  }

  public UploadFileRecord registerFile(@NonNull final String uid, @NonNull final Path file,
                                       @Nullable final byte[] data) {
    final UploadFileRecord newRecord =
        new UploadFileRecord(uid, file, this.mimeTypes.findMimeTypeForFile(file), data);
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
      if (fileRecordStore != null) {
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
    return !this.records.isEmpty() &&
        this.records.values().stream().anyMatch(x -> x.getUploadsCounter() > 0);
  }
}
