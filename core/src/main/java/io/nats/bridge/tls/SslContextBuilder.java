package io.nats.bridge.tls;


import io.nats.client.Options;

import javax.net.ssl.*;
import java.io.*;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Base64;

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
                    new String(getTrustStorePassword()));
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
        return trustStoreKeyManagers;
    }


    public KeyManager[] getKeyStoreKeyManagers() {

        if (keyStoreKeyManagers == null) {
            final String keyStorePath = getKeystorePath();
            if (keyStorePath == null) {
                final String value = System.getenv(getKeyStoreValueEnvVariable());

                byte[] decode = Base64.getDecoder().decode(value);
                try {
                   final KeyStore store =  loadKeystore(new ByteArrayInputStream(decode),
                             getKeystorePassword());
                    final KeyManagerFactory factory = KeyManagerFactory.getInstance(getAlgorithm());
                    factory.init(store, getKeystorePassword());
                    keyStoreKeyManagers = factory.getKeyManagers();
                } catch (Exception e) {
                    throw new IllegalStateException("Unable to load key store from env variable" + getKeyStoreValueEnvVariable(), e);
                }
            } else {
                try {
                    final FileInputStream fileInputStream = new FileInputStream(keyStorePath);
                    final KeyStore store  = loadKeystore(fileInputStream, getKeystorePassword());
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
        return keyStoreKeyManagers;

    }

    public SslContextBuilder withKeyStoreKeyManagers(KeyManager[] keyStoreKeyManagers) {
        this.keyStoreKeyManagers = keyStoreKeyManagers;
        return this;
    }
}
