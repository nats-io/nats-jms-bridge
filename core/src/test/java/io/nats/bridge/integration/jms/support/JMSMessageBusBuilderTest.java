package io.nats.bridge.integration.jms.support;

import io.nats.bridge.jms.support.JMSMessageBusBuilder;
import org.junit.Test;

public class JMSMessageBusBuilderTest {

    @Test
    public void withAll() {
        JMSMessageBusBuilder builder2 = new JMSMessageBusBuilder().withUserNameConnection("cloudurable").withPasswordConnection("cloudurable").withDestinationName("dynamicQueues/dest_test");
        JMSMessageBusBuilder builder1 = new JMSMessageBusBuilder();

        builder1.withMetricsProcessor(builder2.getMetricsProcessor());
        builder1.withResponseConsumer(builder2.getResponseConsumer());
        builder1.withMetrics(builder2.getMetrics());
        builder1.withResponseDestination(builder2.getResponseDestination());
        builder1.withSessionCreator(builder2.getSessionCreator());
        builder1.withConnectionFactoryName(builder2.getConnectionFactoryName());
        builder1.withDestination(builder2.getDestination());
        builder1.withConnection(builder2.getConnection());
        builder1.withConnectionFactory(builder2.getConnectionFactory());
        builder1.withTimeSource(builder2.getTimeSource());
        builder1.withConnectionCreator(builder2.getConnectionCreator());
        builder1.withUserNameConnection(builder2.getUserNameConnection());
        builder1.withPasswordConnection(builder2.getPasswordConnection());
        builder1.withSession(builder2.getSession());
        builder1.withResponseConsumer(builder2.getResponseConsumer());
        builder1.withContext(builder2.getContext());
        builder1.withAcknowledgeSessionMode(builder2.getAcknowledgeSessionMode());
        builder1.withTransactionalSession(builder2.isTransactionalSession());
        builder1.withProducerSupplier(builder2.getProducerSupplier());


        builder1.withConsumerSupplier(builder2.getConsumerSupplier());
        builder1.withJmsBusLogger(builder2.getJmsBusLogger());
        builder1.withTryHandler(builder2.getTryHandler());

        builder1.build();
        builder2.build();


    }
}