package com.igormaznitsa.ravikoodi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.igormaznitsa.ravikoodi.kodijsonapi.PlaylistFileItem;
import org.junit.Test;

public class PlayerPanelItemTest {

  private static PlaylistFileItem item(final String file, final String label, final String type) {
    final PlaylistFileItem item = new PlaylistFileItem();
    item.setFile(file);
    item.setLabel(label);
    item.setType(type);
    return item;
  }

  @Test
  public void testTitleUsesLabel() {
    final PlaylistFileItem item = item("https://example/a", "First", "video");

    assertEquals("First", PlayerPanel.playingItemTitle(item));
  }

  @Test
  public void testTitleFallsBackToFileName() {
    final PlaylistFileItem item = item("https://cdn.example/watch/clip.mp4", "", "video");

    assertEquals("clip.mp4", PlayerPanel.playingItemTitle(item));
  }

  @Test
  public void testDifferentLinkIsDifferentItem() {
    final String first = PlayerPanel.playingItemKey(item("https://example/a", "First", "video"));
    final String second = PlayerPanel.playingItemKey(item("https://example/b", "Second", "video"));

    assertNotEquals(first, second);
  }

  @Test
  public void testLabelArrivalChangesIdentity() {
    final String unnamed = PlayerPanel.playingItemKey(item("https://example/a", "", "video"));
    final String named = PlayerPanel.playingItemKey(item("https://example/a", "Concert", "video"));

    assertNotEquals(unnamed, named);
    assertEquals("Concert",
        PlayerPanel.playingItemTitle(item("https://example/a", "Concert", "video")));
  }
}
