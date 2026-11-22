package com.igormaznitsa.ravikoodi.screencast;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.Test;

public class PreemptiveBufferTest {

  @Test
  public void pollReturnsNullWhenNothingArrives() {
    assertNull(new PreemptiveBuffer(2).poll(30L));
  }

  @Test
  public void pollReturnsDataPublishedByAnotherThread() throws Exception {
    final PreemptiveBuffer buffer = new PreemptiveBuffer(2);
    buffer.start();
    final AtomicReference<byte[]> taken = new AtomicReference<>();
    final Thread reader = new Thread(() -> taken.set(buffer.poll(2000L)));
    reader.start();
    buffer.put(new byte[] {7});
    reader.join(2000L);

    assertFalse(reader.isAlive());
    assertArrayEquals(new byte[] {7}, taken.get());
  }

  @Test
  public void startedBufferKeepsTheOldestChunkWhenFull() {
    final PreemptiveBuffer buffer = new PreemptiveBuffer(2);
    buffer.start();
    buffer.put(new byte[] {1});
    buffer.put(new byte[] {2});
    buffer.put(new byte[] {3});

    assertArrayEquals(new byte[] {1}, buffer.poll(0L));
    assertArrayEquals(new byte[] {3}, buffer.poll(0L));
    assertNull(buffer.poll(0L));
  }
}
