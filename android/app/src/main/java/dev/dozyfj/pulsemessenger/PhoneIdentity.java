package dev.dozyfj.pulsemessenger;

import android.security.keystore.KeyGenParameterSpec;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Date;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.security.auth.x500.X500Principal;

final class PhoneIdentity {
    final String fingerprint;
    final SSLContext tls;

    PhoneIdentity(KeyStore keys, char[] password, String alias) throws Exception {
        KeyManagerFactory factory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        factory.init(keys, password);
        this.tls = SSLContext.getInstance("TLS");
        this.tls.init(factory.getKeyManagers(), null, new SecureRandom());
        this.fingerprint = PhoneServerStore.hex(MessageDigest.getInstance("SHA-256").digest(keys.getCertificate(alias).getEncoded()));
    }

    static PhoneIdentity load(boolean replaceLegacy) throws Exception {
        KeyStore keys = KeyStore.getInstance("AndroidKeyStore");
        keys.load(null);
        if (!replaceLegacy && !keys.containsAlias("pulse-phone-host-ec") && keys.containsAlias("pulse-phone-host")) {
            return new PhoneIdentity(keys, null, "pulse-phone-host");
        }
        if (!keys.containsAlias("pulse-phone-host-ec")) {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("EC", "AndroidKeyStore");
            generator.initialize(new KeyGenParameterSpec.Builder("pulse-phone-host-ec", 12).setAlgorithmParameterSpec(new ECGenParameterSpec("secp256r1")).setDigests("NONE", "SHA-256").setCertificateSubject(new X500Principal("CN=Pulse phone server")).setCertificateSerialNumber(BigInteger.ONE).setCertificateNotBefore(new Date(0L)).setCertificateNotAfter(new Date(4102444800000L)).build());
            generator.generateKeyPair();
        }
        return new PhoneIdentity(keys, null, "pulse-phone-host-ec");
    }
}
