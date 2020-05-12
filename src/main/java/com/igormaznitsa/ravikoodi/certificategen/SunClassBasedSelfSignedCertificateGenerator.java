package com.igormaznitsa.ravikoodi.certificategen;

import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.concurrent.TimeUnit;
import org.springframework.lang.NonNull;
import sun.security.tools.keytool.CertAndKeyGen;
import sun.security.x509.X500Name;

public class SunClassBasedSelfSignedCertificateGenerator implements SelfSignedCertificateGenerator {

  public static final String KEY_TYPE_RSA = "RSA";
  public static final String SIG_ALG_SHA_RSA = "SHA1WithRSA";
  public static final int KEY_SIZE = 1024;
  public static final long CERT_VALIDITY = TimeUnit.DAYS.toSeconds(7);

  @Override
  @NonNull
  public KeyStore createSelfSigned(
          @NonNull final String certValues, 
          @NonNull final String certificateEntryAlias, 
          @NonNull final String keyEntryAlias, 
          @NonNull final String password
  ) throws Exception {
    final CertAndKeyGen keyGen = new CertAndKeyGen(KEY_TYPE_RSA, SIG_ALG_SHA_RSA);
    keyGen.generate(KEY_SIZE);

    final KeyStore ks = emptyStore(password);
    final X509Certificate certificate = keyGen.getSelfCertificate(new X500Name(certValues), CERT_VALIDITY);
    ks.setCertificateEntry(certificateEntryAlias, certificate);
    ks.setKeyEntry(keyEntryAlias, keyGen.getPrivateKey(), password.toCharArray(), new Certificate[]{certificate});
    return ks;
  }

  private KeyStore emptyStore(@NonNull final String password) throws Exception {
    final KeyStore ks = KeyStore.getInstance("JKS");
    ks.load(null, password.toCharArray());
    return ks;
  }

}
