package com.igormaznitsa.ravikoodi;

import com.igormaznitsa.ravikoodi.HttpRange;
import org.junit.Test;
import static org.junit.Assert.*;

public class HttpRangeTest {
  
  @Test
  public void testStartFromZero() {
    final HttpRange range = new HttpRange("bytes=0-", 1000);
    assertEquals(0, range.getStart());
    assertEquals(999, range.getEnd());
    assertEquals(1000, range.getLength());
  }
  
  @Test
  public void testHeaderString() {
    final HttpRange range = new HttpRange("bytes=64312833-64657026", 64657027);
    assertEquals("bytes 64312833-64657026/64657027", range.toStringForHeader());
  }
  
  @Test
  public void testInterval1() {
    final HttpRange range = new HttpRange("bytes=64312833-64657026", 64657027);
    assertEquals(64312833, range.getStart());
    assertEquals(64657026, range.getEnd());
    assertEquals(344194, range.getLength());
  }
  
  @Test
  public void testInterval2() {
    final HttpRange range = new HttpRange("bytes=1073152-64313343", 64657027);
    assertEquals(1073152, range.getStart());
    assertEquals(64313343, range.getEnd());
    assertEquals(63240192, range.getLength());
  }
  
}
