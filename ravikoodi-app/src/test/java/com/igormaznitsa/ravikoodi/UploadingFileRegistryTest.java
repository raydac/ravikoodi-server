package com.igormaznitsa.ravikoodi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.Test;

public class UploadingFileRegistryTest {

  @Test
  public void parkedRecordLeavesAfterItsKeepWindow() {
    final Map<String, UploadFileRecord> parked = new ConcurrentHashMap<>();
    final UploadFileRecord record =
        new UploadFileRecord("clip", Path.of("clip.png"), "image/png", new byte[] {1, 2, 3});
    UploadingFileRegistry.parkRecord(parked, record, 1_000L);

    assertEquals(1_000L + UploadingFileRegistry.PARKED_KEEP_MILLISECONDS, record.getValidUntil());
    UploadingFileRegistry.dropStaleParkedRecords(parked, record.getValidUntil());
    assertEquals(1, parked.size());

    UploadingFileRegistry.dropStaleParkedRecords(parked, record.getValidUntil() + 1L);
    assertTrue(parked.isEmpty());
  }

  @Test
  public void parkedLotKeepsTheNewestRecords() {
    final Map<String, UploadFileRecord> parked = new ConcurrentHashMap<>();
    final int extra = 5;
    for (int index = 0; index < UploadingFileRegistry.MAX_PARKED_RECORDS + extra; index++) {
      UploadingFileRegistry.parkRecord(
          parked,
          new UploadFileRecord("id-" + index, Path.of("file"), "video/mp4", null),
          index);
    }

    assertEquals(UploadingFileRegistry.MAX_PARKED_RECORDS, parked.size());
    assertFalse(parked.containsKey("id-0"));
    assertTrue(parked.containsKey("id-" + (UploadingFileRegistry.MAX_PARKED_RECORDS + extra - 1)));
  }
}
