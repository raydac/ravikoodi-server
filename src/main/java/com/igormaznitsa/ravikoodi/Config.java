package com.igormaznitsa.ravikoodi;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

  private static final Logger LOGGER = LoggerFactory.getLogger(Config.class);

  @Autowired
  private ApplicationContext context;

  @Bean
  public ScheduledExecutorService createScheduledExecutorService() {
    return new ScheduledThreadPoolExecutor(
            2,
            (Runnable r) -> {
      final Thread result = new Thread(r,"scheduled-executor-service-"+System.nanoTime());
      result.setDaemon(true);
      return result;
    });
  }
}
