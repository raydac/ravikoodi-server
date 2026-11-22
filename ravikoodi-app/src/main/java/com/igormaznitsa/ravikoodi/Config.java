package com.igormaznitsa.ravikoodi;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  @Bean(destroyMethod = "shutdownNow")
  public ScheduledExecutorService createScheduledExecutorService() {
    final ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(
        5,
        (Runnable runnable) -> {
          final Thread result =
              new Thread(runnable, "scheduled-executor-service-" + System.nanoTime());
          result.setDaemon(true);
          return result;
        });
    executor.setRemoveOnCancelPolicy(true);
    return executor;
  }
}
