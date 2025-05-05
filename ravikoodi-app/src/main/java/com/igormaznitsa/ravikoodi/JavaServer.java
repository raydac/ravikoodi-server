package com.igormaznitsa.ravikoodi;

import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newCachedThreadPool;

import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsServer;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.util.concurrent.ExecutorService;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

public final class JavaServer {

  private static final Logger LOGGER = LoggerFactory.getLogger(JavaServer.class);

  private final HttpServer server;
  private final ExecutorService executor = newCachedThreadPool(r -> {
    final Thread thread = new Thread(r, JavaServer.class.getSimpleName() + "-" + System.nanoTime());
    thread.setDaemon(true);
    return thread;
  });

  public JavaServer(
      final String host,
      final int port,
      final boolean httpsMode,
      final HttpHandler rootHandler) throws IOException {
    if (httpsMode) {
      LOGGER.info("Init int HTTPS mode {}:{}", host, port);
      final HttpsServer sslServer = HttpsServer.create(new InetSocketAddress(host, port), 64);
      sslServer.setHttpsConfigurator(new HttpsConfigurator(createSslContext()));
      this.server = sslServer;
    } else {
      LOGGER.info("Init int HTTP mode {}:{}", host, port);
      this.server = HttpServer.create(new InetSocketAddress(host, port), 64);
    }
    this.server.setExecutor(this.executor);
    this.server.createContext("/", rootHandler);
  }

  private static SSLContext createSslContext() throws IOException {
    try (final InputStream keyStoreStream = requireNonNull(
        new ClassPathResource("jks/selfsigned.jks").getInputStream())) {
      LOGGER.info("Loading certificate store");
      final String key = "someSecretKey";

      final KeyStore keyStore = KeyStore.getInstance("JKS");
      keyStore.load(keyStoreStream, key.toCharArray());

      final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
      keyManagerFactory.init(keyStore, "someSecretKey".toCharArray());
      final SSLContext ssl = SSLContext.getInstance("TLS");
      ssl.init(keyManagerFactory.getKeyManagers(), null, null);

      LOGGER.info("Certificate store has been loaded from internal JKS store");
      return ssl;
    } catch (Exception ex) {
      LOGGER.error("Can't load and init self-signed certificate, see log!", ex);
      if (ex instanceof IOException) {
        throw (IOException) ex;
      } else {
        throw new IOException(ex);
      }
    }
  }

  public void start() {
    LOGGER.info("Java server started on {}", this.server.getAddress());
    this.server.start();
  }

  public void stop() {
    try {
      this.server.stop(1);
    } catch (Exception ex) {
      LOGGER.error("Error during server close", ex);
    }
  }

  public void close() {
    this.stop();
    this.executor.shutdownNow();
  }

  public String getHost() {
    return this.server.getAddress().getHostString();
  }

  public int getPort() {
    return this.server.getAddress().getPort();
  }
}
