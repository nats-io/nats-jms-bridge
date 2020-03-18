package io.nats.bridge.example.service.a;

import io.nats.bridge.MessageBus;
import io.nats.bridge.jms.support.JMSMessageBusBuilder;
import io.nats.bridge.nats.NatsMessageBus;
import io.nats.client.Nats;
import io.nats.client.Options;

import java.io.IOException;
import java.util.UUID;

public class ServiceAUtil {
    static MessageBus getMessageBusJms() {
        final String queueName = "dynamicQueues/AQueue";
        final JMSMessageBusBuilder jmsMessageBusBuilder = new JMSMessageBusBuilder().withDestinationName(queueName);
        final MessageBus messageBus = jmsMessageBusBuilder.build();
        return messageBus;
    }

    static MessageBus getMessageBusNats() throws IOException, InterruptedException {
        final String subject = "a-subject";

        final Options options = new Options.Builder().
                server("nats://localhost:4222").
                noReconnect(). // Disable reconnect attempts
                build();
        return new NatsMessageBus(subject, Nats.connect(options), "queueGroup" + UUID.randomUUID().toString() + System.currentTimeMillis());
    }
}
