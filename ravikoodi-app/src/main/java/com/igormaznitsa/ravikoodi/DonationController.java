/*
 * Copyright 2020 Igor Maznitsa.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.igormaznitsa.ravikoodi;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class DonationController {

  private final Logger LOGGER = LoggerFactory.getLogger(DonationController.class);
  
  public enum Provider {
    PAYPAL("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2"),
    YOO_MONEY("https://img.shields.io/badge/donation-Yoo.money-blue.svg)](https://yoomoney.ru/to/41001158080699");

    private final URL url;

    private Provider(@NonNull final String url) {
      try {
        this.url = new URL(url);
      } catch (MalformedURLException ex) {
        throw new Error("Malformed URL: " + url, ex);
      }
    }

    @NonNull
    public URL getUrl() {
      return this.url;
    }
  }

  public DonationController() {
  }

  public void openDonationUrl() {
    final Provider provider;
    if ("RU".equalsIgnoreCase(Locale.getDefault().getCountry()) && "RU".equalsIgnoreCase(Locale.getDefault().getLanguage())) {
      provider = Provider.YOO_MONEY;
    } else {
      provider = Provider.PAYPAL;
    }
    LOGGER.info("Selected donation provider: {}", provider);
    Utils.showURLExternal(provider.getUrl());
  }
}
