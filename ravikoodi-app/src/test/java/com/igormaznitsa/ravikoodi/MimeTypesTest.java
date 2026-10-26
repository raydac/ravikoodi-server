/*
 * Copyright 2026 Igor Maznitsa.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import javax.swing.filechooser.FileFilter;
import org.junit.Test;

public class MimeTypesTest {

  private final MimeTypes mimeTypes = new MimeTypes();

  @Test
  public void testOpenDialogFiltersUseShortLabels() {
    final List<FileFilter> filters = this.mimeTypes.makeOpenFileFilters();

    assertEquals("Video", filters.get(0).getDescription());
    assertEquals("Audio", filters.get(1).getDescription());
    assertEquals("Picture", filters.get(2).getDescription());
    assertTrue(filters.stream().allMatch(filter -> filter.getDescription().length() <= 40));
    assertTrue(filters.stream().anyMatch(filter -> "MP4 (*.mp4)".equals(filter.getDescription())));
    assertTrue(filters.stream()
        .anyMatch(filter -> "JPG (*.jpg, *.jpeg)".equals(filter.getDescription())));
    assertFalse(filters.stream().anyMatch(filter ->
        filter.getDescription().contains("*.mpg") && filter.getDescription().contains("*.mp4")));
  }

  @Test
  public void testCategoryFilterAcceptsItsExtensionsOnly() {
    final FileFilter video = this.mimeTypes.makeOpenFileFilters().get(0);
    final FileFilter mp4 = this.mimeTypes.makeOpenFileFilters().stream()
        .filter(filter -> "MP4 (*.mp4)".equals(filter.getDescription()))
        .findFirst()
        .orElseThrow();

    assertTrue(video.accept(new File(".")));
    assertTrue(video.accept(new File("movie.mp4")));
    assertTrue(video.accept(new File("movie.MKV")));
    assertFalse(video.accept(new File("song.mp3")));
    assertTrue(mp4.accept(new File("movie.mp4")));
    assertFalse(mp4.accept(new File("movie.mkv")));
  }
}
