package com.igormaznitsa.ravikoodi.ytloader;

import static com.igormaznitsa.ravikoodi.YoutubeUtils.extractYoutubeVideoId;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class YtLinkExtractorTest {

  @Test
  @Ignore
  public void testFindVideo() throws Exception {
    final ExecutorService executor = Executors.newSingleThreadExecutor();

    try {
      final YtLinkExtractor linkExtractor = new YtLinkExtractor(
          executor,
          new YoutubeSessionStore(new YoutubeSessionStore.PreferenceNode() {
            @Override
            public String get(final String key) {
              return null;
            }

            @Override
            public void put(final String key, final String value) {
            }

            @Override
            public void remove(final String key) {
            }
          }, session -> {
            throw new IOException("no refresh");
          }));
      final String videoId =
          extractYoutubeVideoId("https://www.youtube.com/watch?v=R3GfuzLMPkA").orElseThrow();

      final CountDownLatch countDownLatch = new CountDownLatch(1);
      final AtomicReference<String> refA = new AtomicReference<>();
      final AtomicReference<String> refB = new AtomicReference<>();
      final AtomicReference<Throwable> refError = new AtomicReference<>();

      linkExtractor.findVideoUrlAsync(
          videoId,
          YtQuality.p360,
          YtVideoType.MP4,
          (a, b, error) -> {
            refA.set(a);
            refB.set(b);
            refError.set(error);
            countDownLatch.countDown();
          });

      countDownLatch.await();

      assertNull("error: " + refError.get(), refError.get());

    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      Assert.fail();
    }
  }

}