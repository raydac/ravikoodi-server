package com.igormaznitsa.ravikoodi;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  @Bean
  public ScheduledExecutorService createScheduledExecutorService() {
    return new ScheduledThreadPoolExecutor(
            5,
            (Runnable r) -> {
      final Thread result = new Thread(r,"scheduled-executor-service-"+System.nanoTime());
      result.setDaemon(true);
      return result;
    });
  }
}
