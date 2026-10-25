package com.igormaznitsa.ravikoodi;

import static java.util.Optional.empty;
import static java.util.Optional.of;

import java.util.Optional;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public final class HttpRange {

  private final String unit;
  private final long start;
  private final long end;
  private final long contentSize;
  private final boolean unsatisfiable;

  public HttpRange(@Nullable final String header, final long fullContentSize) {
    if (fullContentSize < 0L) {
      throw new IllegalArgumentException("content size must not be negative");
    }

    final Resolved resolved = resolve(header, fullContentSize);
    this.unit = "bytes";
    this.contentSize = fullContentSize;
    this.unsatisfiable = resolved.unsatisfiable();
    this.start = resolved.start();
    this.end = resolved.end();
  }

  private static Resolved resolve(@Nullable final String header, final long contentSize) {
    if (header == null || header.isBlank()) {
      return entire(contentSize);
    }

    final String trimmed = header.strip();
    final int separator = trimmed.indexOf('=');
    if (separator <= 0) {
      return entire(contentSize);
    }

    final String unit = trimmed.substring(0, separator).strip();
    if (!"bytes".equalsIgnoreCase(unit)) {
      return entire(contentSize);
    }

    final String spec = trimmed.substring(separator + 1).strip();
    if (spec.isEmpty() || spec.indexOf(',') >= 0) {
      return entire(contentSize);
    }

    final int dash = spec.indexOf('-');
    if (dash < 0) {
      return entire(contentSize);
    }

    final String left = spec.substring(0, dash).strip();
    final String right = spec.substring(dash + 1).strip();
    if (left.isEmpty()) {
      return resolveSuffix(right, contentSize);
    }
    return resolveInterval(left, right, contentSize);
  }

  private static Resolved resolveSuffix(final String suffixText, final long contentSize) {
    final long suffixLength = parseLong(suffixText).orElse(-1L);
    if (suffixLength < 0L) {
      return entire(contentSize);
    }
    if (suffixLength == 0L) {
      return unsatisfiable();
    }
    if (contentSize == 0L || suffixLength >= contentSize) {
      return entire(contentSize);
    }
    return partial(contentSize - suffixLength, contentSize - 1L);
  }

  private static Resolved resolveInterval(final String startText, final String endText,
                                          final long contentSize) {
    final long start = parseLong(startText).orElse(-1L);
    if (start < 0L) {
      return entire(contentSize);
    }
    if (start >= contentSize) {
      return unsatisfiable();
    }
    if (endText.isEmpty()) {
      return partial(start, contentSize - 1L);
    }

    final long end = parseLong(endText).orElse(-1L);
    if (end < start) {
      return entire(contentSize);
    }
    return partial(start, Math.min(end, contentSize - 1L));
  }

  private static Optional<Long> parseLong(final String text) {
    if (text.isEmpty()) {
      return empty();
    }
    try {
      return of(Long.parseLong(text));
    } catch (final NumberFormatException ex) {
      return empty();
    }
  }

  private static Resolved entire(final long contentSize) {
    return new Resolved(false, 0L, contentSize - 1L);
  }

  private static Resolved unsatisfiable() {
    return new Resolved(true, 0L, -1L);
  }

  private static Resolved partial(final long start, final long end) {
    return new Resolved(false, start, end);
  }

  public String getUnit() {
    return this.unit;
  }

  public long getStart() {
    return this.start;
  }

  public long getEnd() {
    return this.end;
  }

  public boolean isUnsatisfiable() {
    return this.unsatisfiable;
  }

  public boolean coversEntireContent() {
    return !this.unsatisfiable && this.start == 0L && this.end == this.contentSize - 1L;
  }

  public long getLength() {
    if (this.unsatisfiable) {
      return 0L;
    }
    return this.end - this.start + 1L;
  }

  @Override
  @NonNull
  public String toString() {
    if (this.unsatisfiable) {
      return String.format("HttpRange(unsatisfiable,size=%d)", this.contentSize);
    }
    return String.format("HttpRange(unit=%s,start=%d,end=%d,size=%d)", this.unit, this.start,
        this.end, this.contentSize);
  }

  @NonNull
  public String toStringForHeader() {
    if (this.unsatisfiable) {
      return String.format("bytes */%d", this.contentSize);
    }
    return String.format("bytes %d-%d/%d", this.start, this.end, this.contentSize);
  }

  private record Resolved(boolean unsatisfiable, long start, long end) {
  }
}
