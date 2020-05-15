package io.nats.bridge.tls;


import javax.net.ssl.SSLContext;
import java.io.File;

public class SslContextBuilder {

    private String truststorePath;
    private String keystorePath;
    private String algorithm;
    private String keyPassword;
    private  String storePassword;

    public String getTruststorePath() {
        validatePath("TruststorePath", truststorePath);
        return truststorePath;
    }

    private void validatePath(String name, String path) {

        if (path == null) {
            throw new IllegalStateException(String.format("%s cannot be null", name));
        }

        if (path.trim().isEmpty()) {
            throw new IllegalStateException(String.format("%s cannot be empty", name));
        }

        File filePath = new File(path);
        if (!filePath.exists()) {
            throw new IllegalStateException(String.format("%s path must exist", name));
        }


    }

    public SslContextBuilder withTruststorePath(String truststorePath) {
        this.truststorePath = truststorePath;
        return this;
    }

    public String getKeystorePath() {
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

    public String getKeyPassword() {
        return keyPassword;
    }

    public SslContextBuilder withKeyPassword(final String keyPassword) {
        this.keyPassword = keyPassword;
        return this;
    }

    public String getStorePassword() {
        return storePassword;
    }

    public SslContextBuilder withStorePassword(final String storePassword) {
        this.storePassword = storePassword;
        return this;
    }

    public SSLContext build() {
        try {
            return SSLUtils.createSSLContext(getTruststorePath(), getKeystorePath(), getAlgorithm(), getKeyPassword(), getStorePassword());
        } catch (Exception e) {
            throw new SslContextBuilderException("Unable to create SSL context", e);
        }
    }
}
