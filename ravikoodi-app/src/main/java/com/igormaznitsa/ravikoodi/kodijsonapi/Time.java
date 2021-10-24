package com.igormaznitsa.ravikoodi.kodijsonapi;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import static java.time.temporal.ChronoUnit.MILLIS;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor(force = true)
public class Time {
  public static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
  
  private final int hours;
  private final int minutes;
  private final int seconds;
  private final int milliseconds;
  
  @Override
  public String toString() {
    return asLocalTime().format(FORMATTER);
  }

  public boolean isZero() {
    return this.hours == 0 && this.minutes == 0 && this.seconds == 0 && this.milliseconds == 0;
  }

  public LocalTime asLocalTime() {
    return LocalTime.of(this.hours, this.minutes, this.seconds).plus(this.milliseconds, MILLIS);
  }
}
