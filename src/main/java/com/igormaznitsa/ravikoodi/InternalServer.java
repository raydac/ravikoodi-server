package com.igormaznitsa.ravikoodi;

import com.igormaznitsa.ravikoodi.screencast.PreemptiveBuffer;
import com.igormaznitsa.ravikoodi.UploadingFileRegistry.FileRecord;
import com.igormaznitsa.ravikoodi.certificategen.SelfSignedCertificateGenerator;
import com.igormaznitsa.ravikoodi.certificategen.SelfSignedCertificateGeneratorFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class InternalServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(InternalServer.class);

  private final AtomicReference<Server> serverRef = new AtomicReference<>();

  private final UploadingFileRegistry fileRegistry;
  private final Map<UUID, FileRecord> removedFileRecords = new ConcurrentHashMap<>();
  private final ApplicationPreferences options;
  private final SelfSignedCertificateGeneratorFactory certificateFactory;
  private final PreemptiveBuffer screencastBuffer = new PreemptiveBuffer(4);
  private final List<InternalServerListener> listeners = new CopyOnWriteArrayList<>();
  private final AtomicBoolean screencastActive = new AtomicBoolean();

  private final AtomicReference<Throwable> lastStartServerError = new AtomicReference<>();
  
  public interface InternalServerListener {

    void onScreencastStarted(@NonNull InternalServer source); 

    void onScreencastEnded(@NonNull InternalServer source);
  }

  public void addListener(@NonNull final InternalServerListener listener) {
    this.listeners.add(listener);
  }

  public void removeListener(@NonNull final InternalServerListener listener) {
    this.listeners.remove(listener);
  }

  @Autowired
  public InternalServer(
          final SelfSignedCertificateGeneratorFactory certificateFactory,
          final UploadingFileRegistry fileRegistry,
          final ApplicationPreferences options
  ) {
    this.certificateFactory = certificateFactory;
    this.fileRegistry = fileRegistry;
    this.options = options;
    this.fileRegistry.setRemovedRecordsStore(this.removedFileRecords);
  }

  public PreemptiveBuffer getScreencastDataBuffer() {
    return this.screencastBuffer;
  }

  public boolean isStarted() {
    final Server theServer = this.serverRef.get();
    return theServer != null && theServer.isStarted();
  }

  @NonNull
  public String getScreenCastUrl() {
    return String.format((this.options.isServerSsl() ? "https://" : "http://")
            + "%s:%d/screen-cast.ts", this.getHost(),
            this.getPort());
  }

  @NonNull
  public String makeUrlFor(@NonNull final FileRecord record) {
    try {
      final String name = record.getFile().getFileName().toString();
      final String encodedFileName = URLEncoder.encode(name, "UTF-8");
      return String.format((this.options.isServerSsl() ? "https://" : "http://")
              + "%s:%d/vfile/%s/%s", this.getHost(),
              this.getPort(),
              record.getUUID().toString(),
              encodedFileName);
    } catch (UnsupportedEncodingException ex) {
      throw new Error("Unexpected exception", ex);
    }
  }

  private void addStandardHeaders(final HttpServletResponse response, final FileRecord record, final boolean head) {
    response.setContentType(record.getMimeType());
    response.setContentLengthLong(record.getFile().toFile().length());
    response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Content-Transfer-Encoding", "binary");
    response.setHeader("Expires", "0");
    if (!head) {
      response.setHeader("Connection", "Keep-Alive");
      response.setHeader("Keep-Alive", "max");
    }
    response.setHeader("Accept-Ranges", "bytes");
  }

  private void addScreenCastHeaders(final HttpServletResponse response, final boolean head) {
    response.setContentType("video/MP2T");
    response.setHeader("Cache-Control", "no-cache, no-store");
    response.setHeader("Pragma", "no-cache");
    response.setHeader("Transfer-Encoding", "chunked");
    response.setHeader("Content-Transfer-Encoding", "binary");
    response.setHeader("Expires", "0");
    if (!head) {
      response.setHeader("Connection", "Keep-Alive");
      response.setHeader("Keep-Alive", "max");
    }
    response.setHeader("Accept-Ranges", "none");
  }

  public boolean isScreencastFlowActive() {
    return this.screencastActive.get();
  }

  
  
  @NonNull
  public String getHost() {
    String result = "";
    final Server currentServer = this.serverRef.get();
    if (currentServer != null) {
      final Optional<ServerConnector> serverConnector = Stream.of(currentServer.getConnectors()).filter(x -> x instanceof ServerConnector).map(x -> (ServerConnector)x).findFirst();
      result = serverConnector.isPresent()? serverConnector.get().getHost() : "<server_inactive>";
    }
    return result;
  }
  
  public int getPort() {
    int result = -1;
    final Server currentServer = this.serverRef.get();
    if (currentServer != null) {
      final Optional<ServerConnector> serverConnector = Stream.of(currentServer.getConnectors()).filter(x -> x instanceof ServerConnector).map(x -> (ServerConnector) x).findFirst();
      result = serverConnector.isPresent() ? serverConnector.get().getLocalPort() : -1;
    }
    return result;
  }
  
  @PostConstruct
  public void restartServer() {
    Server theServer = this.serverRef.getAndSet(null);
    stopServer(theServer);

    this.fileRegistry.clear();

    final String host = this.options.getServerHost();
    final int port = this.options.getServerPort();

    LOGGER.info("Starting server on {}:{}", host, port);

    theServer = new Server(new QueuedThreadPool(32, 4, 120000));

    final ServerConnector connector;
    if (this.options.isServerSsl()) {
      final SslContextFactory sslContextFactory = new SslContextFactory.Client(true);
      sslContextFactory.setExcludeCipherSuites("");

      final SelfSignedCertificateGenerator certificateGen = this.certificateFactory.find();
      if (certificateGen == null) {
        LOGGER.warn("Can't find self-signed certificate generator");
      } else {
        try {
          LOGGER.info("Generating self-signed certificate store");
          final KeyStore keyStore = certificateGen.createSelfSigned("CN=ravikoodi-content-server, OU=Igor Maznitsa, O=IgorMaznitsa.com, L=Tallinn, S=Harjumaa, C=EE", "jetty", "jetty", "someSecretPassword");

          sslContextFactory.setKeyStore(keyStore);
          sslContextFactory.setTrustStore(keyStore);
          sslContextFactory.setKeyManagerPassword("someSecretPassword");
          sslContextFactory.setKeyStorePassword("someSecretPassword");
          sslContextFactory.setTrustStorePassword("someSecretPassword");
          sslContextFactory.setCertAlias("jetty");

        } catch (Exception ex) {
          LOGGER.error("Can't generate self-signed certificate, see log!", ex);
        }
      }

      connector = new ServerConnector(theServer, sslContextFactory, new HttpConnectionFactory());
    } else {
      connector = new ServerConnector(theServer, new HttpConnectionFactory());
    }

    connector.setHost(host);
    connector.setPort(port);
    theServer.addConnector(connector);

    final Handler handler = new DefaultHandler() {
      @Override
      public void handle(
              String target,
              Request baseRequest,
              HttpServletRequest request,
              HttpServletResponse response
      ) throws IOException, ServletException {
        try {
          LOGGER.info("Incoming request {} {}", request.getMethod(), target);
          response.setBufferSize(32 * 1024);

          final String[] path = target.split("\\/");

          if (path.length >= 2 && "screen-cast.ts".equals(path[1])) {
            if ("head".equalsIgnoreCase(request.getMethod())) {
              addScreenCastHeaders(response, true);
              response.setStatus(HttpServletResponse.SC_OK);
            } else if ("get".equalsIgnoreCase(request.getMethod())) {
              addScreenCastHeaders(response, false);
              LOGGER.info("Starting screen cast retranslation");
              response.setStatus(HttpServletResponse.SC_OK);
              final OutputStream out = response.getOutputStream();
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
                LOGGER.info("Screen cast retranslation ended, waiDatdEnd={}, thread interrupted = {}", waitDataEndDetected, Thread.currentThread().isInterrupted());
                listeners.forEach(x -> x.onScreencastEnded(InternalServer.this));
              }
            }
          } else if (path.length > 3 && "vfile".equals(path[1])) {

            final UUID uuid = UUID.fromString(path[2]);
            UploadingFileRegistry.FileRecord record = fileRegistry.find(uuid);

            if (record == null) {
              final FileRecord removedRecord = removedFileRecords.remove(uuid);
              if (removedRecord != null) {
                LOGGER.info("found among removed streams, restoring: {}", uuid);
                record = fileRegistry.restoreRecord(removedRecord);
              }
            }

            if (record == null) {
              LOGGER.warn("Request for non-registered file {}", uuid);
              response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            } else {
              record.incUploadsCounter();
              try {
                if ("head".equalsIgnoreCase(request.getMethod())) {
                  addStandardHeaders(response, record, true);
                  response.setStatus(HttpServletResponse.SC_OK);
                } else if ("get".equalsIgnoreCase(request.getMethod())) {

                  final long fileSize = record.getPredefinedData().isPresent() ? record.getPredefinedData().get().length : Files.size(record.getFile());
                  final HttpRange range = new HttpRange(request.getHeader("Range"), fileSize);

                  addStandardHeaders(response, record, false);

                  if (range.getStart() > fileSize || range.getStart() > range.getEnd() || range.getEnd() > fileSize) {
                    response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
                    return;
                  } else if (range.getStart() > 0 || range.getEnd() < (fileSize - 1)) {
                    LOGGER.info("Request for {}, range {}", uuid, range);
                    response.setContentLengthLong(range.getLength());
                    response.setHeader("Content-Range", range.toStringForHeader());
                    response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT);
                  } else {
                    response.setContentLengthLong(fileSize);
                    response.setStatus(HttpServletResponse.SC_OK);
                  }

                  final OutputStream out = response.getOutputStream();

                  final byte[] buffer = new byte[64 * 1024];

                  try (final InputStream in = record.getAsInputStream()) {
                    long pos = range.getStart();

                    if (pos != in.skip(pos)) {
                      response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                      LOGGER.error("Can't skip {} bytes in file", pos);
                      return;
                    }

                    LOGGER.info("Start sending data for {} ({}) to device, requested range = {}, expected length = {} bytes", uuid, record.getFile(), range, range.getLength());

                    while (pos <= range.getEnd() && !Thread.currentThread().isInterrupted()) {
                      final int read = in.read(buffer, 0, Math.min((int) (range.getEnd() - pos + 1), buffer.length));
                      if (read < 0) {
                        break;
                      }
                      out.write(buffer, 0, read);
                      out.flush();
                      pos += read;
                    }
                  }
                  LOGGER.info("Complete '{}' writing in range {}", uuid, range);
                } else {
                  LOGGER.warn("Bad request : {}", request);
                  response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                }
              } finally {
                record.decUploadsCounter();
              }
            }
          } else {
            LOGGER.warn("Unsupported request {}", target);
            response.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
          }
        } finally {
          baseRequest.setHandled(true);
        }
      }
    };

    theServer.setHandler(handler);

    try {
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
  
  private static void stopServer(final Server server) {
    if (server != null) {
      try {
        LOGGER.info("Stopping server");
        server.stop();
      } catch (Exception ex) {
        LOGGER.error("Error during server stop", ex);
      }
    }
  }

  @PreDestroy
  public void preDestroy() throws Exception {
    LOGGER.info("Destroying server");
    final Server theServer = this.serverRef.getAndSet(null);
    if (theServer == null) {
      LOGGER.info("Server was not started");
    } else {
      theServer.stop();
    }
  }
}
