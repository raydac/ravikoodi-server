package com.igormaznitsa.ravikoodi;

import lombok.Data;

@Data
public class KodiAddress {
  private final String host;
  private final int port;
  private final String name;
  private final String password;
  private final boolean useSsl;
}
