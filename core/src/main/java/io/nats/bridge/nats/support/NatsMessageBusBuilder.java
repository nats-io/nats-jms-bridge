package io.nats.bridge.nats.support;

import io.nats.bridge.TimeSource;
import io.nats.bridge.metrics.Metrics;
import io.nats.bridge.metrics.MetricsDisplay;
import io.nats.bridge.metrics.MetricsProcessor;
import io.nats.bridge.metrics.Output;
import io.nats.bridge.metrics.implementation.SimpleMetrics;
import io.nats.bridge.nats.NatsMessageBus;
import io.nats.bridge.support.MessageBusBuilder;
import io.nats.bridge.util.ExceptionHandler;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.LoggerFactory;

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


    private java.util.Queue<NatsMessageBus.NatsReply> replyQueue;
    private java.util.Queue<NatsMessageBus.NatsReply> replyQueueNotDone;


    private String queueGroup;

    public static NatsMessageBusBuilder builder() {
        return new NatsMessageBusBuilder();
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

    public NatsMessageBus build() {
        return new NatsMessageBus(getName(), getSubject(),
                getConnection(),
                getQueueGroup(), getTryHandler(), getReplyQueue(),
                getReplyQueueNotDone(), getTimeSource(),
                getMetrics(), getMetricsProcessor());
    }
}
