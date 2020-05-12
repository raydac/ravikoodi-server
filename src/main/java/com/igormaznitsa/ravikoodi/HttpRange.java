package com.igormaznitsa.ravikoodi;

import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

public class HttpRange {

  private final String unit;
  private final long start;
  private final long end;
  private final long contentSize;

  public HttpRange(@Nullable String header, final long fullContentSize) {
    
    if (header == null) {
      header = "bytes=0-"+(fullContentSize-1);
    }
    
    final String[] unitcontent = header.split("\\=");

    this.contentSize = fullContentSize;
    
    final long maxPos = fullContentSize - 1;

    if (unitcontent.length != 2) {
      this.unit = "bytes";
      this.start = 0L;
      this.end = maxPos;
    } else {
      this.unit = unitcontent[0].trim();
      header = unitcontent[1].trim();
      if (header == null) {
        this.start = 0L;
        this.end = maxPos;
      } else {
        final String[] parse = header.split("\\-");
        switch (parse.length) {
          case 0: {
            this.start = 0L;
            this.end = maxPos;
          }
          break;
          case 1: {
            this.start = parseNumber(parse[0], 0L);
            this.end = maxPos;
          }
          break;
          default: {
            this.start = parseNumber(parse[0], 0L);
            this.end = parseNumber(parse[1], maxPos);
          }
          break;
        }
      }
    }
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

  @Override
  @NonNull
  public String toString() {
    return String.format("HttpRange(unit=%s,start=%d,end=%d)", this.unit, this.start, this.end);
  }

  private long parseNumber(@NonNull final String number, final long dflt) {
    try {
      final long result = Long.parseLong(number.trim());
      return result < 0L ? dflt : result;
    } catch (NumberFormatException ex) {
      return dflt;
    }
  }

  @NonNull
  public String toStringForHeader() {
    return String.format("%s %d-%d/%s", this.unit, this.start, this.end, this.contentSize == Long.MAX_VALUE ? "*" : String.valueOf(this.contentSize));
  }

  public long getLength() {
    return (this.end - this.start) + 1;
  }
}
