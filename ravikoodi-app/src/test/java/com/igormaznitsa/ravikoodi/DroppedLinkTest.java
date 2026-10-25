package com.igormaznitsa.ravikoodi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.junit.Test;

public class DroppedLinkTest {

  @Test
  public void testPlainYoutubeUrl() {
    assertEquals("https://youtu.be/abc",
        DroppedLink.findUrlInText("https://youtu.be/abc").orElseThrow());
  }

  @Test
  public void testUriListSkipsComments() {
    final String uriList = """
        # comment
        https://www.youtube.com/watch?v=abc
        https://example.com/other
        """;

    assertEquals("https://www.youtube.com/watch?v=abc",
        DroppedLink.findUrlInText(uriList).orElseThrow());
  }

  @Test
  public void testHtmlHref() {
    final String html = "<a href=\"https://www.youtube.com/watch?v=abc\">clip</a>";

    assertEquals("https://www.youtube.com/watch?v=abc",
        DroppedLink.findUrlInText(html).orElseThrow());
  }

  @Test
  public void testMozillaUrlUsesFirstLine() {
    assertEquals("https://youtu.be/abc",
        DroppedLink.findUrlInText("https://youtu.be/abc\nTitle").orElseThrow());
  }

  @Test
  public void testHostWithoutScheme() {
    assertEquals("https://youtu.be/abc", DroppedLink.findUrlInText("youtu.be/abc").orElseThrow());
  }

  @Test
  public void testUtf16LinkBytes() throws Exception {
    final byte[] bytes = "https://youtu.be/abc\nTitle".getBytes(StandardCharsets.UTF_16LE);
    final Transferable transferable = new StreamTransferable(DroppedLink.MOZ_URL, bytes);

    final Optional<String> url = DroppedLink.findUrl(transferable);

    assertEquals("https://youtu.be/abc", url.orElseThrow());
  }

  @Test
  public void testEmptyText() {
    assertFalse(DroppedLink.findUrlInText("   ").isPresent());
  }

  private static final class StreamTransferable implements Transferable {
    private final DataFlavor flavor;
    private final byte[] bytes;

    private StreamTransferable(final DataFlavor flavor, final byte[] bytes) {
      this.flavor = flavor;
      this.bytes = bytes;
    }

    @Override
    public DataFlavor[] getTransferDataFlavors() {
      return new DataFlavor[] {this.flavor};
    }

    @Override
    public boolean isDataFlavorSupported(final DataFlavor flavor) {
      return this.flavor.equals(flavor);
    }

    @Override
    public Object getTransferData(final DataFlavor flavor) throws UnsupportedFlavorException {
      if (!this.isDataFlavorSupported(flavor)) {
        throw new UnsupportedFlavorException(flavor);
      }
      return new ByteArrayInputStream(this.bytes);
    }
  }
}
