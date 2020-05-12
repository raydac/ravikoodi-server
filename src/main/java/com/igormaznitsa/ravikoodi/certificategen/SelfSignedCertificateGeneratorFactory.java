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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class SelfSignedCertificateGeneratorFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(SelfSignedCertificateGeneratorFactory.class);

  private final SelfSignedCertificateGenerator generatorInstance;

  public SelfSignedCertificateGeneratorFactory(){
    this.generatorInstance = makeInstance();
    if (this.generatorInstance == null) {
      LOGGER.warn("Can't create self-signed certificate generator");
    } else {
      LOGGER.info("Created self-signed certificate generator: {}", this.generatorInstance.getClass().getSimpleName());
    }
  }
  
  @Nullable
  public SelfSignedCertificateGenerator find() {
    if (generatorInstance == null) {
      LOGGER.warn("Can't create self-signed certificate generator");
    } else {
      LOGGER.info("Created self-signed certificate generator: {}", generatorInstance.getClass().getSimpleName());
    }

    return generatorInstance;
  }

  private SelfSignedCertificateGenerator makeInstance() {
    try {
      final Class<?> klazz = Class.forName("com.igormaznitsa.ravikoodi.certificategen.SunClassBasedSelfSignedCertificateGenerator");
      final SelfSignedCertificateGenerator result = SelfSignedCertificateGenerator.class.cast(klazz.getConstructor().newInstance());
      LOGGER.info("Self-certificate generator is successfuly created");
      return result;
    } catch (Exception ex) {
      LOGGER.debug("Detected exception", ex);
      LOGGER.warn("Can't nstantiate self-certificate generator : {}", ex.getMessage());
    }
    return null;
  }
}
