package io.nats.bridge.tls;


import io.nats.client.Options;

import javax.net.ssl.*;
import java.io.*;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.util.Base64;
import java.util.Enumeration;

public class SslContextBuilder {

    private String truststorePath;
    private String keystorePath;
    private String algorithm;
    private char[] keystorePassword;
    private char[] trustStorePassword;
    private TrustManager[] trustStoreKeyManagers;
    private KeyManager[] keyStoreKeyManagers;
    private String trustStoreValueEnvVariable = "TRUSTSTORE";
    private String keyStoreValueEnvVariable = "KEYSTORE";
    private String trustStorePathEnvVariable = "TRUSTSTORE_PATH";
    private String keyStorePathEnvVariable = "KEYSTORE_PATH";
    private String keyStoreAlias;


    public String getTrustStoreValueEnvVariable() {
        return trustStoreValueEnvVariable;
    }

    public SslContextBuilder withTrustStoreValueEnvVariable(String trustStoreValueEnvVariable) {
        this.trustStoreValueEnvVariable = trustStoreValueEnvVariable;
        return this;
    }

    public String getKeyStoreValueEnvVariable() {
        return keyStoreValueEnvVariable;
    }

    public SslContextBuilder withKeyStoreValueEnvVariable(String keyStoreValueEnvVariable) {
        this.keyStoreValueEnvVariable = keyStoreValueEnvVariable;
        return this;
    }

    public String getTrustStorePathEnvVariable() {
        return trustStorePathEnvVariable;
    }

    public SslContextBuilder withTrustStorePathEnvVariable(String trustStorePathEnvVariable) {
        this.trustStorePathEnvVariable = trustStorePathEnvVariable;
        return this;
    }

    public String getKeyStorePathEnvVariable() {
        return keyStorePathEnvVariable;
    }

    public SslContextBuilder withKeyStorePathEnvVariable(String keyStorePathEnvVariable) {
        this.keyStorePathEnvVariable = keyStorePathEnvVariable;
        return this;
    }

    public String getKeyStoreAlias() { return keyStoreAlias; }

    public SslContextBuilder withKeyStoreAlias(String keyStoreAlias) {
        this.keyStoreAlias = keyStoreAlias;
        return this;
    }

    private static KeyStore loadKeystore(final InputStream in, final char[] password) throws Exception {
        final KeyStore store = KeyStore.getInstance("JKS");

        try {
            store.load(in, password);
        } finally {
            if (in != null) {
                in.close();
            }
        }

        return store;

    }

    private static KeyStore loadKeystoreAlias(final String alias, final char[] password) throws Exception {
        final KeyStore store = KeyStore.getInstance("JKS");
        store.getKey(alias, password);
        return store;
    }

    public SslContextBuilder withTrustStoreKeyManagers(TrustManager[] trustStoreKeyManagers) {
        this.trustStoreKeyManagers = trustStoreKeyManagers;
        return this;
    }

    public String getTruststorePath() {

        if (truststorePath == null) {
            truststorePath = System.getenv(this.getTrustStorePathEnvVariable());
        }
        if (truststorePath!=null)
        validatePath("TruststorePath", truststorePath);
        return truststorePath;
    }

    private void validatePath(String name, String path) {

        if (path == null) {
            throw new SslContextBuilderException(String.format("%s cannot be null", name));
        }

        if (path.trim().isEmpty()) {
            throw new SslContextBuilderException(String.format("%s cannot be empty", name));
        }

        File filePath = new File(path);
        if (!filePath.exists()) {
            throw new SslContextBuilderException(String.format("%s path must exist", name));
        }


    }

