/*
 * Copyright 2018 Igor Maznitsa.
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
package com.igormaznitsa.ravikoodi.certificategen;

import java.security.KeyStore;
import org.springframework.lang.NonNull;

public interface SelfSignedCertificateGenerator {
  
  /**
   * Generate self signed certificate
   * @param certValues parameters like "CN=KodiContentServer, OU=KodiJson, O=Kodi, C=EE"
   * @param certificateEntryAlias alias of the certificate in the key store
   * @param keyEntryAlias alias of the key entry
   * @param password password, must not be null
   * @return generated keystore, must not be null
   * @throws Exception it will be thrown for ny error
   */
  @NonNull
  KeyStore createSelfSigned(@NonNull String certValues, @NonNull String certificateEntryAlias, @NonNull String keyEntryAlias, @NonNull String password) throws Exception;
}
