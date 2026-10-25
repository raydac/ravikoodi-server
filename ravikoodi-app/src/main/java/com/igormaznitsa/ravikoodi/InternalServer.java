package com.igormaznitsa.ravikoodi;

import com.igormaznitsa.ravikoodi.screencast.PreemptiveBuffer;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class InternalServer {

  public static final String PATH_RESOURCES = "res";
  public static final String PATH_VFILES = "vfile";
  private static final Logger LOGGER = LoggerFactory.getLogger(InternalServer.class);
  private final AtomicReference<JavaServer> serverRef = new AtomicReference<>();
  private final UploadingFileRegistry fileRegistry;
  private final StaticFileRegistry staticFileRegistry;
  private final Map<String, UploadFileRecord> removedFileRecords = new ConcurrentHashMap<>();
  private final ApplicationPreferences options;
  private final PreemptiveBuffer screencastBuffer = new PreemptiveBuffer(4);
  private final List<InternalServerListener> listeners = new CopyOnWriteArrayList<>();
  private final AtomicBoolean screencastActive = new AtomicBoolean();
  private final AtomicReference<Throwable> lastStartServerError = new AtomicReference<>();
  
  @Autowired
  public InternalServer(
          final UploadingFileRegistry fileRegistry,
          final StaticFileRegistry staticFileRegistry,
          final ApplicationPreferences options
  ) {
    this.staticFileRegistry = staticFileRegistry;
    this.fileRegistry = fileRegistry;
    this.options = options;
    this.fileRegistry.setRemovedRecordsStore(this.removedFileRecords);
  }

  private static void stopServer(final JavaServer server) {
    if (server != null) {
      try {
        LOGGER.info("Stopping server");
        server.close();
      } catch (Exception ex) {
        LOGGER.error("Error during server stop", ex);
      }
    }
  }

  public void addListener(@NonNull final InternalServerListener listener) {
    this.listeners.add(listener);
  }

  public void removeListener(@NonNull final InternalServerListener listener) {
    this.listeners.remove(listener);
  }

  public PreemptiveBuffer getScreencastDataBuffer() {
    return this.screencastBuffer;
  }

  public boolean isStarted() {
    final JavaServer theServer = this.serverRef.get();
    return theServer != null;
  }

  @NonNull
  public String getScreenCastUrl() {
    return String.format((this.options.isServerSsl() ? "https://" : "http://")
            + "%s:%d/screen-cast.ts", this.getHost(),
            this.getPort());
  }

  @NonNull
  public String makeUrlPrefix(@NonNull final String resourcePath) {
      return String.format((this.options.isServerSsl() ? "https://" : "http://")
              + "%s:%d/%s", this.getHost(),
              this.getPort(),
              resourcePath);
  }
  
  @NonNull
  public String makeUrlFor(@NonNull final UploadFileRecord record) {
    try {
      final String name = record.getFile().getFileName().toString();
      final String encodedFileName = URLEncoder.encode(name, "UTF-8");
      final String pathPrefix = makeUrlPrefix(PATH_VFILES);
      return String.format("%s/%s/%s",
              pathPrefix,
              record.getId(),
              encodedFileName);
    } catch (UnsupportedEncodingException ex) {
      throw new Error("Unexpected exception", ex);
    }
  }

  private static void addStandardHeaders(final Headers headers, final UploadFileRecord record,
                                         final long contentLength, final boolean head) {
    headers.set("Content-Type", record.getMimeType());
    headers.set("Content-Length", Long.toString(contentLength));
    headers.set("Cache-Control", "no-cache, no-store, must-revalidate");
    headers.set("Pragma", "no-cache");
    headers.set("Content-Transfer-Encoding", "binary");
    headers.set("Expires", "0");
    if (!head) {
      headers.set("Connection", "Keep-Alive");
      headers.set("Keep-Alive", "max");
    }
    headers.set("Accept-Ranges", "bytes");
  }

  private void addScreenCastHeaders(final Headers response, final boolean head) {
    response.set("Content-Type","video/MP2T");
    response.set("Cache-Control", "no-cache, no-store");
    response.set("Pragma", "no-cache");
    response.set("Transfer-Encoding", "chunked");
    response.set("Content-Transfer-Encoding", "binary");
    response.set("Expires", "0");
    if (!head) {
      response.set("Connection", "Keep-Alive");
      response.set("Keep-Alive", "max");
    }
    response.set("Accept-Ranges", "none");
  }

  // Pass -1 so the server sends no body; 0 would select chunked encoding.
  private static void sendWithoutBody(final HttpExchange exchange, final int status)
      throws IOException {
    exchange.sendResponseHeaders(status, -1);
  }

  private static int statusOf(final HttpRange range) {
    return range.coversEntireContent() ? 200 : 206;
  }

  private static int statusForOpenFailure(final IOException failure) {
    if (failure instanceof FileNotFoundException || failure instanceof NoSuchFileException) {
      return 404;
    }
    return 500;
  }

  private static void rejectUnsatisfiable(final HttpExchange exchange, final HttpRange range)
      throws IOException {
    LOGGER.info("Unsatisfiable range {}", range);
    final Headers headers = exchange.getResponseHeaders();
    headers.set("Accept-Ranges", "bytes");
    headers.set("Content-Range", range.toStringForHeader());
    sendWithoutBody(exchange, 416);
  }

  private static void writeSuccessHeaders(final HttpExchange exchange,
                                          final UploadFileRecord record, final HttpRange range,
                                          final boolean head) {
    addStandardHeaders(exchange.getResponseHeaders(), record, range.getLength(), head);
    if (!range.coversEntireContent()) {
      exchange.getResponseHeaders().set("Content-Range", range.toStringForHeader());
    }
  }

  static void respondWithPayload(final HttpExchange exchange, final UploadFileRecord record)
      throws IOException {
    final String method = exchange.getRequestMethod();
    final boolean head = "head".equalsIgnoreCase(method);
    if (!head && !"get".equalsIgnoreCase(method)) {
      sendWithoutBody(exchange, 400);
      return;
    }

    final HttpRange range;
    try {
      range =
          new HttpRange(exchange.getRequestHeaders().getFirst("Range"), record.getPayloadSize());
    } catch (final IOException ex) {
      LOGGER.warn("Can't read payload size for {}", record, ex);
      sendWithoutBody(exchange, statusForOpenFailure(ex));
      return;
    }
    if (range.isUnsatisfiable()) {
      rejectUnsatisfiable(exchange, range);
      return;
    }

    if (!range.coversEntireContent()) {
      LOGGER.info("Partial request {} {}", record, range);
    }

    if (head || range.getLength() == 0L) {
      writeSuccessHeaders(exchange, record, range, head);
      sendWithoutBody(exchange, statusOf(range));
      return;
    }

    try (InputStream in = record.openFrom(range.getStart())) {
      writeSuccessHeaders(exchange, record, range, false);
      LOGGER.info("Start sending {} range {}, length {}", record, range, range.getLength());
      exchange.sendResponseHeaders(statusOf(range), range.getLength());
      writeRange(in, exchange.getResponseBody(), range);
      LOGGER.info("Complete sending {} range {}", record, range);
    } catch (final IOException ex) {
      if (exchange.getResponseCode() >= 0) {
        LOGGER.info("Playback connection closed for {}: {}", record, ex.toString());
        return;
      }
      LOGGER.error("Can't read {}", record, ex);
      sendWithoutBody(exchange, statusForOpenFailure(ex));
    }
  }

  private static void writeRange(final InputStream in, final OutputStream out,
                                 final HttpRange range) throws IOException {
    final byte[] buffer = new byte[64 * 1024];
    long remaining = range.getLength();
    while (remaining > 0L && !Thread.currentThread().isInterrupted()) {
      final int read = in.read(buffer, 0, (int) Math.min(buffer.length, remaining));
      if (read < 0) {
        break;
      }
      out.write(buffer, 0, read);
      out.flush();
      remaining -= read;
    }
    if (remaining > 0L) {
      LOGGER.warn("Short payload for {}, {} bytes left unread", range, remaining);
    }
  }

  public boolean isScreencastFlowActive() {
    return this.screencastActive.get();
  }

  private HttpHandler makeHandler() {
    return exchange -> {
        LOGGER.info("Incoming request {} {}", exchange.getRequestMethod(), exchange.getRequestURI().toString());

        String preparedTarget = exchange.getRequestURI().toString();
        if (preparedTarget.contains("?")) {
          preparedTarget = preparedTarget.substring(0, preparedTarget.indexOf("?"));
        }

        final List<String> path = Stream.of(preparedTarget.split("\\/")).collect(Collectors.toList());
        final String pathLast = !path.isEmpty() ? path.get(path.size()-1) : null;
        final String pathPreLast = path.size() > 1 ? path.get(path.size()-2) : null;
        final String pathPrePreLast = path.size() > 2 ? path.get(path.size()-3) : null;

        if ("screen-cast.ts".equals(pathLast)) {
          if ("head".equalsIgnoreCase(exchange.getRequestMethod())) {
            addScreenCastHeaders(exchange.getResponseHeaders(), true);
            exchange.sendResponseHeaders(200, -1);
          } else if ("get".equalsIgnoreCase(exchange.getRequestMethod())) {
            addScreenCastHeaders(exchange.getResponseHeaders(), false);
            LOGGER.info("Starting screen cast retranslation");
            exchange.sendResponseHeaders(200, -1);
            final OutputStream out = exchange.getResponseBody();
            boolean waitDataEndDetected = false;
            try {
              screencastActive.set(true);
              screencastBuffer.clear();
              screencastBuffer.start();
              final long MAX_WAIT_DATA_MS = 15000;
              long waitDataEnd = System.currentTimeMillis() + MAX_WAIT_DATA_MS;
              while (!Thread.currentThread().isInterrupted()) {
                final byte[] next = screencastBuffer.next();
                if (next == null) {
                  if (waitDataEnd < System.currentTimeMillis()) {
                    LOGGER.warn("There is no screen cast data longer than " + (MAX_WAIT_DATA_MS / 1000L) + " sec, stopping");
                    waitDataEndDetected = true;
                    break;
                  }
                } else {
                  screencastActive.set(true);
                  if (next.length > 0) {
                    try {
                      out.write(next);
                      out.flush();
                    } catch (IOException ex) {
                      break;
                    } finally {
                      waitDataEnd = System.currentTimeMillis() + MAX_WAIT_DATA_MS;
                    }
                  }
                }
              }
            } finally {
              screencastActive.set(false);
              LOGGER.info("Screen cast retranslation ended, waitDataEnd={}, thread interrupted = {}", waitDataEndDetected, Thread.currentThread().isInterrupted());
              listeners.forEach(x -> x.onScreencastEnded(InternalServer.this));
            }
          } else {
            LOGGER.warn("Unsupported screen cast method {}", exchange.getRequestMethod());
            exchange.sendResponseHeaders(405, -1);
          }
        } else if (PATH_VFILES.equals(pathPrePreLast) || PATH_RESOURCES.equals(pathPreLast)) {

          final boolean staticResource = PATH_RESOURCES.equals(pathPreLast);
          final String uid = staticResource ? pathLast : pathPreLast;

          final UploadFileRecord record;

          if (staticResource) {
            LOGGER.info("Request for static file resource: {}", uid);
            record = staticFileRegistry.findFile(uid).orElse(null);
          } else {
            UploadFileRecord rec = fileRegistry.find(uid);
            if (rec == null) {
              final UploadFileRecord removedRecord = removedFileRecords.remove(uid);
              if (removedRecord != null) {
                LOGGER.info("found among removed streams, restoring: {}", uid);
                rec = fileRegistry.restoreRecord(removedRecord);
              }
            }
            record = rec;
          }

          if (record == null) {
            LOGGER.warn("Request for non-registered file {}", uid);
            exchange.sendResponseHeaders(404, -1);
          } else {
            record.incUploadsCounter();
            try {
              final String method = exchange.getRequestMethod();
              if ("head".equalsIgnoreCase(method) || "get".equalsIgnoreCase(method)) {
                respondWithPayload(exchange, record);
              } else {
                LOGGER.warn("Bad request : {}", exchange.getRequestURI());
                exchange.sendResponseHeaders(400, -1);
              }
            } finally {
              record.decUploadsCounter();
            }
          }
        } else {
          LOGGER.warn("Unsupported request {}", exchange.getRequestURI());
          exchange.sendResponseHeaders(405, -1);
        }
    };
  }
  
  @NonNull
  public String getHost() {
    String result = "";
    final JavaServer currentServer = this.serverRef.get();
    if (currentServer != null) {
      result = currentServer.getHost();
    }
    return result;
  }
  
  public int getPort() {
    int result = -1;
    final JavaServer currentServer = this.serverRef.get();
    if (currentServer != null) {
      result = currentServer.getPort();
    }
    return result;
  }
  
  @PostConstruct
  public void restartServer() {
    JavaServer theServer = this.serverRef.getAndSet(null);
    stopServer(theServer);

    this.fileRegistry.clear();

    final String host = this.options.getServerHost();
    final int port = this.options.getServerPort();

    LOGGER.info("Starting server on {}:{}", host, port);

    try {
      theServer = new JavaServer(this.options.getServerHost(), this.options.getServerPort(), this.options.isServerSsl(), makeHandler());
      this.lastStartServerError.set(null);
      if (this.serverRef.compareAndSet(null, theServer)) {
        theServer.start();
        LOGGER.info("Server connector has been started on {}:{}", this.getHost(), this.getPort());
      } else {
        LOGGER.warn("Detected racing in server start");
        stopServer(theServer);
        theServer = null;
      }
    } catch (Exception ex) {
      LOGGER.error("Can't start server", ex);
      this.lastStartServerError.set(ex);
      stopServer(theServer);
    }
  }

  public Throwable getLastStartServerError() {
    return this.lastStartServerError.get();
  }
  
  @PreDestroy
  public void preDestroy() throws Exception {
    LOGGER.info("Destroying server");
    final JavaServer theServer = this.serverRef.getAndSet(null);
    if (theServer == null) {
      LOGGER.info("Server was not started");
    } else {
      theServer.close();
    }
  }

  public interface InternalServerListener {

    void onScreencastStarted(@NonNull InternalServer source);

    void onScreencastEnded(@NonNull InternalServer source);
  }
}
