package io.nats.bridge.nats.support;

import io.nats.bridge.TimeSource;
import io.nats.bridge.metrics.Metrics;
import io.nats.bridge.metrics.MetricsDisplay;
import io.nats.bridge.metrics.MetricsProcessor;
import io.nats.bridge.metrics.Output;
import io.nats.bridge.metrics.implementation.SimpleMetrics;
import io.nats.bridge.nats.NatsMessageBus;
import io.nats.bridge.support.MessageBusBuilder;
import io.nats.bridge.tls.SslContextBuilder;
import io.nats.bridge.util.ExceptionHandler;
import io.nats.client.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.LinkedTransferQueue;

public class NatsMessageBusBuilder implements MessageBusBuilder {

    private List<String> servers = new ArrayList<>();
    private Connection connection;
    private String subject;

    private String name = "nats-no-name";
    private ExceptionHandler tryHandler;
    private Options options;
    private Options.Builder optionsBuilder;
    private Properties optionProperties;

    private char[] user;
    private char[] password;

    private TimeSource timeSource;
    private Metrics metrics;
    private MetricsProcessor metricsProcessor;

    public static final String PFX_TLS = "io.nats.client.tls.";
    public static final String JSSL_ENABLE =   PFX_TLS + "jssl.enable";
    public static final String JSSL_TRUST_STORE_PATH =   PFX_TLS + "truststore.path";
    public static final String JSSL_KEY_STORE_PATH =   PFX_TLS + "keystore.path";
    public static final String JSSL_ALGORITHM =   PFX_TLS + "algorithm";
    public static final String JSSL_KEY_STORE_PWD =   PFX_TLS + "keystore.password";
    public static final String JSSL_KEY_STORE_ALIAS =   PFX_TLS + "keystore.alias";
    public static final String JSSL_TRUST_STORE_PWD =   PFX_TLS + "truststore.password";

    public static final String JSSL_KEYSTORE_ENV_VAR_VALUE =   PFX_TLS + "keystore.env_var.value";
    public static final String JSSL_KEYSTORE_ENV_VAR_PATH =   PFX_TLS + "keystore.env_var.path";
    public static final String JSSL_TRUSTSTORE_ENV_VAR_VALUE =   PFX_TLS + "truststore.env_var.value";
    public static final String JSSL_TRUSTSTORE_ENV_VAR_PATH =   PFX_TLS + "truststore.env_var.path";

    public static final String JSSL_KEY_STORE_PWD_BASE64 =   PFX_TLS + "keystore.env_var.base64password";
    public static final String JSSL_TRUST_STORE_PWD_BASE64 =   PFX_TLS + "truststore.env_var.base64password";


    private java.util.Queue<NatsMessageBus.NatsReply> replyQueue;
    private java.util.Queue<NatsMessageBus.NatsReply> replyQueueNotDone;
    private String queueGroup;
    private SslContextBuilder sslContextBuilder;
    private SSLContext sslContext;
    private boolean useTls;



    private Duration durationConnectionsMetrics;

    public boolean isUseTls() {

        if (getOptionProperties().size() > 0) {
            if (getOptionProperties().get(JSSL_ENABLE) != null) {
                useTls = Boolean.parseBoolean(getOptionProperties().getProperty(JSSL_ENABLE, "false"));
            }
        }
        return useTls;
    }

    public NatsMessageBusBuilder withUseTls(boolean useTls) {
        this.useTls = useTls;
        return this;
    }


    public static NatsMessageBusBuilder builder() {
        return new NatsMessageBusBuilder();
    }