    public SslContextBuilder withTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
        return this;
    }

    public String getKeystorePath() {

        if (keystorePath == null) {
            keystorePath = System.getenv(getKeyStorePathEnvVariable());
        }

        if (keystorePath!=null)
        validatePath("keystorePath", keystorePath);
        return keystorePath;
    }

    public SslContextBuilder withKeystorePath(String keystorePath) {
        this.keystorePath = keystorePath;
        return this;
    }

    public String getAlgorithm() {
        if (algorithm == null || algorithm.trim().isEmpty()) {
            throw new IllegalStateException("algorithm must be set");
        }
        return algorithm;
    }

    public SslContextBuilder withAlgorithm(final String algorithm) {
        this.algorithm = algorithm;
        return this;
    }

    public char[] getKeystorePassword() {
        return keystorePassword;
    }

    public SslContextBuilder withKeyPassword(final String keyPassword) {
        this.keystorePassword = keyPassword.toCharArray();
        return this;
    }

    public char[] getTrustStorePassword() {
        return trustStorePassword;
    }

    public SslContextBuilder withStorePassword(final String storePassword) {
        this.trustStorePassword = storePassword.toCharArray();
        return this;
    }

    public SSLContext buildOld() {
        try {
            return SSLUtils.createSSLContext(getTruststorePath(), getKeystorePath(), getAlgorithm(), new String(getKeystorePassword()),
                    new String(getTrustStorePassword()), getKeyStoreAlias());
        } catch (Exception e) {
            throw new SslContextBuilderException("Unable to create SSL context", e);
        }
    }

    public SSLContext build() {
        try {
            SSLContext ctx = SSLContext.getInstance(Options.DEFAULT_SSL_PROTOCOL);
            ctx.init(getKeyStoreKeyManagers(), getTrustStoreKeyManagers(), new SecureRandom());
            return ctx;
        } catch (Exception e) {
            throw new SslContextBuilderException("Unable to create SSL context", e);
        }
    }


    public TrustManager[] getTrustStoreKeyManagers() {
        if (trustStoreKeyManagers == null) {
            final String truststorePath = getTruststorePath();
            if (truststorePath == null) {
                final String value = System.getenv(getTrustStoreValueEnvVariable());
                byte[] decode = Base64.getDecoder().decode(value);
                try {
                    final KeyStore store =  loadKeystore(new ByteArrayInputStream(decode),
                            getTrustStorePassword());


                    final TrustManagerFactory factory = TrustManagerFactory.getInstance(getAlgorithm());
                    factory.init(store);
                    trustStoreKeyManagers = factory.getTrustManagers();
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to load Trust store from env variable" + getTrustStoreValueEnvVariable(), e);
                }
            }
            else {
                final String extension = truststorePath.substring(truststorePath.lastIndexOf(".") + 1, truststorePath.length());
                if (extension.equals("crt")) {
                    KeyStore truststore = null;
                    try {
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        InputStream cert = new FileInputStream(truststorePath);
                        Certificate ca;
                        try {
                            ca = cf.generateCertificate(cert);
                        } finally {
                            cert.close();
                        }
                        String trustStoreType = KeyStore.getDefaultType();
                        truststore = KeyStore.getInstance(trustStoreType);
                        truststore.load(null, getTrustStorePassword());
                        //truststore.load(null, "cloudurable".toCharArray());
                        truststore.setCertificateEntry("ca",ca);
                        final TrustManagerFactory factory = TrustManagerFactory.getInstance(getAlgorithm());
                        factory.init(truststore);
                        trustStoreKeyManagers = factory.getTrustManagers();
                    } catch (Exception e) {
                        throw new SslContextBuilderException("Unable to convert the CRT file " + truststorePath, e);
                    }

                } else {
                    try {
                        final FileInputStream fileInputStream = new FileInputStream(truststorePath);
                        final KeyStore store =  loadKeystore(fileInputStream, getTrustStorePassword());
                        final TrustManagerFactory factory = TrustManagerFactory.getInstance(getAlgorithm());
                        factory.init(store);
                        trustStoreKeyManagers = factory.getTrustManagers();
                    } catch (FileNotFoundException e) {
                        throw new SslContextBuilderException("Trust store path not found" + truststorePath, e);
                    } catch (Exception e) {
                        throw new SslContextBuilderException("Unable to load Trust store with path" + truststorePath, e);
                    }
                }

            }
        }
        return trustStoreKeyManagers;
    }


    public KeyManager[] getKeyStoreKeyManagers() {

        if (keyStoreKeyManagers == null) {
            final String keyStorePath = getKeystorePath();
            final String keyStoreAlias = getKeyStoreAlias();
            if (keyStorePath == null) {
                final String value = System.getenv(getKeyStoreValueEnvVariable());

                byte[] decode = Base64.getDecoder().decode(value);
                try {
                    final KeyStore store = loadKeystore(new ByteArrayInputStream(decode),
                            getKeystorePassword());
                    final KeyManagerFactory factory = KeyManagerFactory.getInstance(getAlgorithm());
                    factory.init(store, getKeystorePassword());
                    keyStoreKeyManagers = factory.getKeyManagers();

                } catch (Exception e) {
                    throw new IllegalStateException("Unable to load key store from env variable" + getKeyStoreValueEnvVariable(), e);
                }

            } else {
                final String extension = keyStorePath.substring(keyStorePath.lastIndexOf(".") + 1, keyStorePath.length());
                if (extension.equals("crt")) {
                    KeyStore keystore = null;
                    try {
                        CertificateFactory cf = CertificateFactory.getInstance("X.509");
                        InputStream cert = new FileInputStream(keyStorePath);
                        Certificate ca;
                        try {
                            ca = cf.generateCertificate(cert);
                        } finally {
                            cert.close();
                        }
                        String keyStoreType = KeyStore.getDefaultType();
                        keystore = KeyStore.getInstance(keyStoreType);
                        keystore.load(null, getKeystorePassword());
                        //truststore.load(null, "cloudurable".toCharArray());
                        keystore.setCertificateEntry("ca", ca);
                        final KeyManagerFactory factory = KeyManagerFactory.getInstance(getAlgorithm());
                        factory.init(keystore, getKeystorePassword());
                        keyStoreKeyManagers = factory.getKeyManagers();
                    } catch (Exception e) {
                        throw new SslContextBuilderException("Unable to convert the CRT file " + keyStorePath, e);
                    }

                } else {
                    try {
                        final FileInputStream fileInputStream = new FileInputStream(keyStorePath);
                        final KeyStore store = loadKeystore(fileInputStream, getKeystorePassword());

                        if (keyStoreAlias != null) {

                            try {
                                store.setKeyEntry(keyStoreAlias,store.getKey(keyStoreAlias,getKeystorePassword()),
                                        getKeystorePassword(),store.getCertificateChain(keyStoreAlias));
                            } catch (Exception e) {
                            throw new SslContextBuilderException("Unable to find the alias " + keyStoreAlias, e);
                            }
                        }
                        final KeyManagerFactory factory = KeyManagerFactory.getInstance(getAlgorithm());
                        factory.init(store, getKeystorePassword());
                        keyStoreKeyManagers = factory.getKeyManagers();
                    } catch (FileNotFoundException e) {
                        throw new IllegalStateException("Key store path not found" + keyStorePath, e);
                    } catch (Exception e) {
                        throw new IllegalStateException("Unable to load Key store with path" + keyStorePath, e);
                    }
                }
            }
        }
        return keyStoreKeyManagers;

    }

    public SslContextBuilder withKeyStoreKeyManagers(KeyManager[] keyStoreKeyManagers) {
        this.keyStoreKeyManagers = keyStoreKeyManagers;
        return this;
    }
}
