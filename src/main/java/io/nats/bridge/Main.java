package io.nats.bridge;

import io.nats.bridge.jms.support.JMSMessageBusBuilder;


//TODO turn this into a test.
public class Main {

    public static void main(String... args) {
        try {
            JMSMessageBusBuilder jmsMessageBusBuilder = new JMSMessageBusBuilder().withDestinationName("dynamicQueues/testQueue");

            MessageBus messageBus = jmsMessageBusBuilder.build();

            while (true) {
                Thread.sleep(1000);
                messageBus.request("some message", System.out::println);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
