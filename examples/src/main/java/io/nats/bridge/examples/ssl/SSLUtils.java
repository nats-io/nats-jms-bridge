package io.nats.bridge.examples.ssl;

import io.nats.client.Options;

import javax.net.ssl.*;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.SecureRandom;

// This examples requires certificates to be in the java keystore format (.jks).
// To do so openssl is used to generate a pkcs12 file (.p12) from client-cert.pem and client-key.pem.
// The resulting file is then imported int a java keystore named keystore.jks using keytool which is part of java jdk.
// keytool is also used to import the CA certificate rootCA.pem into truststore.jks.
//
// openssl pkcs12 -export -out keystore.p12 -inkey client-key.pem -in client-cert.pem -password pass:password
// keytool -importkeystore -srcstoretype PKCS12 -srckeystore keystore.p12 -srcstorepass password -destkeystore keystore.jks -deststorepass password
//
// keytool -importcert -trustcacerts -file rootCA.pem -storepass password -noprompt -keystore truststore.jks
class SSLUtils {
    //public static String KEYSTORE_PATH = "keystore.jks";
    //public static String TRUSTSTORE_PATH = "truststore.jks";
    //public static String STORE_PASSWORD = "password";
    //public static String KEY_PASSWORD = "password";
    //public static String ALGORITHM = "SunX509";

    public static KeyStore loadKeystore(final String path, final String storePassword) throws Exception {
        KeyStore store = KeyStore.getInstance("JKS");
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(path));

        try {
            store.load(in, storePassword.toCharArray());
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return store;
    }

    public static KeyManager[] createTestKeyManagers(final String keystorePath, final String algorithm,
                                                     final String keyPassword, final String storePassword) throws Exception {
        KeyStore store = loadKeystore(keystorePath, storePassword);
        KeyManagerFactory factory = KeyManagerFactory.getInstance(algorithm);
        factory.init(store, keyPassword.toCharArray());
        return factory.getKeyManagers();
    }

    public static TrustManager[] createTestTrustManagers(final String truststorePath, final String algorithm,
                                                         final String storePassword) throws Exception {
        KeyStore store = loadKeystore(truststorePath,  storePassword);
        TrustManagerFactory factory = TrustManagerFactory.getInstance(algorithm);
        factory.init(store);
        return factory.getTrustManagers();
    }

    public static SSLContext createSSLContext(final String truststorePath, final String keystorePath,
                                              final String algorithm, final String keyPassword, final  String storePassword) throws Exception {
        SSLContext ctx = SSLContext.getInstance(Options.DEFAULT_SSL_PROTOCOL);
        ctx.init(createTestKeyManagers(keystorePath, algorithm, keyPassword, storePassword),
                createTestTrustManagers( truststorePath, algorithm, storePassword), new SecureRandom());
        return ctx;
    }
}

//public class ConnectTLS {
//    public static void main(String[] args) {
//
//        try {
//            SSLContext ctx = SSLUtils.createSSLContext();
//            Options options = new Options.Builder().
//                    server("nats://localhost:4222").
//                    sslContext(ctx). // Set the SSL context
//                    build();
//            Connection nc = Nats.connect(options);
//
//            // Do something with the connection
//
//            nc.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//}