    public SslContextBuilder getSslContextBuilder() {

        if (sslContextBuilder == null) {

            sslContextBuilder = new SslContextBuilder();

            if(getOptionProperties().getProperty(JSSL_KEY_STORE_PATH) != null) {
                sslContextBuilder.withKeystorePath(getOptionProperties().getProperty(JSSL_KEY_STORE_PATH));
            }

            if(getOptionProperties().getProperty(JSSL_TRUST_STORE_PATH) != null) {
                sslContextBuilder.withTruststorePath(getOptionProperties().getProperty(JSSL_TRUST_STORE_PATH));
            }

            if(getOptionProperties().getProperty(JSSL_TRUST_STORE_PWD) != null) {
                sslContextBuilder.withStorePassword(getOptionProperties().getProperty(JSSL_TRUST_STORE_PWD));
            }

            if(getOptionProperties().getProperty(JSSL_KEY_STORE_PWD) != null) {
                sslContextBuilder.withKeyPassword(getOptionProperties().getProperty(JSSL_KEY_STORE_PWD));
            }

            if(getOptionProperties().getProperty(JSSL_KEY_STORE_ALIAS) != null) {
                sslContextBuilder.withKeyStoreAlias(getOptionProperties().getProperty(JSSL_KEY_STORE_ALIAS));
            }

            if(getOptionProperties().getProperty(JSSL_ALGORITHM) != null) {
                sslContextBuilder.withAlgorithm(getOptionProperties().getProperty(JSSL_ALGORITHM));
            }

            if(getOptionProperties().getProperty(JSSL_KEYSTORE_ENV_VAR_PATH) != null) {
                sslContextBuilder.withKeyStorePathEnvVariable(getOptionProperties().getProperty(JSSL_KEYSTORE_ENV_VAR_PATH));
            }

            if(getOptionProperties().getProperty(JSSL_KEYSTORE_ENV_VAR_VALUE) != null) {
                sslContextBuilder.withKeyStoreValueEnvVariable(getOptionProperties().getProperty(JSSL_KEYSTORE_ENV_VAR_VALUE));
            }

            if(getOptionProperties().getProperty(JSSL_TRUSTSTORE_ENV_VAR_PATH) != null) {
                sslContextBuilder.withTrustStorePathEnvVariable(getOptionProperties().getProperty(JSSL_TRUSTSTORE_ENV_VAR_PATH));
            }

            if(getOptionProperties().getProperty(JSSL_TRUSTSTORE_ENV_VAR_VALUE) != null) {
                sslContextBuilder.withTrustStoreValueEnvVariable(getOptionProperties().getProperty(JSSL_TRUSTSTORE_ENV_VAR_VALUE));
            }

            if(getOptionProperties().getProperty(JSSL_KEY_STORE_PWD_BASE64) != null) {
                sslContextBuilder.withKeyStorePassBase64EnvVariable(getOptionProperties().getProperty(JSSL_KEY_STORE_PWD_BASE64));
            }

            if(getOptionProperties().getProperty(JSSL_TRUST_STORE_PWD_BASE64) != null) {
                sslContextBuilder.withTrustStorePassBase64EnvVariable(getOptionProperties().getProperty(JSSL_TRUST_STORE_PWD_BASE64));
            }
        }
        return sslContextBuilder;
    }

    public NatsMessageBusBuilder setSslContextBuilder(SslContextBuilder sslContextBuilder) {
        this.sslContextBuilder = sslContextBuilder;
        return this;
    }


    public SSLContext getSslContext() {
        if (sslContext == null) {
            sslContext = getSslContextBuilder().build();
        }
        return sslContext;
    }

    public NatsMessageBusBuilder withSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public Metrics getMetrics() {
        if (metrics == null) {
            metrics = new SimpleMetrics(System::currentTimeMillis);
        }
        return metrics;
    }

    public NatsMessageBusBuilder withMetrics(final Metrics metrics) {
        this.metrics = metrics;
        return this;
    }

    public MetricsProcessor getMetricsProcessor() {
        if (metricsProcessor == null) {
            metricsProcessor = new MetricsDisplay(new Output() {
            }, getMetrics(), 10, Duration.ofSeconds(10), System::currentTimeMillis, name);
        }
        return metricsProcessor;
    }

    public NatsMessageBusBuilder withMetricsProcessor(MetricsProcessor metricsProcessor) {
        this.metricsProcessor = metricsProcessor;
        return this;
    }

    public Queue<NatsMessageBus.NatsReply> getReplyQueue() {
        if (replyQueue == null) {
            replyQueue = new LinkedTransferQueue<>();
        }
        return replyQueue;
    }

    public NatsMessageBusBuilder withReplyQueue(Queue<NatsMessageBus.NatsReply> replyQueueNotDone) {
        this.replyQueue = replyQueue;
        return this;
    }

    public Queue<NatsMessageBus.NatsReply> getReplyQueueNotDone() {
        if (replyQueueNotDone == null) {
            replyQueueNotDone = new LinkedTransferQueue<>();
        }
        return replyQueueNotDone;
    }

    public NatsMessageBusBuilder withReplyQueueNotDone(Queue<NatsMessageBus.NatsReply> replyQueueNotDone) {
        this.replyQueueNotDone = replyQueueNotDone;
        return this;
    }

    public TimeSource getTimeSource() {
        if (timeSource == null) {
            timeSource = System::currentTimeMillis;
        }
        return timeSource;
    }

    public NatsMessageBusBuilder withTimeSource(TimeSource timeSource) {
        this.timeSource = timeSource;
        return this;
    }

    public char[] getUser() {

        return user;
    }

    public NatsMessageBusBuilder withUser(String user) {
        this.user = user.toCharArray();
        return this;
    }

    public char[] getPassword() {
        return password;
    }

    public NatsMessageBusBuilder withPassword(String password) {
        this.password = password.toCharArray();
        return this;
    }

    public Properties getOptionProperties() {

        if (optionProperties == null) {
            optionProperties = new Properties();
        }
        return optionProperties;
    }

