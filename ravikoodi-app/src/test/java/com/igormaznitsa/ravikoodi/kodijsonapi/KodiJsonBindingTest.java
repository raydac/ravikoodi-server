package com.igormaznitsa.ravikoodi.kodijsonapi;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Test;

public class KodiJsonBindingTest {

  @Test
  public void readsRepliesWithoutMutatingFinalFields() throws Exception {
    final PrintStream previous = System.err;
    final ByteArrayOutputStream captured = new ByteArrayOutputStream();
    System.setErr(new PrintStream(captured, true, UTF_8));
    try {
      final ActivePlayerInfo player = KodiService.mapper().readValue(
          "{\"playerid\":7,\"type\":\"video\",\"extra\":true}", ActivePlayerInfo.class);
      assertEquals(7L, player.getPlayerid());
      assertEquals("video", player.getType());

      final PlayerProperties properties = KodiService.mapper().readValue(
          "{\"speed\":1,\"time\":{\"hours\":0,\"minutes\":1,\"seconds\":2,\"milliseconds\":3}}",
          PlayerProperties.class);
      assertEquals(1L, properties.getSpeed());
      assertEquals(1, properties.getTime().getMinutes());
      assertFalse(properties.isCanseek());
      assertNull(properties.getCurrentaudiostream());

      final PlayerSpeed speed = KodiService.mapper().readValue("{\"speed\":2}", PlayerSpeed.class);
      assertEquals(2L, speed.speed);

      final ListLimitsReturned limits =
          KodiService.mapper().readValue("{\"total\":10,\"start\":1}", ListLimitsReturned.class);
      assertEquals(10L, limits.getTotal());
      assertEquals(1L, limits.getStart());
      assertEquals(-1L, limits.getEnd());

      final ListLimitsReturned bounded =
          KodiService.mapper().readValue("{\"total\":10,\"end\":4}", ListLimitsReturned.class);
      assertEquals(0L, bounded.getStart());
      assertEquals(4L, bounded.getEnd());
    } finally {
      System.setErr(previous);
    }

    assertFalse(captured.toString(UTF_8).contains("has been mutated reflectively"));
  }
}
