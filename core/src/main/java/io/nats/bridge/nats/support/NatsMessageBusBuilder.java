package io.nats.bridge.nats.support;

import io.nats.bridge.nats.NatsMessageBus;
import io.nats.bridge.util.ExceptionHandler;
import io.nats.client.Connection;
import io.nats.client.Nats;
import io.nats.client.Options;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NatsMessageBusBuilder {


    private List<String> servers = new ArrayList<>();
    private Connection connection;
    private String subject;
    private ExecutorService pool;
    private ExceptionHandler tryHandler;
    private Options options;
    private Options.Builder optionsBuilder;

    private String queueGroup;

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

    public ExecutorService getPool() {
        if (pool == null) {
            pool = Executors.newFixedThreadPool(25);
        }
        return pool;
    }

    public NatsMessageBusBuilder withPool(ExecutorService pool) {
        this.pool = pool;
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
            optionsBuilder = new Options.Builder()
                    .servers(getServers().toArray(new String[1]));
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

    public static NatsMessageBusBuilder builder() {
        return new NatsMessageBusBuilder();
    }
    public NatsMessageBus build() {
        return new NatsMessageBus(getSubject(), getConnection(), getQueueGroup(), getPool(), getTryHandler());
    }
}