    public NatsMessageBusBuilder withOptionProperties(Properties properties) {
        optionProperties = properties;
        return this;
    }

    public String getQueueGroup() {
        if (queueGroup == null) {
            queueGroup = "queueGroup" + UUID.randomUUID().toString() + "_" + System.currentTimeMillis();
        }
        return queueGroup;
    }

    public NatsMessageBusBuilder withQueueGroup(String queueGroup) {
        this.queueGroup = queueGroup;
        return this;
    }

    public List<String> getServers() {
        if (servers.isEmpty()) {
            final String hosts = System.getenv().getOrDefault("NATS_BRIDGE_NATS_SERVERS", "nats://localhost:4222");
            if (!hosts.contains(",")) {
                servers.add(hosts);
            } else {
                Collections.addAll(servers, hosts.split(","));
            }
        }
        return servers;
    }

    public NatsMessageBusBuilder withHost(String host) {
        this.servers.add(host);
        return this;
    }

    public NatsMessageBusBuilder withServers(List<String> servers) {
        this.servers.addAll(servers);
        return this;
    }

    public Connection getConnection() {
        if (connection == null) {
            try {

                connection = Nats.connect(this.getOptions());

            } catch (Exception e) {

                throw new NatsBuilderException("Issues getting connection", e);
            }
        }
        return connection;
    }

    public NatsMessageBusBuilder withConnection(Connection connection) {
        this.connection = connection;
        return this;
    }

    public String getName() {
        return name;
    }

    public NatsMessageBusBuilder withName(String name) {
        if (name == null) {
            throw new NatsBuilderException("Name must be set");
        }
        this.name = name;
        return this;
    }

    public String getSubject() {
        return subject;
    }

    public NatsMessageBusBuilder withSubject(String subject) {
        if (subject == null) {
            throw new NatsBuilderException("Subject must be set");
        }
        this.subject = subject;
        return this;
    }

    public ExceptionHandler getTryHandler() {
        if (tryHandler == null) {
            tryHandler = new ExceptionHandler(LoggerFactory.getLogger(NatsMessageBus.class));
        }
        return tryHandler;
    }

    public NatsMessageBusBuilder withTryHandler(ExceptionHandler tryHandler) {
        this.tryHandler = tryHandler;
        return this;
    }

    public Options getOptions() {
        if (options == null) {
            options = getOptionsBuilder().build();
        }
        return options;
    }







    public Options.Builder getOptionsBuilder() {
        if (optionsBuilder == null) {
            if (optionProperties == null) {
                optionsBuilder = new Options.Builder()
                        .servers(getServers().toArray(new String[1]));
            } else {
                optionsBuilder = new Options.Builder(getOptionProperties())
                        .servers(getServers().toArray(new String[1]));
            }
            if (password != null && user != null) {
                optionsBuilder.userInfo(user, password);
            }
            if (isUseTls()) {
                optionsBuilder.sslContext(this.getSslContext());
            }

            final Logger logger = LoggerFactory.getLogger(NatsMessageBus.class);
            optionsBuilder.errorListener(new ErrorListener() {
                @Override
                public void errorOccurred(Connection conn, String error) {
                    logger.error("Error Occurred with NATS: {} with the bus {}", error, name);
                }

                @Override
                public void exceptionOccurred(Connection conn, Exception exp) {
                    logger.error("Exception Occurred with NATS:", exp);
                    logger.error("Exception Occurred with NATS: {} with the bus {}", exp.getLocalizedMessage(), name);
                }

                @Override
                public void slowConsumerDetected(Connection conn, Consumer consumer) {
                    logger.warn("Slow Consumer Detected {}", name);
                }
            });
        }
        return optionsBuilder;
    }

    public NatsMessageBusBuilder withOptionsBuilder(Options.Builder optionsBuilder) {
        this.optionsBuilder = optionsBuilder;
        return this;
    }

    public NatsMessageBusBuilder withOptions(Options options) {
        this.options = options;
        return this;
    }

    public Duration getDurationConnectionsMetrics() {
        if (durationConnectionsMetrics == null) {
            durationConnectionsMetrics = Duration.ofSeconds(30);
        }
        return durationConnectionsMetrics;
    }

    public NatsMessageBusBuilder withDurationConnectionsMetrics(Duration durationConnectionsMetrics) {
        this.durationConnectionsMetrics = durationConnectionsMetrics;
        return this;
    }

    public NatsMessageBus build() {
        return new NatsMessageBus(getName(), getSubject(),
                getConnection(),
                getQueueGroup(), getTryHandler(), getReplyQueue(),
                getReplyQueueNotDone(), getTimeSource(),
                getMetrics(), getMetricsProcessor(), getDurationConnectionsMetrics());
    }
}
