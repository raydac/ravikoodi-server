package com.igormaznitsa.ravikoodi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class LinuxBrowserDropSiteTest {

  private static final String TREE = """
           0x4600004 "pom.xml - ravikoodi-server - Cursor": ("cursor" "cursor")  1920x1053+1305+27
           0x6200007 "Ravikoodi content server": ("org-springframework-boot-loader-launch-JarLauncher" "org-springframework-boot-loader-launch-JarLauncher")  1920x1043+0+37
              0x620001c "Content window": ("org-springframework-boot-loader-launch-JarLauncher" "org-springframework-boot-loader-launch-JarLauncher")  1920x1080+0+-37
           0x2601fa3 "java - Stack Overflow — Mozilla Firefox": ("Navigator" "firefox")  1920x1053+1305+27
              0x2601fa4 "Content window": ("Navigator" "firefox")  100x100+0+0
      """;

  @Test
  public void testContentWindowUnderMatchingFrame() {
    final LinuxBrowserDropSite.ShellContent site = LinuxBrowserDropSite
        .findContentWindow(TREE, "Ravikoodi content server")
        .orElseThrow();

    assertEquals(0x6200007L, site.shell());
    assertEquals(0x620001cL, site.content());
  }

  @Test
  public void testClassNameIsNotTheWindowName() {
    assertFalse(LinuxBrowserDropSite.findContentWindow(TREE, "cursor").isPresent());
  }

  @Test
  public void testOtherFrameKeepsItsOwnContentWindow() {
    final LinuxBrowserDropSite.ShellContent site = LinuxBrowserDropSite
        .findContentWindow(TREE, "java - Stack Overflow — Mozilla Firefox")
        .orElseThrow();

    assertEquals(0x2601fa3L, site.shell());
    assertEquals(0x2601fa4L, site.content());
  }

  @Test
  public void testMissingContentWindow() {
    assertTrue(LinuxBrowserDropSite.findContentWindow("""
           0x10 "Ravikoodi content server": ("java" "java")  10x10+0+0
              0x11 "button": ("java" "java")  10x10+0+0
        """, "Ravikoodi content server").isEmpty());
  }
}
