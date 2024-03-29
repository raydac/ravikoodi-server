package com.igormaznitsa.ravikoodi;

import com.igormaznitsa.ravikoodi.screencast.PreemptiveBuffer;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.security.KeyStore;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.DefaultHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class InternalServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(InternalServer.class);

  private final AtomicReference<Server> serverRef = new AtomicReference<>();

  private final UploadingFileRegistry fileRegistry;
  private final StaticFileRegistry staticFileRegistry;
  private final Map<String, UploadFileRecord> removedFileRecords = new ConcurrentHashMap<>();
  private final ApplicationPreferences options;
  private final PreemptiveBuffer screencastBuffer = new PreemptiveBuffer(4);
  private final List<InternalServerListener> listeners = new CopyOnWriteArrayList<>();
  private final AtomicBoolean screencastActive = new AtomicBoolean();

  private final AtomicReference<Throwable> lastStartServerError = new AtomicReference<>();
  
  public static final String PATH_RESOURCES = "res";
  public static final String PATH_VFILES = "vfile";
  
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
          final UploadingFileRegistry fileRegistry,
          final StaticFileRegistry staticFileRegistry,
          final ApplicationPreferences options
  ) {
    this.staticFileRegistry = staticFileRegistry;  
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

  private void addStandardHeaders(final HttpServletResponse response, final UploadFileRecord record, final boolean head) {
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

    theServer = new Server(new QueuedThreadPool(8, 2, 30000));

    final HttpConfiguration httpConfiguration = new HttpConfiguration();
    final SecureRequestCustomizer secureRequestCustomizer = new SecureRequestCustomizer();
    secureRequestCustomizer.setSniHostCheck(false);
    httpConfiguration.addCustomizer(secureRequestCustomizer);
    
    final ServerConnector connector;
    if (this.options.isServerSsl()) {
      final SslContextFactory.Server sslContextFactory = new SslContextFactory.Server();
      sslContextFactory.setSniRequired(false);
      sslContextFactory.setTrustAll(true);
      sslContextFactory.setExcludeCipherSuites("");
      
      try (final InputStream keyStoreStream = Objects.requireNonNull(new ClassPathResource("jks/selfsigned.jks").getInputStream())) {
          LOGGER.info("Loading certificate store");
          final String key = "someSecretKey";

          final KeyStore keyStore = KeyStore.getInstance("JKS");
          keyStore.load(keyStoreStream, key.toCharArray());

          sslContextFactory.setKeyStore(keyStore);
          sslContextFactory.setTrustStore(keyStore);
          sslContextFactory.setKeyManagerPassword(key);
          sslContextFactory.setKeyStorePassword(key);
          sslContextFactory.setTrustStorePassword(key);
          sslContextFactory.setCertAlias("jetty");

          LOGGER.info("Certificate store has been loaded from internal JKS store");
      } catch (Exception ex) {
          LOGGER.error("Can't generate self-signed certificate, see log!", ex);
      }

      connector = new ServerConnector(theServer, 
              new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()), 
              new HttpConnectionFactory(httpConfiguration));
    } else {
      connector = new ServerConnector(theServer, new HttpConnectionFactory(httpConfiguration));
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
          
          String preparedTarget = target;
          if (target.contains("?")) {
              preparedTarget = target.substring(0, target.indexOf("?"));
          }
          
          final List<String> path = Stream.of(preparedTarget.split("\\/")).collect(Collectors.toList());
          final String pathLast = path.size() > 0 ? path.get(path.size()-1) : null;
          final String pathPreLast = path.size() > 1 ? path.get(path.size()-2) : null;
          final String pathPrePreLast = path.size() > 2 ? path.get(path.size()-3) : null;
          
          if ("screen-cast.ts".equals(pathLast)) {
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
                LOGGER.info("Screen cast retranslation ended, waiDatdEnd={}, thread interrupted = {}", waitDataEndDetected, Thread.currentThread().isInterrupted());
                listeners.forEach(x -> x.onScreencastEnded(InternalServer.this));
              }
            }
          } else if (PATH_VFILES.equals(pathPrePreLast) || PATH_RESOURCES.equals(pathPreLast)) {

            final boolean staticResiource = PATH_RESOURCES.equals(pathPreLast);  
            final String uid = staticResiource ? pathLast : pathPreLast;
            
            final UploadFileRecord record;
            
            if (staticResiource) {
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
                    LOGGER.info("Request for {}, range {}", uid, range);
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

                    LOGGER.info("Start sending data for {} ({}) to device, requested range = {}, expected length = {} bytes", uid, record.getFile(), range, range.getLength());

                    while (pos <= range.getEnd() && !Thread.currentThread().isInterrupted()) {
                      final long rangeEndPos = range.getEnd() - pos + 1;
                      final int read = in.read(buffer, 0, Math.min(rangeEndPos > (long)Integer.MAX_VALUE ? buffer.length : (int) rangeEndPos, buffer.length));
                      if (read < 0) {
                        break;
                      }
                      out.write(buffer, 0, read);
                      out.flush();
                      pos += read;
                    }
                  }
                  LOGGER.info("Complete '{}' writing in range {}", uid, range);
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
