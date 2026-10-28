package com.igormaznitsa.ravikoodi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class InternalServerRangeTest {

  private static final AtomicReference<UploadFileRecord> current = new AtomicReference<>();
  private static HttpServer server;
  private static Path directory;

  @BeforeClass
  public static void start() throws Exception {
    directory = Files.createTempDirectory("ravikoodi-range");
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      try {
        InternalServer.respondWithPayload(exchange, current.get());
      } finally {
        exchange.close();
      }
    });
    server.start();
  }

  @AfterClass
  public static void stop() throws Exception {
    if (server != null) {
      server.stop(0);
    }
    if (directory != null) {
      try (Stream<Path> walk = Files.walk(directory)) {
        walk.sorted(Comparator.reverseOrder()).forEach(InternalServerRangeTest::deleteQuietly);
      }
    }
  }

  private static UploadFileRecord fileRecord(final byte[] bytes) throws IOException {
    final Path file = directory.resolve("clip-" + System.nanoTime() + ".mp4");
    Files.write(file, bytes);
    return new UploadFileRecord("clip", file, "video/mp4", null);
  }

  private static byte[] payload(final int size) {
    final byte[] bytes = new byte[size];
    for (int i = 0; i < size; i++) {
      bytes[i] = (byte) i;
    }
    return bytes;
  }

  private static Response request(final String method, final String range) throws IOException {
    final HttpURLConnection connection = (HttpURLConnection) URI.create(
            "http://127.0.0.1:" + server.getAddress().getPort() + "/vfile/clip/clip.mp4").toURL()
        .openConnection();
    connection.setRequestMethod(method);
    connection.setConnectTimeout(2000);
    connection.setReadTimeout(2000);
    if (range != null) {
      connection.setRequestProperty("Range", range);
    }

    try {
      final int status = connection.getResponseCode();
      return new Response(
          status,
          connection.getHeaderField("Content-Length"),
          connection.getHeaderField("Content-Range"),
          connection.getHeaderField("Accept-Ranges"),
          readBody(connection, status));
    } finally {
      connection.disconnect();
    }
  }

  private static byte[] readBody(final HttpURLConnection connection, final int status)
      throws IOException {
    final InputStream in =
        status >= 400 ? connection.getErrorStream() : connection.getInputStream();
    if (in == null) {
      return new byte[0];
    }
    try (InputStream stream = in) {
      return stream.readAllBytes();
    }
  }

  private static void deleteQuietly(final Path path) {
    try {
      Files.deleteIfExists(path);
    } catch (final IOException ex) {
      throw new java.io.UncheckedIOException(ex);
    }
  }

  @Test
  public void testFullGet() throws Exception {
    final byte[] payload = payload(100);
    current.set(fileRecord(payload));

    final Response response = request("GET", null);

    assertEquals(200, response.status);
    assertEquals("100", response.contentLength);
    assertNull(response.contentRange);
    assertEquals("bytes", response.acceptRanges);
    assertArrayEquals(payload, response.body);
  }

  @Test
  public void testSeekFromOffset() throws Exception {
    final byte[] payload = payload(100);
    current.set(fileRecord(payload));

    final Response response = request("GET", "bytes=40-");

    assertEquals(206, response.status);
    assertEquals("60", response.contentLength);
    assertEquals("bytes 40-99/100", response.contentRange);
    assertArrayEquals(Arrays.copyOfRange(payload, 40, 100), response.body);
  }

  @Test
  public void testClosedRange() throws Exception {
    final byte[] payload = payload(100);
    current.set(fileRecord(payload));

    final Response response = request("GET", "bytes=10-19");

    assertEquals(206, response.status);
    assertEquals("10", response.contentLength);
    assertEquals("bytes 10-19/100", response.contentRange);
    assertEquals(response.body.length, Integer.parseInt(response.contentLength));
    assertArrayEquals(Arrays.copyOfRange(payload, 10, 20), response.body);
  }

  @Test
  public void testEndPastEofIsClamped() throws Exception {
    final byte[] payload = payload(100);
    current.set(fileRecord(payload));

    final Response response = request("GET", "bytes=90-99999");

    assertEquals(206, response.status);
    assertEquals("10", response.contentLength);
    assertEquals("bytes 90-99/100", response.contentRange);
    assertArrayEquals(Arrays.copyOfRange(payload, 90, 100), response.body);
  }

  @Test
  public void testEndEqualToSizeDoesNotReadPastEof() throws Exception {
    final byte[] payload = payload(100);
    current.set(fileRecord(payload));

    final Response response = request("GET", "bytes=0-100");

    assertEquals(200, response.status);
    assertEquals("100", response.contentLength);
    assertArrayEquals(payload, response.body);
  }

  @Test
  public void testSuffixRange() throws Exception {
    final byte[] payload = payload(100);
    current.set(fileRecord(payload));

    final Response response = request("GET", "bytes=-5");

    assertEquals(206, response.status);
    assertEquals("5", response.contentLength);
    assertEquals("bytes 95-99/100", response.contentRange);
    assertArrayEquals(Arrays.copyOfRange(payload, 95, 100), response.body);
  }

  @Test
  public void testUnsatisfiableRange() throws Exception {
    current.set(fileRecord(payload(100)));

    final Response response = request("GET", "bytes=100-");

    assertEquals(416, response.status);
    assertEquals("bytes */100", response.contentRange);
    assertEquals(0, response.body.length);
  }

  @Test
  public void testHeadReportsSelectedLength() throws Exception {
    current.set(fileRecord(payload(100)));

    final Response response = request("HEAD", "bytes=10-19");

    assertEquals(206, response.status);
    assertEquals("10", response.contentLength);
    assertEquals("bytes 10-19/100", response.contentRange);
    assertEquals(0, response.body.length);
  }

  @Test
  public void testInMemoryPayloadIgnoresMissingFileLength() throws Exception {
    final byte[] data = "abcdefgh".getBytes(UTF_8);
    current.set(new UploadFileRecord("mem", Path.of("missing-clipboard.png"), "image/png", data));

    final Response full = request("GET", null);
    assertEquals(200, full.status);
    assertEquals("8", full.contentLength);
    assertArrayEquals(data, full.body);

    final Response partial = request("GET", "bytes=2-4");
    assertEquals(206, partial.status);
    assertEquals("3", partial.contentLength);
    assertEquals("bytes 2-4/8", partial.contentRange);
    assertArrayEquals("cde".getBytes(UTF_8), partial.body);

    final Response head = request("HEAD", null);
    assertEquals(200, head.status);
    assertEquals("8", head.contentLength);
    assertEquals(0, head.body.length);
  }

  @Test
  public void testEmptyFile() throws Exception {
    current.set(fileRecord(new byte[0]));

    final Response response = request("GET", null);

    assertEquals(200, response.status);
    assertEquals("0", response.contentLength);
    assertEquals(0, response.body.length);
  }

  @Test
  public void testMissingFile() throws Exception {
    current.set(new UploadFileRecord("gone", directory.resolve("missing.mp4"), "video/mp4", null));

    final Response response = request("GET", null);

    assertEquals(404, response.status);
  }

  private static void writeRangeRequest(final OutputStream out, final int port,
                                        final String range) throws IOException {
    final String request = """
        GET /vfile/clip/clip.mp4 HTTP/1.1\r
        Host: 127.0.0.1:%d\r
        Range: %s\r
        Connection: keep-alive\r
        \r
        """.formatted(port, range);
    out.write(request.getBytes(UTF_8));
    out.flush();
  }

  private static Received readHttpBody(final InputStream in) throws IOException {
    final String status = readLine(in);
    int contentLength = -1;
    String line = readLine(in);
    while (!line.isEmpty()) {
      if (line.regionMatches(true, 0, "content-length:", 0, "content-length:".length())) {
        contentLength = Integer.parseInt(line.substring(line.indexOf(':') + 1).strip());
      }
      line = readLine(in);
    }
    if (contentLength < 0) {
      throw new IOException("response has no content length: " + status);
    }
    final byte[] body = in.readNBytes(contentLength);
    if (body.length != contentLength) {
      throw new IOException("short body for " + status);
    }
    return new Received(status, body);
  }

  private static String readLine(final InputStream in) throws IOException {
    final StringBuilder line = new StringBuilder();
    int previous = -1;
    while (true) {
      final int next = in.read();
      if (next < 0) {
        break;
      }
      if (previous == '\r' && next == '\n') {
        break;
      }
      if (next != '\r') {
        line.append((char) next);
      }
      previous = next;
    }
    return line.toString();
  }

  @Test
  public void testSecondRangeReusesConnection() throws Exception {
    final byte[] payload = payload(100);
    final UploadFileRecord record = fileRecord(payload);
    final HttpServer local = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    local.createContext("/", exchange -> InternalServer.respondWithPayload(exchange, record));
    local.start();
    try (Socket socket = new Socket()) {
      socket.connect(local.getAddress(), 2000);
      socket.setSoTimeout(2000);
      final OutputStream out = socket.getOutputStream();
      final InputStream in = socket.getInputStream();

      writeRangeRequest(out, local.getAddress().getPort(), "bytes=0-9");
      final Received first = readHttpBody(in);
      assertTrue(first.status().contains("206"));
      assertArrayEquals(Arrays.copyOfRange(payload, 0, 10), first.body());

      writeRangeRequest(out, local.getAddress().getPort(), "bytes=40-49");
      final Received second = readHttpBody(in);
      assertTrue(second.status().contains("206"));
      assertArrayEquals(Arrays.copyOfRange(payload, 40, 50), second.body());
    } finally {
      local.stop(0);
    }
  }

  @Test
  public void testRestoreReturnsRecordAlreadyPresent() {
    final UploadingFileRegistry registry = new UploadingFileRegistry();
    final UploadFileRecord record =
        new UploadFileRecord("clip", Path.of("clip.mp4"), "video/mp4", null);

    final UploadFileRecord restored = registry.restoreRecord(record);

    assertSame(restored, registry.restoreRecord(record));
  }

  private record Received(String status, byte[] body) {
  }

  private record Response(int status, String contentLength, String contentRange,
                          String acceptRanges, byte[] body) {
  }
}
