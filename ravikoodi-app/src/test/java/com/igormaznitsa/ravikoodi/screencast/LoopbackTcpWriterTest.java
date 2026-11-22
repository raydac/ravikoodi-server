package com.igormaznitsa.ravikoodi.screencast;

import static org.junit.Assert.assertTrue;

import java.net.Socket;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;

public class LoopbackTcpWriterTest {

  @Test(timeout = 5000)
  public void timeoutOnTheWriterThreadDoesNotDeadlock() throws Exception {
    final LoopbackTcpWriter writer = new LoopbackTcpWriter(
        "test-writer", Duration.ofMillis(50), 2, () -> true);
    final CountDownLatch finished = new CountDownLatch(1);
    writer.addNetConectionListener(new NetConnection.NetConnectionListener() {
      @Override
      public void onDataFlowTimeout(final NetConnection source, final Duration timeout) {
        source.dispose();
        finished.countDown();
      }

      @Override
      public void onCompleted(final NetConnection source) {
      }
    });
    writer.start();
    try (Socket socket = new Socket(writer.getInetAddress(), writer.getPort())) {
      assertTrue(finished.await(3, TimeUnit.SECONDS));
      assertTrue(socket.isConnected());
    } finally {
      writer.close();
    }
  }
}
