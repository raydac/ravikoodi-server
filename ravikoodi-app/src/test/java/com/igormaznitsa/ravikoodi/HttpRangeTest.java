package com.igormaznitsa.ravikoodi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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

  @Test
  public void testOpenEndedSeek() {
    final HttpRange range = new HttpRange("bytes=9500-", 10000);
    assertFalse(range.isUnsatisfiable());
    assertFalse(range.coversEntireContent());
    assertEquals(9500, range.getStart());
    assertEquals(9999, range.getEnd());
    assertEquals(500, range.getLength());
    assertEquals("bytes 9500-9999/10000", range.toStringForHeader());
  }

  @Test
  public void testEndPastEofIsClamped() {
    final HttpRange range = new HttpRange("bytes=50-99999", 100);
    assertEquals(50, range.getStart());
    assertEquals(99, range.getEnd());
    assertEquals(50, range.getLength());
    assertEquals("bytes 50-99/100", range.toStringForHeader());
  }

  @Test
  public void testEndEqualToSizeIsClamped() {
    final HttpRange range = new HttpRange("bytes=0-100", 100);
    assertTrue(range.coversEntireContent());
    assertEquals(0, range.getStart());
    assertEquals(99, range.getEnd());
    assertEquals(100, range.getLength());
  }

  @Test
  public void testSuffixRange() {
    final HttpRange range = new HttpRange("bytes=-20", 100);
    assertEquals(80, range.getStart());
    assertEquals(99, range.getEnd());
    assertEquals(20, range.getLength());
    assertEquals("bytes 80-99/100", range.toStringForHeader());
  }

  @Test
  public void testSuffixCoveringWholeFile() {
    final HttpRange range = new HttpRange("bytes=-500", 100);
    assertTrue(range.coversEntireContent());
    assertEquals(100, range.getLength());
  }

  @Test
  public void testUnsatisfiableStart() {
    final HttpRange range = new HttpRange("bytes=100-", 100);
    assertTrue(range.isUnsatisfiable());
    assertEquals(0, range.getLength());
    assertEquals("bytes */100", range.toStringForHeader());
  }

  @Test
  public void testUnknownUnitIsIgnored() {
    final HttpRange range = new HttpRange("items=0-1", 100);
    assertTrue(range.coversEntireContent());
  }

  @Test
  public void testWhitespace() {
    final HttpRange range = new HttpRange("  BYTES = 10 - 20 ", 100);
    assertEquals(10, range.getStart());
    assertEquals(20, range.getEnd());
    assertEquals(11, range.getLength());
  }

}
